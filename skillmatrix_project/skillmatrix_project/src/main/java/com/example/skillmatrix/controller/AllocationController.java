package com.example.skillmatrix.controller;

import com.example.skillmatrix.dto.Dtos.AllocationRequest;
import com.example.skillmatrix.dto.Dtos.AllocationResponse;
import com.example.skillmatrix.dto.Dtos.AllocationUpdateRequest;
import com.example.skillmatrix.service.AllocationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/allocations")
public class AllocationController {
    private final AllocationService service;

    public AllocationController(AllocationService service) { this.service = service; }

    @GetMapping
    public List<AllocationResponse> all() { return service.findAll(); }

    @GetMapping("/{id}")
    public AllocationResponse one(@PathVariable Long id) { return service.findById(id); }

    @PostMapping
    public ResponseEntity<AllocationResponse> create(@Valid @RequestBody AllocationRequest r) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(r));
    }

    @PutMapping("/{id}")
    public AllocationResponse update(@PathVariable Long id, @Valid @RequestBody AllocationUpdateRequest r) {
        return service.update(id, r.percentage());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
