package com.eventsApp.employee;

import com.eventsApp.employee.model.Employee;
import com.eventsApp.employee.model.dto.EmployeeDTO;
import com.eventsApp.employee.model.command.EmployeeCreateCommand;
import com.eventsApp.employee.model.command.EmployeeUpdateCommand;

import java.util.Optional;

public class EmployeeMapper {

    public static Employee fromEmployeeCreateCommand(EmployeeCreateCommand command) {
        return Employee.builder()
                .firstName(command.getFirstName())
                .lastName(command.getLastName())
                .hourlyRate(command.getHourlyRate())
                .build();
    }

    public static EmployeeDTO mapToDTO(Employee employee) {
        return EmployeeDTO.builder()
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .hourlyRate(employee.getHourlyRate())
                .build();
    }

    public static void updateFromCommand(Employee employee, EmployeeUpdateCommand command) {
        Optional.ofNullable(command.getFirstName()).ifPresent(employee::setFirstName);
        Optional.ofNullable(command.getLastName()).ifPresent(employee::setLastName);
        Optional.ofNullable(command.getHourlyRate()).ifPresent(employee::setHourlyRate);
    }
}
