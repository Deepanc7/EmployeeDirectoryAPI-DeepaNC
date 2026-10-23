package com.example.directory.dto;

import java.time.LocalDate;

public record EmployeeFilter(
        Long departmentId,
        Long skillId,
        String skillName,
        String designation,
        String name,
        LocalDate joinedAfter,
        LocalDate joinedBefore) {
}
