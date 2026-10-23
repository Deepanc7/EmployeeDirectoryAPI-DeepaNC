package com.example.directory.controller;

import com.example.directory.config.AppProperties;
import com.example.directory.config.DirectoryProperties;
import com.example.directory.config.PaginationProperties;
import com.example.directory.dto.InfoResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/info")
public class InfoController {

    private final AppProperties app;
    private final DirectoryProperties directory;
    private final PaginationProperties pagination;

    public InfoController(AppProperties app, DirectoryProperties directory, PaginationProperties pagination) {
        this.app = app;
        this.directory = directory;
        this.pagination = pagination;
    }

    @GetMapping
    public InfoResponse info() {
        return new InfoResponse(app.name(), app.version(),
                directory.maxSkillsPerEmployee(), pagination.defaultPageSize());
    }
}
