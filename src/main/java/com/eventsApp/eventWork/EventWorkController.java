package com.eventsApp.eventWork;

import com.eventsApp.eventWork.model.command.EventWorkCreateCommand;
import com.eventsApp.eventWork.model.dto.EventWorkDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("event-api/event-work")
public class EventWorkController {
    private final EventWorkService eventWorkService;

    @PostMapping
    public ResponseEntity<EventWorkDTO> create(@RequestBody EventWorkCreateCommand command) {
        return new ResponseEntity<>(eventWorkService.create(command), HttpStatus.OK);
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<EventWorkDTO>> getAllByEventId(@PathVariable int eventId) {
        return ResponseEntity.ok(eventWorkService.getAllByEventId(eventId));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EventWorkDTO>> getAllByEmployeeId(@PathVariable int employeeId) {
        return ResponseEntity.ok(eventWorkService.getAllByEmployeeId(employeeId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        eventWorkService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
