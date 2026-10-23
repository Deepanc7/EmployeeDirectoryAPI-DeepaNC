package com.example.directory.util;

import com.example.directory.config.PaginationProperties;
import com.example.directory.exception.InvalidRequestException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;
import java.util.TreeSet;

@Component
public class PaginationSupport {

    private final PaginationProperties properties;

    public PaginationSupport(PaginationProperties properties) {
        this.properties = properties;
    }

    /**
     * @param offset      rows to skip (default 0)
     * @param limit       max rows to return (default pagination.default-page-size)
     * @param sort        "field" or "field,asc|desc"; may be null
     * @param sortable    whitelist of sortable property names
     * @param defaultSort used when {@code sort} is blank, same format
     */
    public Pageable pageable(Integer offset, Integer limit, String sort, Set<String> sortable, String defaultSort) {
        long rowOffset = offset == null ? 0 : offset;
        int rowLimit = limit == null ? properties.defaultPageSize() : limit;
        if (rowOffset < 0) {
            throw new InvalidRequestException("offset must be >= 0");
        }
        if (rowLimit < 1 || rowLimit > properties.maxPageSize()) {
            throw new InvalidRequestException("limit must be between 1 and " + properties.maxPageSize());
        }
        return new OffsetBasedPageRequest(rowOffset, rowLimit, parseSort(sort, sortable, defaultSort));
    }

    private Sort parseSort(String sort, Set<String> sortable, String defaultSort) {
        String spec = StringUtils.hasText(sort) ? sort : defaultSort;
        String[] parts = spec.split(",");
        String field = parts[0].trim();
        if (!sortable.contains(field)) {
            throw new InvalidRequestException(
                    "Cannot sort by '" + field + "'. Allowed fields: " + new TreeSet<>(sortable));
        }
        Sort.Direction direction = Sort.Direction.ASC;
        if (parts.length > 1) {
            direction = Sort.Direction.fromOptionalString(parts[1].trim())
                    .orElseThrow(() -> new InvalidRequestException("Sort direction must be 'asc' or 'desc'"));
        }
        Sort primary = Sort.by(direction, field);
        // Secondary key keeps row order deterministic when the primary key has ties.
        return "id".equals(field) ? primary : primary.and(Sort.by("id"));
    }
}