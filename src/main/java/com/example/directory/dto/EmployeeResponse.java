package com.example.directory.dto;

import com.example.directory.domain.Employee;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record EmployeeResponse(
        Long id,
        String name,
        String email,
        String designation,
        LocalDate joiningDate,
        Long departmentId,
        String departmentName,
        List<SkillResponse> skills) {

    public static EmployeeResponse from(Employee e) {
        List<SkillResponse> skills = e.getSkills().stream()
                .map(SkillResponse::from)
                .sorted(Comparator.comparing(SkillResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new EmployeeResponse(
                e.getId(), e.getName(), e.getEmail(), e.getDesignation(), e.getJoiningDate(),
                e.getDepartment().getId(), e.getDepartment().getName(), skills);
    }
}
