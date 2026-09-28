package com.example.skillmatrix.dto;

import jakarta.validation.constraints.*;

import java.util.List;

/** Request and response objects used by the REST API. */
public final class Dtos {
    private Dtos() {}

    // ----- requests -----
    public record EmployeeRequest(
            @NotBlank(message = "Employee name is required") @Size(max = 120, message = "Name must be at most 120 characters") String name,
            @NotBlank(message = "Email is required") @Email(message = "Email must be a valid address") @Size(max = 160, message = "Email must be at most 160 characters") String email,
            @NotNull(message = "Available capacity is required")
            @Min(value = 1, message = "Available capacity must be at least 1%")
            @Max(value = 100, message = "Available capacity cannot exceed 100%") Integer availableCapacity) {}

    public record SkillRequest(
            @NotBlank(message = "Skill name is required") @Size(max = 100, message = "Skill name must be at most 100 characters") String name,
            @Size(max = 255, message = "Description must be at most 255 characters") String description) {}

    public record ProjectRequest(
            @NotBlank(message = "Project name is required") @Size(max = 150, message = "Project name must be at most 150 characters") String name,
            @Size(max = 500, message = "Description must be at most 500 characters") String description) {}

    /** Used for both employee proficiency and project minimum proficiency. */
    public record ProficiencyRequest(
            @NotNull(message = "skillId is required") Long skillId,
            @NotNull(message = "Proficiency is required")
            @Min(value = 1, message = "Proficiency must be between 1 and 5")
            @Max(value = 5, message = "Proficiency must be between 1 and 5") Integer proficiency) {}

    public record AllocationRequest(
            @NotNull(message = "employeeId is required") Long employeeId,
            @NotNull(message = "projectId is required") Long projectId,
            @NotNull(message = "Allocation percentage is required")
            @Min(value = 1, message = "Allocation must be between 1 and 100")
            @Max(value = 100, message = "Allocation must be between 1 and 100") Integer percentage) {}

    public record AllocationUpdateRequest(
            @NotNull(message = "Allocation percentage is required")
            @Min(value = 1, message = "Allocation must be between 1 and 100")
            @Max(value = 100, message = "Allocation must be between 1 and 100") Integer percentage) {}

    // ----- responses -----
    public record SkillLevel(Long skillId, String skillName, int proficiency) {}

    public record EmployeeResponse(Long id, String name, String email, int availableCapacity,
                                   int allocatedCapacity, int freeCapacity, List<SkillLevel> skills) {}

    public record SkillResponse(Long id, String name, String description) {}

    public record ProjectResponse(Long id, String name, String description, List<SkillLevel> requiredSkills) {}

    public record AllocationResponse(Long id, Long employeeId, String employeeName, Long projectId,
                                     String projectName, int percentage, String status) {}

    public record Recommendation(int rank, Long employeeId, String employeeName, int matchedSkills,
                                 int totalRequiredSkills, int proficiencyScore, int freeCapacity,
                                 List<SkillLevel> matchedDetails, List<String> missingSkills) {}

    public record CoverageEmployee(Long employeeId, String employeeName, int proficiency, int freeCapacity) {}

    public record CoverageEntry(Long skillId, String skillName, int requiredProficiency, int qualifiedCount,
                                int availableCount, String status, List<CoverageEmployee> employees) {}

    public record CoverageResponse(int totalSkills, int coveredSkills, List<String> gaps, List<CoverageEntry> entries) {}
}
