package com.example.directory.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(List<T> content, long offset, int limit, long totalElements, boolean hasNext) {

    public static <T> PageResponse<T> from(Page<T> page) {
        long offset = page.getPageable().getOffset();
        return new PageResponse<>(page.getContent(), offset, page.getSize(), page.getTotalElements(),
                offset + page.getNumberOfElements() < page.getTotalElements());
    }
}