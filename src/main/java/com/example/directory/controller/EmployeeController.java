package com.example.directory.controller;

import com.example.directory.dto.EmployeeFilter;
import com.example.directory.dto.EmployeeRequest;
import com.example.directory.dto.EmployeeResponse;
import com.example.directory.dto.PageResponse;
import com.example.directory.service.EmployeeService;
import com.example.directory.util.PaginationSupport;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.Set;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private static final Set<String> SORTABLE = Set.of("id", "name", "email", "designation", "joiningDate");

    private final EmployeeService employeeService;
    private final PaginationSupport pagination;

    public EmployeeController(EmployeeService employeeService, PaginationSupport pagination) {
        this.employeeService = employeeService;
        this.pagination = pagination;
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse created = employeeService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public PageResponse<EmployeeResponse> search(
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long skillId,
            @RequestParam(name = "skill", required = false) String skillName,
            @RequestParam(required = false) String designation,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinedAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate joinedBefore,
            @RequestParam(required = false) Integer offset,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort) {
        EmployeeFilter filter = new EmployeeFilter(
                departmentId, skillId, skillName, designation, name, joinedAfter, joinedBefore);
        return employeeService.search(filter, pagination.pageable(offset, limit, sort, SORTABLE, "name,asc"));
    }

    @GetMapping("/{id}")
    public EmployeeResponse get(@PathVariable Long id) {
        return employeeService.get(id);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeRequest request) {
        return employeeService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        employeeService.delete(id);
    }

    @PostMapping("/{id}/skills/{skillId}")
    public EmployeeResponse addSkill(@PathVariable Long id, @PathVariable Long skillId) {
        return employeeService.addSkill(id, skillId);
    }

    @DeleteMapping("/{id}/skills/{skillId}")
    public EmployeeResponse removeSkill(@PathVariable Long id, @PathVariable Long skillId) {
        return employeeService.removeSkill(id, skillId);
    }
}
