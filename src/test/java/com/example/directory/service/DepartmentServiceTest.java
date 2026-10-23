package com.example.directory.service;

import com.example.directory.domain.Department;
import com.example.directory.dto.DepartmentRequest;
import com.example.directory.dto.DepartmentResponse;
import com.example.directory.dto.PageResponse;
import com.example.directory.exception.ConflictException;
import com.example.directory.exception.ResourceNotFoundException;
import com.example.directory.repository.DepartmentHeadCount;
import com.example.directory.repository.DepartmentRepository;
import com.example.directory.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceTest {

    @Mock DepartmentRepository departmentRepository;
    @Mock EmployeeRepository employeeRepository;
    @InjectMocks DepartmentService service;

    private static Department dept(Long id, String name, String location) {
        Department d = new Department(name, location);
        ReflectionTestUtils.setField(d, "id", id);
        return d;
    }

    @Test
    void create_trimsNameAndReturnsZeroHeadCount() {
        when(departmentRepository.existsByNameIgnoreCase("Engineering")).thenReturn(false);
        when(departmentRepository.save(any(Department.class))).thenAnswer(inv -> {
            Department d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", 1L);
            return d;
        });

        DepartmentResponse result = service.create(new DepartmentRequest("  Engineering ", "Bengaluru"));

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("Engineering");
        assertThat(result.location()).isEqualTo("Bengaluru");
        assertThat(result.headCount()).isZero();
    }

    @Test
    void create_duplicateName_throwsConflictAndDoesNotSave() {
        when(departmentRepository.existsByNameIgnoreCase("Engineering")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new DepartmentRequest("Engineering", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Engineering");
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void get_returnsDepartmentWithComputedHeadCount() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept(1L, "Engineering", "BLR")));
        when(employeeRepository.countByDepartmentId(1L)).thenReturn(5L);

        DepartmentResponse result = service.get(1L);

        assertThat(result.headCount()).isEqualTo(5L);
        assertThat(result.name()).isEqualTo("Engineering");
    }

    @Test
    void get_unknownId_throwsNotFound() {
        when(departmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Department with id 99 not found");
    }

    @Test
    void list_computesHeadCountsWithSingleGroupedQuery() {
        Pageable pageable = PageRequest.of(0, 10);
        List<Department> departments = List.of(dept(1L, "Engineering", null), dept(2L, "HR", null));
        when(departmentRepository.findAll(pageable)).thenReturn(new PageImpl<>(departments, pageable, 2));
        when(employeeRepository.countByDepartmentIds(anyCollection()))
                .thenReturn(List.of(new DepartmentHeadCount(1L, 3L)));

        PageResponse<DepartmentResponse> result = service.list(pageable);

        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content()).extracting(DepartmentResponse::headCount).containsExactly(3L, 0L);
    }

    @Test
    void update_renamingToAnotherDepartmentsName_throwsConflict() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(dept(1L, "Engineering", null)));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("HR", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new DepartmentRequest("HR", null)))
                .isInstanceOf(ConflictException.class);
        verify(departmentRepository, never()).save(any());
    }

    @Test
    void update_changesFieldsAndPersists() {
        Department existing = dept(1L, "Engineering", "BLR");
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(departmentRepository.existsByNameIgnoreCaseAndIdNot("Platform", 1L)).thenReturn(false);
        when(departmentRepository.save(existing)).thenReturn(existing);
        when(employeeRepository.countByDepartmentId(1L)).thenReturn(2L);

        DepartmentResponse result = service.update(1L, new DepartmentRequest("Platform", "  "));

        assertThat(result.name()).isEqualTo("Platform");
        assertThat(result.location()).isNull(); // blank location is normalised to null
        assertThat(result.headCount()).isEqualTo(2L);
    }

    @Test
    void delete_departmentWithEmployees_throwsConflict() {
        Department d = dept(1L, "Engineering", null);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(d));
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ConflictException.class);
        verify(departmentRepository, never()).delete(any());
    }

    @Test
    void delete_emptyDepartment_deletesIt() {
        Department d = dept(1L, "Engineering", null);
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(d));
        when(employeeRepository.existsByDepartmentId(1L)).thenReturn(false);

        service.delete(1L);

        ArgumentCaptor<Department> captor = ArgumentCaptor.forClass(Department.class);
        verify(departmentRepository).delete(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1L);
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(departmentRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
