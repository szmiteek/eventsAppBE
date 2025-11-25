package com.eventsApp.employee;

import com.eventsApp.employee.model.Employee;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    public void createEmployee(@RequestBody Employee employee) {
        employeeRepository.save(employee);
    }

    public Page<Employee> getAll(int page, int size) {
        return employeeRepository.findAll(PageRequest.of(page, size));
    }


    public void deleteEmployee(@RequestParam int id) {
        employeeRepository.deleteById(id);
    }

    public void updateEmployee(int id, Employee employee) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));

        e.setFirstName(employee.getFirstName());
        e.setLastName(employee.getLastName());
        e.setHourlyRate(employee.getHourlyRate());

        employeeRepository.save(employee);
    }
}
