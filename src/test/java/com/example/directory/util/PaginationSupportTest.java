package com.example.directory.util;

import com.example.directory.config.PaginationProperties;
import com.example.directory.exception.InvalidRequestException;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationSupportTest {

    private static final Set<String> SORTABLE = Set.of("id", "name");
    private final PaginationSupport support = new PaginationSupport(new PaginationProperties(20, 50));

    @Test
    void usesConfiguredDefaultPageSizeAndDefaultSort() {
        Pageable pageable = support.pageable(null, null, null, SORTABLE, "name,asc");

        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertThat(pageable.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(pageable.getSort().getOrderFor("id")).isNotNull(); // tie-breaker
    }

    @Test
    void honoursExplicitValues() {
        Pageable pageable = support.pageable(2, 10, "name,desc", SORTABLE, "name,asc");

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(10);
        assertThat(pageable.getSort().getOrderFor("name").getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void rejectsSizeAboveConfiguredMaximum() {
        assertThatThrownBy(() -> support.pageable(0, 51, null, SORTABLE, "name"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("between 1 and 50");
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> support.pageable(-1, 5, null, SORTABLE, "name"))
                .isInstanceOf(InvalidRequestException.class);
    }

    @Test
    void rejectsNonWhitelistedSortField() {
        assertThatThrownBy(() -> support.pageable(0, 5, "password,asc", SORTABLE, "name"))
                .isInstanceOf(InvalidRequestException.class)
                .hasMessageContaining("Cannot sort by 'password'");
    }

    @Test
    void rejectsUnknownSortDirection() {
        assertThatThrownBy(() -> support.pageable(0, 5, "name,sideways", SORTABLE, "name"))
                .isInstanceOf(InvalidRequestException.class);
    }
}
