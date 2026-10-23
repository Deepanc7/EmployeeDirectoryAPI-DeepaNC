package com.example.directory.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Objects;

public class OffsetBasedPageRequest implements Pageable {

    private final long offset;
    private final int limit;
    private final Sort sort;

    public OffsetBasedPageRequest(long offset, int limit, Sort sort) {
        this.offset = offset;
        this.limit = limit;
        this.sort = sort;
    }

    @Override public int getPageNumber() { return (int) (offset / limit); }
    @Override public int getPageSize() { return limit; }
    @Override public long getOffset() { return offset; }
    @Override public Sort getSort() { return sort; }
    @Override public Pageable next() { return new OffsetBasedPageRequest(offset + limit, limit, sort); }
    @Override public Pageable previousOrFirst() { return hasPrevious() ? previous() : first(); }
    @Override public Pageable first() { return new OffsetBasedPageRequest(0, limit, sort); }
    @Override public Pageable withPage(int pageNumber) {
        return new OffsetBasedPageRequest((long) pageNumber * limit, limit, sort);
    }
    @Override public boolean hasPrevious() { return offset > 0; }

    private Pageable previous() {
        return new OffsetBasedPageRequest(Math.max(0, offset - limit), limit, sort);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OffsetBasedPageRequest other)) return false;
        return offset == other.offset && limit == other.limit && sort.equals(other.sort);
    }

    @Override
    public int hashCode() { return Objects.hash(offset, limit, sort); }
}