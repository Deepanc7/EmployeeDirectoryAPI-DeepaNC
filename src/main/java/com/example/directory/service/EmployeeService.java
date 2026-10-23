package com.example.directory.service;

import com.example.directory.config.DirectoryProperties;
import com.example.directory.domain.Department;
import com.example.directory.domain.Employee;
import com.example.directory.domain.Skill;
import com.example.directory.dto.EmployeeFilter;
import com.example.directory.dto.EmployeeRequest;
import com.example.directory.dto.EmployeeResponse;
import com.example.directory.dto.PageResponse;
import com.example.directory.exception.BusinessRuleException;
import com.example.directory.exception.ConflictException;
import com.example.directory.exception.ResourceNotFoundException;
import com.example.directory.repository.DepartmentRepository;
import com.example.directory.repository.EmployeeRepository;
import com.example.directory.repository.EmployeeSpecifications;
import com.example.directory.repository.SkillRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final SkillRepository skillRepository;
    private final DirectoryProperties directoryProperties;

    public EmployeeService(EmployeeRepository employeeRepository,
                           DepartmentRepository departmentRepository,
                           SkillRepository skillRepository,
                           DirectoryProperties directoryProperties) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
        this.skillRepository = skillRepository;
        this.directoryProperties = directoryProperties;
    }

    @Transactional
    public EmployeeResponse create(EmployeeRequest request) {
        String email = normalizeEmail(request.email());
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Employee with email '" + email + "' already exists");
        }
        Department department = findDepartment(request.departmentId());
        Set<Skill> skills = resolveSkills(request.skillIds());

        Employee employee = new Employee(request.name().trim(), email, trimToNull(request.designation()),
                request.joiningDate(), department);
        employee.replaceSkills(skills);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    public PageResponse<EmployeeResponse> search(EmployeeFilter filter, Pageable pageable) {
        Page<Employee> page = employeeRepository.findAll(EmployeeSpecifications.withFilter(filter), pageable);
        return PageResponse.from(page.map(EmployeeResponse::from));
    }

    public PageResponse<EmployeeResponse> listByDepartment(Long departmentId, Pageable pageable) {
        if (!departmentRepository.existsById(departmentId)) {
            throw ResourceNotFoundException.of("Department", departmentId);
        }
        return PageResponse.from(employeeRepository.findByDepartmentId(departmentId, pageable)
                .map(EmployeeResponse::from));
    }

    public EmployeeResponse get(Long id) {
        return EmployeeResponse.from(findDetailed(id));
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeRequest request) {
        Employee employee = findDetailed(id);

        String email = normalizeEmail(request.email());
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("Employee with email '" + email + "' already exists");
        }
        if (!employee.getDepartment().getId().equals(request.departmentId())) {
            employee.setDepartment(findDepartment(request.departmentId()));
        }
        if (request.skillIds() != null) {
            employee.replaceSkills(resolveSkills(request.skillIds()));
        }
        employee.setName(request.name().trim());
        employee.setEmail(email);
        employee.setDesignation(trimToNull(request.designation()));
        employee.setJoiningDate(request.joiningDate());
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public void delete(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", id));
        employeeRepository.delete(employee); // also removes employee_skill rows (owning side)
    }

    @Transactional
    public EmployeeResponse addSkill(Long employeeId, Long skillId) {
        Employee employee = findDetailed(employeeId);
        Skill skill = skillRepository.findById(skillId)
                .orElseThrow(() -> ResourceNotFoundException.of("Skill", skillId));

        if (employee.getSkills().contains(skill)) {
            throw new ConflictException("Employee " + employeeId + " already has skill " + skillId);
        }
        int max = directoryProperties.maxSkillsPerEmployee();
        if (employee.getSkills().size() >= max) {
            throw new BusinessRuleException(
                    "Employee " + employeeId + " already has the maximum of " + max + " skills");
        }
        employee.getSkills().add(skill);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    @Transactional
    public EmployeeResponse removeSkill(Long employeeId, Long skillId) {
        Employee employee = findDetailed(employeeId);
        Skill assigned = employee.getSkills().stream()
                .filter(s -> skillId.equals(s.getId()))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Employee " + employeeId + " does not have skill " + skillId));
        employee.getSkills().remove(assigned);
        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    // ---- helpers ----

    private Employee findDetailed(Long id) {
        return employeeRepository.findDetailedById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Employee", id));
    }

    private Department findDepartment(Long id) {
        return departmentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Department", id));
    }

    /** Validates the max-skills rule and that every id exists; returns the managed Skill entities. */
    private Set<Skill> resolveSkills(Set<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return Set.of();
        }
        int max = directoryProperties.maxSkillsPerEmployee();
        if (skillIds.size() > max) {
            throw new BusinessRuleException("An employee can have at most " + max + " skills");
        }
        Set<Skill> found = new HashSet<>(skillRepository.findAllById(skillIds));
        if (found.size() != skillIds.size()) {
            Set<Long> missing = new TreeSet<>(skillIds);
            found.forEach(s -> missing.remove(s.getId()));
            throw new ResourceNotFoundException("Skills not found: " + missing);
        }
        return found;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
