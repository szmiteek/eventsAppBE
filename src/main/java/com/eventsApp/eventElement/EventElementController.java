package com.eventsApp.eventElement;

import com.eventsApp.eventElement.model.command.EventElementCreateCommand;
import com.eventsApp.eventElement.model.command.EventElementUpdateCommand;
import com.eventsApp.eventElement.model.dto.EventElementDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("event-api/event-element")
public class EventElementController {

    private final EventElementService eventElementService;

    @PostMapping
    public ResponseEntity<EventElementDTO> create(@Valid @RequestBody EventElementCreateCommand command) {
        return new ResponseEntity<>(eventElementService.create(command), HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<EventElementDTO> update(@PathVariable int id, @Valid @RequestBody EventElementUpdateCommand command) {
        return ResponseEntity.ok(eventElementService.update(id, command));
    }

    @GetMapping("/offer/{offerId}")
    public ResponseEntity<List<EventElementDTO>> getAllByOfferId(@PathVariable int offerId) {
        return ResponseEntity.ok(eventElementService.getAllByOfferId(offerId));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<EventElementDTO>> getAllByEventId(@PathVariable int eventId) {
        return ResponseEntity.ok(eventElementService.getAllByEventId(eventId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        eventElementService.delete(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
