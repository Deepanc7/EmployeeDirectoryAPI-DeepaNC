package com.example.directory.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "pagination")
public record PaginationProperties(@Min(1) int defaultPageSize, @Min(1) int maxPageSize) {
}
