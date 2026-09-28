package com.example.skillmatrix.service;

import com.example.skillmatrix.dto.Dtos.SkillRequest;
import com.example.skillmatrix.dto.Dtos.SkillResponse;
import com.example.skillmatrix.entity.Skill;
import com.example.skillmatrix.exception.BusinessRuleException;
import com.example.skillmatrix.exception.ResourceNotFoundException;
import com.example.skillmatrix.repository.EmployeeSkillRepository;
import com.example.skillmatrix.repository.ProjectSkillRepository;
import com.example.skillmatrix.repository.SkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SkillService {
    private final SkillRepository skillRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final ProjectSkillRepository projectSkillRepository;

    public SkillService(SkillRepository skillRepository, EmployeeSkillRepository employeeSkillRepository,
                        ProjectSkillRepository projectSkillRepository) {
        this.skillRepository = skillRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.projectSkillRepository = projectSkillRepository;
    }

    public Skill getEntity(Long id) {
        return skillRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Skill not found with id " + id));
    }

    private SkillResponse toResponse(Skill s) {
        return new SkillResponse(s.getId(), s.getName(), s.getDescription());
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> findAll() {
        return skillRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public SkillResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public SkillResponse create(SkillRequest r) {
        String name = r.name().trim();
        if (skillRepository.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException("A skill named '" + name + "' already exists.");
        }
        Skill s = new Skill();
        s.setName(name);
        s.setDescription(r.description() == null ? null : r.description().trim());
        return toResponse(skillRepository.save(s));
    }

    @Transactional
    public SkillResponse update(Long id, SkillRequest r) {
        Skill s = getEntity(id);
        String name = r.name().trim();
        if (skillRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new BusinessRuleException("A skill named '" + name + "' already exists.");
        }
        s.setName(name);
        s.setDescription(r.description() == null ? null : r.description().trim());
        return toResponse(skillRepository.save(s));
    }

    @Transactional
    public void delete(Long id) {
        Skill s = getEntity(id);
        if (employeeSkillRepository.existsBySkillId(id) || projectSkillRepository.existsBySkillId(id)) {
            throw new BusinessRuleException("Skill '" + s.getName()
                    + "' is still used by employees or projects. Remove those links before deleting it.");
        }
        skillRepository.delete(s);
    }
}
