package com.eventsApp.eventWork.model.command;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EventWorkCreateCommand {
    private int employeeId;
    private int eventId;
    private LocalDate workDate;
    private double hoursWorked;
    private String description;
}
