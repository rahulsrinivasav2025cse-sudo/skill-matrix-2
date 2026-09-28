package com.example.skillmatrix.controller;

import com.example.skillmatrix.dto.Dtos.SkillRequest;
import com.example.skillmatrix.dto.Dtos.SkillResponse;
import com.example.skillmatrix.service.SkillService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {
    private final SkillService service;

    public SkillController(SkillService service) { this.service = service; }

    @GetMapping
    public List<SkillResponse> all() { return service.findAll(); }

    @GetMapping("/{id}")
    public SkillResponse one(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<SkillResponse> create(@Valid @RequestBody SkillRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));
    }

    @PutMapping("/{id}")
    public SkillResponse update(@PathVariable Long id, @Valid @RequestBody SkillRequest r) {
        return service.update(id, r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
