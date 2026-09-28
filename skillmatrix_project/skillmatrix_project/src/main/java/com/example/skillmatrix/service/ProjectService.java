package com.example.skillmatrix.service;

import com.example.skillmatrix.dto.Dtos.ProficiencyRequest;
import com.example.skillmatrix.dto.Dtos.ProjectRequest;
import com.example.skillmatrix.dto.Dtos.ProjectResponse;
import com.example.skillmatrix.dto.Dtos.SkillLevel;
import com.example.skillmatrix.entity.Project;
import com.example.skillmatrix.entity.ProjectSkill;
import com.example.skillmatrix.exception.ResourceNotFoundException;
import com.example.skillmatrix.repository.ProjectRepository;
import com.example.skillmatrix.repository.ProjectSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;
    private final ProjectSkillRepository projectSkillRepository;
    private final SkillService skillService;

    public ProjectService(ProjectRepository projectRepository, ProjectSkillRepository projectSkillRepository,
                          SkillService skillService) {
        this.projectRepository = projectRepository;
        this.projectSkillRepository = projectSkillRepository;
        this.skillService = skillService;
    }

    public Project getEntity(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project not found with id " + id));
    }

    private ProjectResponse toResponse(Project p) {
        List<SkillLevel> req = projectSkillRepository.findByProjectId(p.getId()).stream()
                .map(ps -> new SkillLevel(ps.getSkill().getId(), ps.getSkill().getName(), ps.getMinProficiency()))
                .sorted((a, b) -> a.skillName().compareToIgnoreCase(b.skillName()))
                .toList();
        return new ProjectResponse(p.getId(), p.getName(), p.getDescription(), req);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> findAll() {
        return projectRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public ProjectResponse create(ProjectRequest r) {
        Project p = new Project();
        p.setName(r.name().trim());
        p.setDescription(r.description() == null ? null : r.description().trim());
        return toResponse(projectRepository.save(p));
    }

    @Transactional
    public ProjectResponse update(Long id, ProjectRequest r) {
        Project p = getEntity(id);
        p.setName(r.name().trim());
        p.setDescription(r.description() == null ? null : r.description().trim());
        return toResponse(projectRepository.save(p));
    }

    @Transactional
    public void delete(Long id) {
        projectRepository.delete(getEntity(id));
    }

    /** Adds a required skill to the project, or updates its minimum proficiency. */
    @Transactional
    public ProjectResponse setRequiredSkill(Long projectId, ProficiencyRequest r) {
        Project p = getEntity(projectId);
        var skill = skillService.getEntity(r.skillId());
        ProjectSkill ps = projectSkillRepository.findByProjectIdAndSkillId(projectId, skill.getId())
                .orElseGet(() -> {
                    ProjectSkill n = new ProjectSkill();
                    n.setProject(p);
                    n.setSkill(skill);
                    return n;
                });
        ps.setMinProficiency(r.proficiency());
        projectSkillRepository.save(ps);
        return toResponse(p);
    }

    @Transactional
    public ProjectResponse removeRequiredSkill(Long projectId, Long skillId) {
        Project p = getEntity(projectId);
        ProjectSkill ps = projectSkillRepository.findByProjectIdAndSkillId(projectId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Project " + p.getName() + " does not require skill with id " + skillId));
        projectSkillRepository.delete(ps);
        return toResponse(p);
    }
}
