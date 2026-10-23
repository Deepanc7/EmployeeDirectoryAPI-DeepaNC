package com.example.directory.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "directory")
public record DirectoryProperties(@Min(1) int maxSkillsPerEmployee) {
}
