package com.example.directory.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record EmployeeRequest(
        @NotBlank(message = "name is required") @Size(max = 100) String name,
        @NotBlank(message = "email is required") @Email(message = "email must be a valid address") @Size(max = 150) String email,
        @Size(max = 100) String designation,
        @NotNull(message = "joiningDate is required") LocalDate joiningDate,
        @NotNull(message = "departmentId is required") Long departmentId,
        Set<@NotNull(message = "skillIds must not contain null") Long> skillIds) {
}
