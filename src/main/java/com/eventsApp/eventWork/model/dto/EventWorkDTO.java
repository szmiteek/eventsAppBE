package com.eventsApp.eventWork.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class EventWorkDTO {
    private int id;
    private int eventId;
    private int employeeId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate workDate;
    private double hoursWorked;
    private String description;

}
