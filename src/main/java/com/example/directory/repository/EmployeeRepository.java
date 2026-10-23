package com.example.directory.repository;

import com.example.directory.domain.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    @EntityGraph(attributePaths = {"department", "skills"})
    Optional<Employee> findDetailedById(Long id);

    Page<Employee> findByDepartmentId(Long departmentId, Pageable pageable);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByDepartmentId(Long departmentId);

    boolean existsBySkillsId(Long skillId);

    long countByDepartmentId(Long departmentId);

    /** One grouped query to compute head counts for a whole page of departments (avoids N+1). */
    @Query("select new com.example.directory.repository.DepartmentHeadCount(e.department.id, count(e)) "
            + "from Employee e where e.department.id in :ids group by e.department.id")
    List<DepartmentHeadCount> countByDepartmentIds(@Param("ids") Collection<Long> ids);
}
