package com.example.directory.controller;

import com.example.directory.dto.PageResponse;
import com.example.directory.dto.SkillRequest;
import com.example.directory.dto.SkillResponse;
import com.example.directory.service.SkillService;
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
@RequestMapping("/api/skills")
public class SkillController {

    private static final Set<String> SORTABLE = Set.of("id", "name", "category");

    private final SkillService skillService;
    private final PaginationSupport pagination;

    public SkillController(SkillService skillService, PaginationSupport pagination) {
        this.skillService = skillService;
        this.pagination = pagination;
    }

    @PostMapping
    public ResponseEntity<SkillResponse> create(@Valid @RequestBody SkillRequest request) {
        SkillResponse created = skillService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public PageResponse<SkillResponse> list(@RequestParam(required = false) String category,
                                            @RequestParam(required = false) Integer offset,
                                            @RequestParam(required = false) Integer limit,
                                            @RequestParam(required = false) String sort) {
        return skillService.list(category, pagination.pageable(offset, limit, sort, SORTABLE, "name,asc"));
    }

    @GetMapping("/{id}")
    public SkillResponse get(@PathVariable Long id) {
        return skillService.get(id);
    }

    @PutMapping("/{id}")
    public SkillResponse update(@PathVariable Long id, @Valid @RequestBody SkillRequest request) {
        return skillService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        skillService.delete(id);
    }
}
