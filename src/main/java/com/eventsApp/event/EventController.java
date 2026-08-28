package com.eventsApp.event;

import com.eventsApp.event.model.command.EventCreateCommand;
import com.eventsApp.event.model.command.EventUpdateCommand;
import com.eventsApp.event.model.dto.EventDTO;
import com.eventsApp.event.model.dto.EventFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("event-api/event")
@RequiredArgsConstructor
public class EventController {
    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventDTO>> getAll(@PageableDefault Pageable pageable, EventFilter filters) {
        return new ResponseEntity<>(eventService.getAll(pageable, filters), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDTO> getById(@PathVariable int id) {
        return new ResponseEntity<>(eventService.getById(id), HttpStatus.OK);
    }

    @PostMapping
    public ResponseEntity<EventDTO> create(@RequestBody EventCreateCommand command) {
        return new ResponseEntity<>(eventService.create(command), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventDTO> update(@PathVariable int id, @RequestBody EventUpdateCommand command) {
        return ResponseEntity.ok(eventService.update(id, command));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        eventService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
