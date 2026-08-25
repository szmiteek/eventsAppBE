package com.eventsApp.eventWork;

import com.eventsApp.eventWork.model.EventWork;
import com.eventsApp.eventWork.model.command.EventWorkCreateCommand;
import com.eventsApp.eventWork.model.dto.EventWorkDTO;


public class EventWorkMapper {

    public static EventWork fromCreateCommand(EventWorkCreateCommand command) {
        return EventWork.builder()
                .description(command.getDescription())
                .workDate(command.getWorkDate())
                .hoursWorked(command.getHoursWorked())
                .build();
    }

    public static EventWorkDTO mapToDTO(EventWork eventWork) {
        return EventWorkDTO.builder()
                .eventId(eventWork.getEvent().getId())
                .employeeId(eventWork.getEmployee().getId())
                .id(eventWork.getId())
                .description(eventWork.getDescription())
                .hoursWorked(eventWork.getHoursWorked())
                .workDate(eventWork.getWorkDate())
                .build();
    }
}
