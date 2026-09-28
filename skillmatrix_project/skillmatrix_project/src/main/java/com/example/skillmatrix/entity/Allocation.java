package com.example.skillmatrix.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "allocations",
        uniqueConstraints = @UniqueConstraint(columnNames = {"employee_id", "project_id"}))
public class Allocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne(optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** Percentage of the employee's time given to this project (1-100). */
    @Column(nullable = false)
    private int percentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AllocationStatus status;

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public Project getProject() { return project; }
    public void setProject(Project project) { this.project = project; }
    public int getPercentage() { return percentage; }
    public void setPercentage(int percentage) { this.percentage = percentage; }
    public AllocationStatus getStatus() { return status; }
    public void setStatus(AllocationStatus status) { this.status = status; }
}
