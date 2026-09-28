package com.example.skillmatrix.repository;

import com.example.skillmatrix.entity.Allocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    List<Allocation> findByProjectId(Long projectId);
    List<Allocation> findByEmployeeId(Long employeeId);
    boolean existsByEmployeeIdAndProjectId(Long employeeId, Long projectId);

    @Query("select coalesce(sum(a.percentage), 0L) from Allocation a where a.employee.id = :employeeId")
    Long sumPercentageByEmployeeId(@Param("employeeId") Long employeeId);
}
