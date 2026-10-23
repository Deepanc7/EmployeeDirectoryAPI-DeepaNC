package com.example.directory.repository;

import com.example.directory.domain.Employee;
import com.example.directory.dto.EmployeeFilter;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import com.example.directory.domain.Skill;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    public static Specification<Employee> withFilter(EmployeeFilter f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (f.departmentId() != null) {
                predicates.add(cb.equal(root.get("department").get("id"), f.departmentId()));
            }
            if (f.skillId() != null || StringUtils.hasText(f.skillName())) {
                Join<Employee, Skill> skill = root.join("skills");
                query.distinct(true);
                if (f.skillId() != null) {
                    predicates.add(cb.equal(skill.get("id"), f.skillId()));
                }
                if (StringUtils.hasText(f.skillName())) {
                    predicates.add(cb.equal(cb.lower(skill.get("name")), f.skillName().trim().toLowerCase()));
                }
            }
            if (StringUtils.hasText(f.designation())) {
                predicates.add(cb.equal(cb.lower(root.get("designation")), f.designation().trim().toLowerCase()));
            }
            if (StringUtils.hasText(f.name())) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + f.name().trim().toLowerCase() + "%"));
            }
            if (f.joinedAfter() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("joiningDate"), f.joinedAfter()));
            }
            if (f.joinedBefore() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDate>get("joiningDate"), f.joinedBefore()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
