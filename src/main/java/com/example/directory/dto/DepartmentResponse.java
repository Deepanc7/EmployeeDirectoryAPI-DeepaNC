package com.example.directory.dto;

import com.example.directory.domain.Department;

public record DepartmentResponse(Long id, String name, String location, long headCount) {

    public static DepartmentResponse from(Department department, long headCount) {
        return new DepartmentResponse(department.getId(), department.getName(), department.getLocation(), headCount);
    }
}
