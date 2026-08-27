package com.eventsApp.auth;

import com.eventsApp.auth.model.command.ChangePasswordCommand;
import com.eventsApp.auth.model.command.LoginCommand;
import com.eventsApp.auth.model.command.TenantEmailSettingsUpdateCommand;
import com.eventsApp.auth.model.dto.LoginResponseDTO;
import com.eventsApp.auth.model.dto.TenantDTO;
import com.eventsApp.auth.model.dto.TenantEmailSettingsDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("event-api/auth")
public class AuthController {

    private final AuthService authService;
    private final TenantService tenantService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> login(@Valid @RequestBody LoginCommand command) {
        return ResponseEntity.ok(authService.login(command));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordCommand command) {
        authService.changePassword(command);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PatchMapping("/public-form-token/regenerate")
    public ResponseEntity<TenantDTO> regeneratePublicFormToken() {
        return ResponseEntity.ok(tenantService.regenerateOwnPublicFormToken());
    }

    @GetMapping("/email-settings")
    public ResponseEntity<TenantEmailSettingsDTO> getEmailSettings() {
        return ResponseEntity.ok(authService.getEmailSettings());
    }

    @PatchMapping("/email-settings")
    public ResponseEntity<TenantEmailSettingsDTO> updateEmailSettings(@RequestBody TenantEmailSettingsUpdateCommand command) {
        return ResponseEntity.ok(authService.updateEmailSettings(command));
    }
}
