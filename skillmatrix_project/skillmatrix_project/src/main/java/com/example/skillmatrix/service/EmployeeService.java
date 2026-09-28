package com.example.skillmatrix.service;

import com.example.skillmatrix.dto.Dtos.EmployeeRequest;
import com.example.skillmatrix.dto.Dtos.EmployeeResponse;
import com.example.skillmatrix.dto.Dtos.ProficiencyRequest;
import com.example.skillmatrix.dto.Dtos.SkillLevel;
import com.example.skillmatrix.entity.Employee;
import com.example.skillmatrix.entity.EmployeeSkill;
import com.example.skillmatrix.exception.BusinessRuleException;
import com.example.skillmatrix.exception.ResourceNotFoundException;
import com.example.skillmatrix.repository.AllocationRepository;
import com.example.skillmatrix.repository.EmployeeRepository;
import com.example.skillmatrix.repository.EmployeeSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final AllocationRepository allocationRepository;
    private final SkillService skillService;

    public EmployeeService(EmployeeRepository employeeRepository, EmployeeSkillRepository employeeSkillRepository,
                           AllocationRepository allocationRepository, SkillService skillService) {
        this.employeeRepository = employeeRepository;
        this.employeeSkillRepository = employeeSkillRepository;
        this.allocationRepository = allocationRepository;
        this.skillService = skillService;
    }

    public Employee getEntity(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found with id " + id));
    }

    private int allocated(Long employeeId) {
        return allocationRepository.sumPercentageByEmployeeId(employeeId).intValue();
    }

    private EmployeeResponse toResponse(Employee e) {
        int allocated = allocated(e.getId());
        List<SkillLevel> skills = employeeSkillRepository.findByEmployeeId(e.getId()).stream()
                .map(es -> new SkillLevel(es.getSkill().getId(), es.getSkill().getName(), es.getProficiency()))
                .sorted((a, b) -> a.skillName().compareToIgnoreCase(b.skillName()))
                .toList();
        return new EmployeeResponse(e.getId(), e.getName(), e.getEmail(), e.getAvailableCapacity(),
                allocated, e.getAvailableCapacity() - allocated, skills);
    }

    @Transactional(readOnly = true)
    public List<EmployeeResponse> findAll() {
        return employeeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest r) {
        String email = r.email().trim();
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new BusinessRuleException("An employee with email '" + email + "' already exists.");
        }
        Employee e = new Employee();
        e.setName(r.name().trim());
        e.setEmail(email);
        e.setAvailableCapacity(r.availableCapacity());
        return toResponse(employeeRepository.save(e));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest r) {
        Employee e = getEntity(id);
        String email = r.email().trim();
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new BusinessRuleException("An employee with email '" + email + "' already exists.");
        }
        int allocated = allocated(id);
        if (r.availableCapacity() < allocated) {
            throw new BusinessRuleException("Cannot reduce available capacity to " + r.availableCapacity()
                    + "% because " + e.getName() + " is already allocated " + allocated
                    + "% across projects. Reduce their allocations first.");
        }
        e.setName(r.name().trim());
        e.setEmail(email);
        e.setAvailableCapacity(r.availableCapacity());
        return toResponse(employeeRepository.save(e));
    }

    @Transactional
    public void delete(Long id) {
        employeeRepository.delete(getEntity(id));
    }

    /** Adds the skill to the employee's matrix row, or updates the proficiency if it already exists. */
    @Transactional
    public EmployeeResponse setSkill(Long employeeId, ProficiencyRequest r) {
        Employee e = getEntity(employeeId);
        var skill = skillService.getEntity(r.skillId());
        EmployeeSkill es = employeeSkillRepository.findByEmployeeIdAndSkillId(employeeId, skill.getId())
                .orElseGet(() -> {
                    EmployeeSkill n = new EmployeeSkill();
                    n.setEmployee(e);
                    n.setSkill(skill);
                    return n;
                });
        es.setProficiency(r.proficiency());
        employeeSkillRepository.save(es);
        return toResponse(e);
    }

    @Transactional
    public EmployeeResponse removeSkill(Long employeeId, Long skillId) {
        Employee e = getEntity(employeeId);
        EmployeeSkill es = employeeSkillRepository.findByEmployeeIdAndSkillId(employeeId, skillId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee " + e.getName() + " does not have skill with id " + skillId));
        employeeSkillRepository.delete(es);
        return toResponse(e);
    }
}
