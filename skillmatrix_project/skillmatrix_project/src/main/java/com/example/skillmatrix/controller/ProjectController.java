package com.example.skillmatrix.controller;

import com.example.skillmatrix.dto.Dtos.ProficiencyRequest;
import com.example.skillmatrix.dto.Dtos.ProjectRequest;
import com.example.skillmatrix.dto.Dtos.ProjectResponse;
import com.example.skillmatrix.dto.Dtos.Recommendation;
import com.example.skillmatrix.service.ProjectService;
import com.example.skillmatrix.service.RecommendationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {
    private final ProjectService service;
    private final RecommendationService recommendationService;

    public ProjectController(ProjectService service, RecommendationService recommendationService) {
        this.service = service;
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public List<ProjectResponse> all() { return service.findAll(); }

    @GetMapping("/{id}")
    public ProjectResponse one(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<ProjectResponse> create(@Valid @RequestBody ProjectRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));
    }

    @PutMapping("/{id}")
    public ProjectResponse update(@PathVariable Long id, @Valid @RequestBody ProjectRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Define a required skill (minimum proficiency) for the project, or update it. */
    @PutMapping("/{id}/skills")
    public ProjectResponse setRequiredSkill(@PathVariable Long id, @Valid @RequestBody ProficiencyRequest r) {
        return service.setRequiredSkill(id, r);
    }

    @DeleteMapping("/{id}/skills/{skillId}")
    public ProjectResponse removeRequiredSkill(@PathVariable Long id, @PathVariable Long skillId) {
        return service.removeRequiredSkill(id, skillId);
    }

    /** Employees ranked by skill match and current availability. */
    @GetMapping("/{id}/recommendations")
    public List<Recommendation> recommendations(@PathVariable Long id) {
        return recommendationService.recommend(id);
    }
}
