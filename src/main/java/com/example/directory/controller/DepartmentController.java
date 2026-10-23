package com.example.directory.controller;

import com.example.directory.dto.DepartmentRequest;
import com.example.directory.dto.DepartmentResponse;
import com.example.directory.dto.EmployeeResponse;
import com.example.directory.dto.PageResponse;
import com.example.directory.service.DepartmentService;
import com.example.directory.service.EmployeeService;
import com.example.directory.util.PaginationSupport;
import jakarta.validation.Valid;
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
import java.util.Set;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private static final Set<String> SORTABLE = Set.of("id", "name", "location");
    private static final Set<String> EMPLOYEE_SORTABLE = Set.of("id", "name", "email", "designation", "joiningDate");

    private final DepartmentService departmentService;
    private final EmployeeService employeeService;
    private final PaginationSupport pagination;

    public DepartmentController(DepartmentService departmentService, EmployeeService employeeService,
                                PaginationSupport pagination) {
        this.departmentService = departmentService;
        this.employeeService = employeeService;
        this.pagination = pagination;
    }

    @PostMapping
    public ResponseEntity<DepartmentResponse> create(@Valid @RequestBody DepartmentRequest request) {
        DepartmentResponse created = departmentService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public PageResponse<DepartmentResponse> list(@RequestParam(required = false) Integer page,
                                                 @RequestParam(required = false) Integer size,
                                                 @RequestParam(required = false) String sort) {
        return departmentService.list(pagination.pageable(page, size, sort, SORTABLE, "name,asc"));
    }

    @GetMapping("/{id}")
    public DepartmentResponse get(@PathVariable Long id) {
        return departmentService.get(id);
    }

    @PutMapping("/{id}")
    public DepartmentResponse update(@PathVariable Long id, @Valid @RequestBody DepartmentRequest request) {
        return departmentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        departmentService.delete(id);
    }

    @GetMapping("/{id}/employees")
    public PageResponse<EmployeeResponse> employees(@PathVariable Long id,
                                                    @RequestParam(required = false) Integer offset,
                                                    @RequestParam(required = false) Integer limit,
                                                    @RequestParam(required = false) String sort) {
        return employeeService.listByDepartment(id,
                pagination.pageable(offset, limit, sort, EMPLOYEE_SORTABLE, "name,asc"));
    }
}
