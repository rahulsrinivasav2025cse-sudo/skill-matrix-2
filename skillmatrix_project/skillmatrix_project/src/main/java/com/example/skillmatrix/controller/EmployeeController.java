package com.example.skillmatrix.controller;

import com.example.skillmatrix.dto.Dtos.EmployeeRequest;
import com.example.skillmatrix.dto.Dtos.EmployeeResponse;
import com.example.skillmatrix.dto.Dtos.ProficiencyRequest;
import com.example.skillmatrix.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    private final EmployeeService service;

    public EmployeeController(EmployeeService service) { this.service = service; }

    @GetMapping
    public List<EmployeeResponse> all() { return service.findAll(); }

    @GetMapping("/{id}")
    public EmployeeResponse one(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Add a skill to the employee, or change its proficiency. */
    @PutMapping("/{id}/skills")
    public EmployeeResponse setSkill(@PathVariable Long id, @Valid @RequestBody ProficiencyRequest r) {
        return service.setSkill(id, r);
    }

    @DeleteMapping("/{id}/skills/{skillId}")
    public EmployeeResponse removeSkill(@PathVariable Long id, @PathVariable Long skillId) {
        return service.removeSkill(id, skillId);
    }
}
