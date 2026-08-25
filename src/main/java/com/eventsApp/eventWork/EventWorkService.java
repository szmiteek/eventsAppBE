package com.eventsApp.eventWork;

import com.eventsApp.auth.CurrentTenantProvider;
import com.eventsApp.employee.EmployeeRepository;
import com.eventsApp.employee.model.Employee;
import com.eventsApp.event.EventRepository;
import com.eventsApp.event.model.Event;
import com.eventsApp.eventWork.model.EventWork;
import com.eventsApp.eventWork.model.command.EventWorkCreateCommand;
import com.eventsApp.eventWork.model.dto.EventWorkDTO;
import com.eventsApp.exceptions.EventApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.eventsApp.eventWork.EventWorkMapper.fromCreateCommand;
import static com.eventsApp.eventWork.EventWorkMapper.mapToDTO;

@Service
@RequiredArgsConstructor
public class EventWorkService {
    private final EventWorkRepository eventWorkRepository;
    private final EventRepository eventRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentTenantProvider currentTenantProvider;

    public EventWorkDTO create(EventWorkCreateCommand command) {
        Event event = getOwnedEvent(command.getEventId());
        Employee employee = getOwnedEmployee(command.getEmployeeId());
        EventWork eventWork = fromCreateCommand(command);
        eventWork.setEmployee(employee);
        eventWork.setEvent(event);
        return mapToDTO(eventWorkRepository.save(eventWork));
    }

    public void delete(int id) {
        EventWork eventWork = eventWorkRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Event work not found", HttpStatus.NOT_FOUND));
        getOwnedEvent(eventWork.getEvent().getId());
        eventWorkRepository.deleteById(id);
    }

    public List<EventWorkDTO> getAllByEventId(int eventId) {
        getOwnedEvent(eventId);
        return eventWorkRepository.findAllByEventId(eventId)
                .stream()
                .map(EventWorkMapper::mapToDTO)
                .toList();
    }

    public List<EventWorkDTO> getAllByEmployeeId(int employeeId) {
        getOwnedEmployee(employeeId);
        return eventWorkRepository.findAllByEmployeeId(employeeId)
                .stream()
                .map(EventWorkMapper::mapToDTO)
                .toList();
    }

    private Event getOwnedEvent(int eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventApiException("Event not found", HttpStatus.NOT_FOUND));
        if (!Integer.valueOf(event.getTenantId()).equals(currentTenantProvider.requireTenantId())) {
            throw new EventApiException("Event not found", HttpStatus.NOT_FOUND);
        }
        return event;
    }

    private Employee getOwnedEmployee(int employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new EventApiException("Employee not found", HttpStatus.NOT_FOUND));
        if (!Integer.valueOf(employee.getTenantId()).equals(currentTenantProvider.requireTenantId())) {
            throw new EventApiException("Employee not found", HttpStatus.NOT_FOUND);
        }
        return employee;
    }
}
