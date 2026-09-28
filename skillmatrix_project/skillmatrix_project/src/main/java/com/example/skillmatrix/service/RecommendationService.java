package com.example.skillmatrix.service;

import com.example.skillmatrix.dto.Dtos.CoverageEmployee;
import com.example.skillmatrix.dto.Dtos.CoverageEntry;
import com.example.skillmatrix.dto.Dtos.CoverageResponse;
import com.example.skillmatrix.dto.Dtos.Recommendation;
import com.example.skillmatrix.dto.Dtos.SkillLevel;
import com.example.skillmatrix.entity.Employee;
import com.example.skillmatrix.entity.EmployeeSkill;
import com.example.skillmatrix.entity.ProjectSkill;
import com.example.skillmatrix.entity.Skill;
import com.example.skillmatrix.exception.BusinessRuleException;
import com.example.skillmatrix.repository.AllocationRepository;
import com.example.skillmatrix.repository.EmployeeRepository;
import com.example.skillmatrix.repository.EmployeeSkillRepository;
import com.example.skillmatrix.repository.ProjectSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    private final ProjectService projectService;
    private final SkillService skillService;
    private final ProjectSkillRepository projectSkillRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final AllocationRepository allocationRepository;

    public RecommendationService(ProjectService projectService, SkillService skillService,
                                 ProjectSkillRepository projectSkillRepository, EmployeeRepository employeeRepository,
                                 EmployeeSkillRepository employeeSkillRepository, AllocationRepository allocationRepository) {
        this.projectService = projectService;
        this.skillService = skillService;
        this.projectSkillRepository = projectSkillRepository;
        this.employeeRepository = employeeRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.allocationRepository = allocationRepository;
    }

    private record Scored(Employee employee, int free, int matched, int score,
                          List<SkillLevel> details, List<String> missing) {}

    private record Requirement(Skill skill, int minProficiency) {}

    private int freeCapacity(Employee e) {
        return e.getAvailableCapacity() - allocationRepository.sumPercentageByEmployeeId(e.getId()).intValue();
    }

    /**
     * Ranks available employees for a project. An employee "matches" a required skill when their proficiency
     * meets the project's minimum. Ranking: more matched skills first; then higher total proficiency on the
     * matched skills (higher proficiency beats lower); then more free capacity; then name.
     * Employees with no free capacity, no matching skill, or already on the project are excluded.
     */
    @Transactional(readOnly = true)
    public List<Recommendation> recommend(Long projectId) {
        var project = projectService.getEntity(projectId);
        List<ProjectSkill> required = projectSkillRepository.findByProjectId(projectId);
        if (required.isEmpty()) {
            throw new BusinessRuleException("Project '" + project.getName()
                    + "' has no required skills yet. Add required skills before requesting recommendations.");
        }
        Set<Long> alreadyOnProject = allocationRepository.findByProjectId(projectId).stream()
                .map(a -> a.getEmployee().getId()).collect(Collectors.toSet());

        List<Scored> scored = new ArrayList<>();
        for (Employee e : employeeRepository.findAll()) {
            if (alreadyOnProject.contains(e.getId())) continue;
            int free = freeCapacity(e);
            if (free <= 0) continue;
            Map<Long, Integer> prof = new HashMap<>();
            for (EmployeeSkill es : employeeSkillRepository.findByEmployeeId(e.getId())) {
                prof.put(es.getSkill().getId(), es.getProficiency());
            }
            int matched = 0, score = 0;
            List<SkillLevel> details = new ArrayList<>();
            List<String> missing = new ArrayList<>();
            for (ProjectSkill ps : required) {
                Integer p = prof.get(ps.getSkill().getId());
                if (p != null && p >= ps.getMinProficiency()) {
                    matched++;
                    score += p;
                    details.add(new SkillLevel(ps.getSkill().getId(), ps.getSkill().getName(), p));
                } else {
                    missing.add(ps.getSkill().getName());
                }
            }
            if (matched == 0) continue;
            scored.add(new Scored(e, free, matched, score, details, missing));
        }

        scored.sort(Comparator.comparingInt(Scored::matched).reversed()
                .thenComparing(Comparator.comparingInt(Scored::score).reversed())
                .thenComparing(Comparator.comparingInt(Scored::free).reversed())
                .thenComparing(s -> s.employee().getName().toLowerCase()));

        List<Recommendation> result = new ArrayList<>();
        for (int i = 0; i < scored.size(); i++) {
            Scored s = scored.get(i);
            result.add(new Recommendation(i + 1, s.employee().getId(), s.employee().getName(), s.matched(),
                    required.size(), s.score(), s.free(), s.details(), s.missing()));
        }
        return result;
    }

    /** Team-wide coverage and gaps for a required skill set (from a project, or explicit skill ids). */
    @Transactional(readOnly = true)
    public CoverageResponse coverage(Long projectId, List<Long> skillIds, Integer minProficiency) {
        List<Requirement> reqs = new ArrayList<>();
        if (projectId != null) {
            projectService.getEntity(projectId);
            for (ProjectSkill ps : projectSkillRepository.findByProjectId(projectId)) {
                reqs.add(new Requirement(ps.getSkill(), ps.getMinProficiency()));
            }
            if (reqs.isEmpty()) {
                throw new BusinessRuleException("That project has no required skills defined yet.");
            }
        } else if (skillIds != null && !skillIds.isEmpty()) {
            int min = minProficiency == null ? 1 : minProficiency;
            if (min < 1 || min > 5) {
                throw new BusinessRuleException("minProficiency must be between 1 and 5.");
            }
            for (Long id : skillIds.stream().distinct().toList()) {
                reqs.add(new Requirement(skillService.getEntity(id), min));
            }
        } else {
            throw new BusinessRuleException("Provide either a projectId or at least one skillId.");
        }

        List<CoverageEntry> entries = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        int covered = 0;
        for (Requirement r : reqs) {
            List<CoverageEmployee> people = employeeSkillRepository.findBySkillId(r.skill().getId()).stream()
                    .filter(es -> es.getProficiency() >= r.minProficiency())
                    .map(es -> new CoverageEmployee(es.getEmployee().getId(), es.getEmployee().getName(),
                            es.getProficiency(), freeCapacity(es.getEmployee())))
                    .sorted(Comparator.comparingInt(CoverageEmployee::proficiency).reversed())
                    .toList();
            int available = (int) people.stream().filter(p -> p.freeCapacity() > 0).count();
            String status = people.isEmpty() ? "GAP" : (available == 0 ? "NO_AVAILABILITY" : "COVERED");
            if (people.isEmpty()) gaps.add(r.skill().getName());
            else covered++;
            entries.add(new CoverageEntry(r.skill().getId(), r.skill().getName(), r.minProficiency(),
                    people.size(), available, status, people));
        }
        return new CoverageResponse(reqs.size(), covered, gaps, entries);
    }
}
