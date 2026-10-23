package com.example.directory.service;

import com.example.directory.domain.Skill;
import com.example.directory.dto.PageResponse;
import com.example.directory.dto.SkillRequest;
import com.example.directory.dto.SkillResponse;
import com.example.directory.exception.ConflictException;
import com.example.directory.exception.ResourceNotFoundException;
import com.example.directory.repository.EmployeeRepository;
import com.example.directory.repository.SkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class SkillService {

    private final SkillRepository skillRepository;
    private final EmployeeRepository employeeRepository;

    public SkillService(SkillRepository skillRepository, EmployeeRepository employeeRepository) {
        this.skillRepository = skillRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public SkillResponse create(SkillRequest request) {
        String name = request.name().trim();
        if (skillRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Skill with name '" + name + "' already exists");
        }
        return SkillResponse.from(skillRepository.save(new Skill(name, trimToNull(request.category()))));
    }

    public PageResponse<SkillResponse> list(String category, Pageable pageable) {
        Page<Skill> page = StringUtils.hasText(category)
                ? skillRepository.findByCategoryIgnoreCase(category.trim(), pageable)
                : skillRepository.findAll(pageable);
        return PageResponse.from(page.map(SkillResponse::from));
    }

    public SkillResponse get(Long id) {
        return SkillResponse.from(findOrThrow(id));
    }

    @Transactional
    public SkillResponse update(Long id, SkillRequest request) {
        Skill skill = findOrThrow(id);
        String name = request.name().trim();
        if (skillRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Skill with name '" + name + "' already exists");
        }
        skill.setName(name);
        skill.setCategory(trimToNull(request.category()));
        return SkillResponse.from(skillRepository.save(skill));
    }

    @Transactional
    public void delete(Long id) {
        Skill skill = findOrThrow(id);
        if (employeeRepository.existsBySkillsId(id)) {
            throw new ConflictException("Skill " + id + " is assigned to employees; remove it from them first");
        }
        skillRepository.delete(skill);
    }

    private Skill findOrThrow(Long id) {
        return skillRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Skill", id));
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
