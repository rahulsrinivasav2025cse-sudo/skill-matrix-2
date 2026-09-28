package com.example.skillmatrix.repository;

import com.example.skillmatrix.entity.EmployeeSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, Long> {
    List<EmployeeSkill> findByEmployeeId(Long employeeId);
    List<EmployeeSkill> findBySkillId(Long skillId);
    Optional<EmployeeSkill> findByEmployeeIdAndSkillId(Long employeeId, Long skillId);
    boolean existsBySkillId(Long skillId);
}
