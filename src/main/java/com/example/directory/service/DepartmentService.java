package com.example.directory.service;

import com.example.directory.domain.Department;
import com.example.directory.dto.DepartmentRequest;
import com.example.directory.dto.DepartmentResponse;
import com.example.directory.dto.PageResponse;
import com.example.directory.exception.ConflictException;
import com.example.directory.exception.ResourceNotFoundException;
import com.example.directory.repository.DepartmentHeadCount;
import com.example.directory.repository.DepartmentRepository;
import com.example.directory.repository.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public DepartmentResponse create(DepartmentRequest request) {
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Department with name '" + name + "' already exists");
        }
        Department saved = departmentRepository.save(new Department(name, trimToNull(request.location())));
        return DepartmentResponse.from(saved, 0);
    }

    public PageResponse<DepartmentResponse> list(Pageable pageable) {
        Page<Department> page = departmentRepository.findAll(pageable);
        Map<Long, Long> counts = headCounts(page.getContent());
        return PageResponse.from(page.map(d -> DepartmentResponse.from(d, counts.getOrDefault(d.getId(), 0L))));
    }

    public DepartmentResponse get(Long id) {
        Department department = findOrThrow(id);
        return DepartmentResponse.from(department, employeeRepository.countByDepartmentId(id));
    }

    @Transactional
    public DepartmentResponse update(Long id, DepartmentRequest request) {
        Department department = findOrThrow(id);
        String name = request.name().trim();
        if (departmentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Department with name '" + name + "' already exists");
        }
        department.setName(name);
        department.setLocation(trimToNull(request.location()));
        Department saved = departmentRepository.save(department);
        return DepartmentResponse.from(saved, employeeRepository.countByDepartmentId(id));
    }

    @Transactional
    public void delete(Long id) {
        Department department = findOrThrow(id);
        if (employeeRepository.existsByDepartmentId(id)) {
            throw new ConflictException(
                    "Department " + id + " still has employees; reassign or delete them first");
        }
        departmentRepository.delete(department);
    }

    private Department findOrThrow(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Department", id));
    }

    private Map<Long, Long> headCounts(List<Department> departments) {
        if (departments.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = departments.stream().map(Department::getId).toList();
        return employeeRepository.countByDepartmentIds(ids).stream()
                .collect(Collectors.toMap(DepartmentHeadCount::departmentId, DepartmentHeadCount::total));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
