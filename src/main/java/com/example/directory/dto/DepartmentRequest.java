package com.example.directory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentRequest(
        @NotBlank(message = "name is required") @Size(max = 100) String name,
        @Size(max = 150) String location) {
}
