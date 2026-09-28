package com.example.skillmatrix.service;

import com.example.skillmatrix.dto.Dtos.AllocationRequest;
import com.example.skillmatrix.dto.Dtos.AllocationResponse;
import com.example.skillmatrix.entity.Allocation;
import com.example.skillmatrix.entity.AllocationStatus;
import com.example.skillmatrix.entity.Employee;
import com.example.skillmatrix.entity.Project;
import com.example.skillmatrix.exception.BusinessRuleException;
import com.example.skillmatrix.exception.ResourceNotFoundException;
import com.example.skillmatrix.repository.AllocationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AllocationService {
    private final AllocationRepository allocationRepository;
    private final EmployeeService employeeService;
    private final ProjectService projectService;

    public AllocationService(AllocationRepository allocationRepository, EmployeeService employeeService,
                             ProjectService projectService) {
        this.allocationRepository = allocationRepository;
        this.employeeService = employeeService;
        this.projectService = projectService;
    }

    private Allocation getEntity(Long id) {
        return allocationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation not found with id " + id));
    }

    private AllocationResponse toResponse(Allocation a) {
        return new AllocationResponse(a.getId(), a.getEmployee().getId(), a.getEmployee().getName(),
                a.getProject().getId(), a.getProject().getName(), a.getPercentage(), a.getStatus().name());
    }

    private AllocationStatus statusFor(int percentage) {
        return percentage >= 100 ? AllocationStatus.FULLY_ALLOCATED : AllocationStatus.PARTIALLY_ALLOCATED;
    }

    /**
     * Business rule: an employee cannot be allocated beyond 100% of their available capacity across projects.
     * Always checked BEFORE saving. {@code alreadyCounted} is the percentage of the allocation being replaced (0 for new).
     */
    private void assertWithinCapacity(Employee e, int requested, int alreadyCounted) {
        int currentTotal = allocationRepository.sumPercentageByEmployeeId(e.getId()).intValue() - alreadyCounted;
        int capacity = e.getAvailableCapacity();
        if (currentTotal + requested > capacity) {
            int remaining = Math.max(0, capacity - currentTotal);
            throw new BusinessRuleException("An employee cannot be allocated beyond 100% of their available capacity across projects. "
                    + e.getName() + " has " + capacity + "% available capacity, " + currentTotal
                    + "% already allocated (" + remaining + "% remaining), but " + requested + "% was requested.");
        }
    }

    @Transactional(readOnly = true)
    public List<AllocationResponse> findAll() {
        return allocationRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public AllocationResponse findById(Long id) {
        return toResponse(getEntity(id));
    }

    @Transactional
    public AllocationResponse create(AllocationRequest r) {
        Employee e = employeeService.getEntity(r.employeeId());
        Project p = projectService.getEntity(r.projectId());
        if (allocationRepository.existsByEmployeeIdAndProjectId(e.getId(), p.getId())) {
            throw new BusinessRuleException(e.getName() + " is already allocated to project '" + p.getName()
                    + "'. Update the existing allocation instead.");
        }
        assertWithinCapacity(e, r.percentage(), 0);
        Allocation a = new Allocation();
        a.setEmployee(e);
        a.setProject(p);
        a.setPercentage(r.percentage());
        a.setStatus(statusFor(r.percentage()));
        return toResponse(allocationRepository.save(a));
    }

    @Transactional
    public AllocationResponse update(Long id, int percentage) {
        Allocation a = getEntity(id);
        assertWithinCapacity(a.getEmployee(), percentage, a.getPercentage());
        a.setPercentage(percentage);
        a.setStatus(statusFor(percentage));
        return toResponse(allocationRepository.save(a));
    }

    @Transactional
    public void delete(Long id) {
        allocationRepository.delete(getEntity(id));
    }
}
