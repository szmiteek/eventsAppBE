package com.eventsApp.employee.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeeDTO {
    private int id;
    private String firstName;
    private String lastName;
    private double hourlyRate;
}
