package com.eventsApp.employee;

import com.eventsApp.employee.model.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    Page<Employee> findAllByTenantId(Integer tenantId, Pageable pageable);
}
