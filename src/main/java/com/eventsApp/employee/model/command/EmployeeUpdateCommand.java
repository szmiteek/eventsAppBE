package com.eventsApp.employee.model.command;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class EmployeeUpdateCommand {
    @NotBlank(message = "EMPTY_VALUE")
    private String firstName;

    @NotBlank(message = "EMPTY_VALUE")
    private String lastName;

    @Min(value = 30, message = "TO_LOW_VALUE")
    private double hourlyRate;
}
