package com.eventsApp.employee;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.employee.model.Employee;
import com.eventsApp.employee.model.dto.EmployeeDTO;
import com.eventsApp.employee.model.command.EmployeeCreateCommand;
import com.eventsApp.employee.model.command.EmployeeUpdateCommand;
import com.eventsApp.exceptions.EventApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.eventsApp.employee.EmployeeMapper.mapToDTO;
import static com.eventsApp.employee.EmployeeMapper.updateFromCommand;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;
    private final CurrentTenantProvider currentTenantProvider;

    public EmployeeDTO create(EmployeeCreateCommand command) {
        Employee e = EmployeeMapper.fromEmployeeCreateCommand(command);
        e.setTenantId(currentTenantProvider.requireTenantId());
        return mapToDTO(employeeRepository.save(e));
    }

    public Page<EmployeeDTO> getAll(Pageable pageable) {
        return employeeRepository.findAllByTenantId(currentTenantProvider.requireTenantId(), pageable).map(EmployeeMapper::mapToDTO);
    }

    public void delete(int id) {
        Employee employee = getOwnedEmployee(id);
        employeeRepository.delete(employee);
    }

    @Transactional
    public EmployeeDTO update(int id, EmployeeUpdateCommand command) {
        Employee employee = getOwnedEmployee(id);
        updateFromCommand(employee, command);
        return mapToDTO(employee);
    }

    private Employee getOwnedEmployee(int id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Employee not found", HttpStatus.NOT_FOUND));
        if (!Integer.valueOf(employee.getTenantId()).equals(currentTenantProvider.requireTenantId())) {
            throw new EventApiException("Employee not found", HttpStatus.NOT_FOUND);
        }
        return employee;
    }
}
