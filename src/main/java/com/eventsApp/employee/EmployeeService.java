package com.eventsApp.employee;

import com.eventsApp.employee.model.Employee;
import com.eventsApp.employee.model.dto.EmployeeDTO;
import com.eventsApp.employee.model.command.EmployeeCreateCommand;
import com.eventsApp.employee.model.command.EmployeeUpdateCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.eventsApp.employee.EmployeeMapper.mapToDTO;
import static com.eventsApp.employee.EmployeeMapper.updateFromCommand;

@Service
@RequiredArgsConstructor
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    public EmployeeDTO create(EmployeeCreateCommand command) {
        Employee e = EmployeeMapper.fromEmployeeCreateCommand(command);
        return mapToDTO(employeeRepository.save(e));
    }

    public Page<EmployeeDTO> getAll(Pageable pageable) {
        return employeeRepository.findAll(pageable).map(EmployeeMapper::mapToDTO);
    }

    public void delete(int id) {
        employeeRepository.deleteById(id);
    }

    @Transactional
    public EmployeeDTO update(int id, EmployeeUpdateCommand command) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found"));
        updateFromCommand(employee, command);
        return mapToDTO(employee);
    }
}
