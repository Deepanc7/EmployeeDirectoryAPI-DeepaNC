package com.example.directory.service;

import com.example.directory.config.DirectoryProperties;
import com.example.directory.domain.Department;
import com.example.directory.domain.Employee;
import com.example.directory.domain.Skill;
import com.example.directory.dto.EmployeeFilter;
import com.example.directory.dto.EmployeeRequest;
import com.example.directory.dto.EmployeeResponse;
import com.example.directory.dto.PageResponse;
import com.example.directory.dto.SkillResponse;
import com.example.directory.exception.BusinessRuleException;
import com.example.directory.exception.ConflictException;
import com.example.directory.exception.ResourceNotFoundException;
import com.example.directory.repository.DepartmentRepository;
import com.example.directory.repository.EmployeeRepository;
import com.example.directory.repository.SkillRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    private static final int MAX_SKILLS = 2;
    private static final LocalDate JOINED = LocalDate.of(2023, 1, 15);

    @Mock EmployeeRepository employeeRepository;
    @Mock DepartmentRepository departmentRepository;
    @Mock SkillRepository skillRepository;

    private EmployeeService service;
    private Department engineering;
    private Skill java;
    private Skill sql;
    private Skill react;

    @BeforeEach
    void setUp() {
        // The limit is injected exactly as in production (from directory.max-skills-per-employee).
        service = new EmployeeService(employeeRepository, departmentRepository, skillRepository,
                new DirectoryProperties(MAX_SKILLS));
        engineering = withId(new Department("Engineering", "BLR"), 1L);
        java = withId(new Skill("Java", "Backend"), 10L);
        sql = withId(new Skill("SQL", "Data"), 11L);
        react = withId(new Skill("React", "Frontend"), 12L);
    }

    private static <T> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private Employee employee(Long id, Skill... skills) {
        Employee e = withId(new Employee("Asha", "asha@corp.com", "Engineer", JOINED, engineering), id);
        e.replaceSkills(Set.of(skills));
        return e;
    }

    private static EmployeeRequest request(String email, Long departmentId, Set<Long> skillIds) {
        return new EmployeeRequest("  Asha Rao ", email, "Engineer", JOINED, departmentId, skillIds);
    }

    // ---------- create ----------

    @Test
    void create_withSkills_savesNormalisedEmployee() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(engineering));
        Set<Long> ids = Set.of(10L, 11L);
        when(skillRepository.findAllById(ids)).thenReturn(List.of(java, sql));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> withId(inv.getArgument(0), 100L));

        EmployeeResponse result = service.create(request("  Asha@Corp.com ", 1L, ids));

        assertThat(result.id()).isEqualTo(100L);
        assertThat(result.name()).isEqualTo("Asha Rao");
        assertThat(result.email()).isEqualTo("asha@corp.com");
        assertThat(result.departmentName()).isEqualTo("Engineering");
        assertThat(result.skills()).extracting(SkillResponse::name).containsExactly("Java", "SQL");
    }

    @Test
    void create_withoutSkillIds_createsEmployeeWithNoSkills() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(engineering));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> withId(inv.getArgument(0), 100L));

        EmployeeResponse result = service.create(request("asha@corp.com", 1L, null));

        assertThat(result.skills()).isEmpty();
    }

    @Test
    void create_duplicateEmail_throwsConflict() {
        when(employeeRepository.existsByEmailIgnoreCase("asha@corp.com")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("ASHA@corp.com", 1L, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("asha@corp.com");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void create_unknownDepartment_throwsNotFound() {
        when(departmentRepository.findById(42L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(request("asha@corp.com", 42L, null)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Department with id 42 not found");
    }

    @Test
    void create_unknownSkillId_throwsNotFoundListingMissingIds() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(engineering));
        Set<Long> ids = Set.of(10L, 99L);
        when(skillRepository.findAllById(ids)).thenReturn(List.of(java));

        assertThatThrownBy(() -> service.create(request("asha@corp.com", 1L, ids)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Skills not found: [99]");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void create_moreSkillsThanConfiguredLimit_throwsBusinessRuleViolation() {
        when(departmentRepository.findById(1L)).thenReturn(Optional.of(engineering));

        assertThatThrownBy(() -> service.create(request("asha@corp.com", 1L, Set.of(10L, 11L, 12L))))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("at most " + MAX_SKILLS);
        verify(employeeRepository, never()).save(any());
    }

    // ---------- read ----------

    @Test
    void get_returnsEmployeeWithSkills() {
        when(employeeRepository.findDetailedById(5L)).thenReturn(Optional.of(employee(5L, java)));

        EmployeeResponse result = service.get(5L);

        assertThat(result.id()).isEqualTo(5L);
        assertThat(result.departmentId()).isEqualTo(1L);
        assertThat(result.skills()).extracting(SkillResponse::id).containsExactly(10L);
    }

    @Test
    void get_unknownId_throwsNotFound() {
        when(employeeRepository.findDetailedById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(5L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Employee with id 5 not found");
    }

    @Test
    void search_delegatesToRepositoryWithPageable() {
        Pageable pageable = PageRequest.of(0, 5);
        when(employeeRepository.findAll(ArgumentMatchers.<Specification<Employee>>any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(employee(5L, java)), pageable, 1));

        PageResponse<EmployeeResponse> result =
                service.search(new EmployeeFilter(1L, 10L, null, null, null, null, null), pageable);

        assertThat(result.totalElements()).isEqualTo(1);
        assertThat(result.content()).hasSize(1);
    }

    @Test
    void listByDepartment_unknownDepartment_throwsNotFound() {
        when(departmentRepository.existsById(9L)).thenReturn(false);

        assertThatThrownBy(() -> service.listByDepartment(9L, PageRequest.of(0, 5)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listByDepartment_returnsMembers() {
        Pageable pageable = PageRequest.of(0, 5);
        when(departmentRepository.existsById(1L)).thenReturn(true);
        when(employeeRepository.findByDepartmentId(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(employee(5L)), pageable, 1));

        assertThat(service.listByDepartment(1L, pageable).content()).hasSize(1);
    }

    // ---------- update ----------

    @Test
    void update_changesDepartmentAndReplacesSkills() {
        Employee existing = employee(5L, java);
        Department hr = withId(new Department("HR", null), 2L);
        when(employeeRepository.findDetailedById(5L)).thenReturn(Optional.of(existing));
        when(departmentRepository.findById(2L)).thenReturn(Optional.of(hr));
        Set<Long> ids = Set.of(11L);
        when(skillRepository.findAllById(ids)).thenReturn(List.of(sql));
        when(employeeRepository.save(existing)).thenReturn(existing);

        EmployeeResponse result = service.update(5L, request("new@corp.com", 2L, ids));

        assertThat(result.departmentName()).isEqualTo("HR");
        assertThat(result.email()).isEqualTo("new@corp.com");
        assertThat(result.skills()).extracting(SkillResponse::name).containsExactly("SQL");
    }

    @Test
    void update_nullSkillIds_keepsExistingSkills() {
        Employee existing = employee(5L, java);
        when(employeeRepository.findDetailedById(5L)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(existing)).thenReturn(existing);

        EmployeeResponse result = service.update(5L, request("asha@corp.com", 1L, null));

        assertThat(result.skills()).extracting(SkillResponse::name).containsExactly("Java");
        verify(skillRepository, never()).findAllById(any());
        verify(departmentRepository, never()).findById(any()); // department unchanged -> no lookup
    }

    @Test
    void update_emailUsedByAnotherEmployee_throwsConflict() {
        when(employeeRepository.findDetailedById(5L)).thenReturn(Optional.of(employee(5L)));
        when(employeeRepository.existsByEmailIgnoreCaseAndIdNot("taken@corp.com", 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(5L, request("taken@corp.com", 1L, null)))
                .isInstanceOf(ConflictException.class);
        verify(employeeRepository, never()).save(any());
    }

    // ---------- delete ----------

    @Test
    void delete_existingEmployee_deletesIt() {
        Employee existing = employee(5L, java);
        when(employeeRepository.findById(5L)).thenReturn(Optional.of(existing));

        service.delete(5L);

        verify(employeeRepository).delete(existing);
    }

    @Test
    void delete_unknownEmployee_throwsNotFound() {
        when(employeeRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- add / remove skill ----------

    @Test
    void addSkill_belowLimit_addsSkill() {
        Employee existing = employee(5L, java);
        when(employeeRepository.findDetailedById(5L)).thenReturn(Optional.of(existing));
        when(skillRepository.findById(11L)).thenReturn(Optional.of(sql));
        when(employeeRepository.save(existing)).thenReturn(existing);

        EmployeeResponse result = service.addSkill(5L, 11L);

        assertThat(result.skills()).extracting(SkillResponse::name).containsExactly("Java", "SQL");
    }

    @Test
    void addSkill_atConfiguredLimit_throwsBusinessRuleViolation() {
        Employee full = employee(5L, java, sql); // MAX_SKILLS == 2
        when(employeeRepository.findDetailedById(5L)).thenReturn(Optional.of(full));
        when(skillRepository.findById(12L)).thenReturn(Optional.of(react));

        assertThatThrownBy(() -> service.addSkill(5L, 12L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Employee 5 already has the maximum of 2 skills");
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void addSkill_alreadyAssigned_throwsConflictEvenWhenAtLimit() {
        Employee full = employee(5L, java, sql);
        when(employeeRepository.findDetailedById(5L)).thenReturn(Optional.of(full));
        when(skillRepository.findById(10L)).thenReturn(Optional.of(java));

        assertThatThrownBy(() -> service.addSkill(5L, 10L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void addSkill_unknownSkill_throwsNotFound() {
        when(employeeRepository.findDetailedById(5L)).thenReturn(Optional.of(employee(5L)));
        when(skillRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addSkill(5L, 77L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Skill with id 77 not found");
    }

    @Test
    void removeSkill_assignedSkill_removesIt() {
        Employee existing = employee(5L, java, sql);
        when(employeeRepository.findDetailedById(5L)).thenReturn(Optional.of(existing));
        when(employeeRepository.save(existing)).thenReturn(existing);

        EmployeeResponse result = service.removeSkill(5L, 10L);

        assertThat(result.skills()).extracting(SkillResponse::name).containsExactly("SQL");
    }

    @Test
    void removeSkill_notAssigned_throwsNotFound() {
        when(employeeRepository.findDetailedById(5L)).thenReturn(Optional.of(employee(5L, java)));

        assertThatThrownBy(() -> service.removeSkill(5L, 11L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Employee 5 does not have skill 11");
        verify(employeeRepository, never()).save(any());
    }
}
