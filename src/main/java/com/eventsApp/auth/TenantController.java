package com.eventsApp.auth;

import com.eventsApp.auth.model.command.TenantCreateCommand;
import com.eventsApp.auth.model.command.TenantStatusCommand;
import com.eventsApp.auth.model.dto.TenantDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("event-api/tenants")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping
    public ResponseEntity<TenantDTO> create(@Valid @RequestBody TenantCreateCommand command) {
        return new ResponseEntity<>(tenantService.create(command), HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<TenantDTO>> getAll() {
        return ResponseEntity.ok(tenantService.getAll());
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TenantDTO> setActive(@PathVariable int id, @Valid @RequestBody TenantStatusCommand command) {
        return ResponseEntity.ok(tenantService.setActive(id, command.getActive()));
    }
}
