package com.example.directory.service;

import com.example.directory.domain.Skill;
import com.example.directory.dto.PageResponse;
import com.example.directory.dto.SkillRequest;
import com.example.directory.dto.SkillResponse;
import com.example.directory.exception.ConflictException;
import com.example.directory.exception.ResourceNotFoundException;
import com.example.directory.repository.EmployeeRepository;
import com.example.directory.repository.SkillRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillServiceTest {

    @Mock SkillRepository skillRepository;
    @Mock EmployeeRepository employeeRepository;
    @InjectMocks SkillService service;

    private static Skill skill(Long id, String name, String category) {
        Skill s = new Skill(name, category);
        ReflectionTestUtils.setField(s, "id", id);
        return s;
    }

    @Test
    void create_persistsAndReturnsSkill() {
        when(skillRepository.existsByNameIgnoreCase("Java")).thenReturn(false);
        when(skillRepository.save(any(Skill.class))).thenAnswer(inv -> {
            Skill s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", 7L);
            return s;
        });

        SkillResponse result = service.create(new SkillRequest(" Java ", "Backend"));

        assertThat(result).isEqualTo(new SkillResponse(7L, "Java", "Backend"));
    }

    @Test
    void create_duplicateName_throwsConflict() {
        when(skillRepository.existsByNameIgnoreCase("Java")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new SkillRequest("Java", null)))
                .isInstanceOf(ConflictException.class);
        verify(skillRepository, never()).save(any());
    }

    @Test
    void list_withCategory_filtersCaseInsensitively() {
        Pageable pageable = PageRequest.of(0, 10);
        when(skillRepository.findByCategoryIgnoreCase("backend", pageable))
                .thenReturn(new PageImpl<>(List.of(skill(1L, "Java", "Backend")), pageable, 1));

        PageResponse<SkillResponse> result = service.list("backend", pageable);

        assertThat(result.content()).extracting(SkillResponse::name).containsExactly("Java");
        verify(skillRepository, never()).findAll(pageable);
    }

    @Test
    void list_withoutCategory_returnsEverything() {
        Pageable pageable = PageRequest.of(0, 10);
        when(skillRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(skill(1L, "Java", "Backend"), skill(2L, "React", "Frontend")),
                        pageable, 2));

        assertThat(service.list(null, pageable).content()).hasSize(2);
    }

    @Test
    void get_unknownId_throwsNotFound() {
        when(skillRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(3L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_renameToExistingSkill_throwsConflict() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill(1L, "Java", "Backend")));
        when(skillRepository.existsByNameIgnoreCaseAndIdNot("SQL", 1L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(1L, new SkillRequest("SQL", "Data")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void delete_skillAssignedToEmployees_throwsConflict() {
        when(skillRepository.findById(1L)).thenReturn(Optional.of(skill(1L, "Java", "Backend")));
        when(employeeRepository.existsBySkillsId(1L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L)).isInstanceOf(ConflictException.class);
        verify(skillRepository, never()).delete(any());
    }

    @Test
    void delete_unusedSkill_deletesIt() {
        Skill s = skill(1L, "Java", "Backend");
        when(skillRepository.findById(1L)).thenReturn(Optional.of(s));
        when(employeeRepository.existsBySkillsId(1L)).thenReturn(false);

        service.delete(1L);

        verify(skillRepository).delete(s);
    }
}
