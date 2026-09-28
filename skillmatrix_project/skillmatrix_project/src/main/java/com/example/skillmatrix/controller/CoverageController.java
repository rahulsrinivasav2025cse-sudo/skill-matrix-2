package com.example.skillmatrix.controller;

import com.example.skillmatrix.dto.Dtos.CoverageResponse;
import com.example.skillmatrix.service.RecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/coverage")
public class CoverageController {
    private final RecommendationService service;

    public CoverageController(RecommendationService service) { this.service = service; }

    /** GET /api/coverage?projectId=1  or  GET /api/coverage?skillIds=1,2,3&minProficiency=3 */
    @GetMapping
    public CoverageResponse coverage(@RequestParam(required = false) Long projectId,
                                     @RequestParam(required = false) List<Long> skillIds,
                                     @RequestParam(required = false) Integer minProficiency) {
        return service.coverage(projectId, skillIds, minProficiency);
    }
}
