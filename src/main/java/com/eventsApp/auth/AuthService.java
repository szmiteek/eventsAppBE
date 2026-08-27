package com.eventsApp.auth;

import com.eventsApp.auth.model.Tenant;
import com.eventsApp.auth.model.command.ChangePasswordCommand;
import com.eventsApp.auth.model.command.LoginCommand;
import com.eventsApp.auth.model.command.TenantEmailSettingsUpdateCommand;
import com.eventsApp.auth.model.dto.LoginResponseDTO;
import com.eventsApp.auth.model.dto.TenantEmailSettingsDTO;
import com.eventsApp.exceptions.EventApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentTenantProvider currentTenantProvider;

    public LoginResponseDTO login(LoginCommand command) {
        Tenant tenant = tenantRepository.findByEmail(command.getEmail())
                .orElseThrow(() -> new EventApiException("Nieprawidłowy e-mail lub hasło", HttpStatus.UNAUTHORIZED));

        if (!tenant.isActive()) {
            throw new EventApiException("Konto zostało dezaktywowane", HttpStatus.FORBIDDEN);
        }
        if (!passwordEncoder.matches(command.getPassword(), tenant.getPasswordHash())) {
            throw new EventApiException("Nieprawidłowy e-mail lub hasło", HttpStatus.UNAUTHORIZED);
        }

        return LoginResponseDTO.builder()
                .token(jwtService.generateToken(tenant))
                .tenantId(tenant.getId())
                .companyName(tenant.getCompanyName())
                .role(tenant.getRole())
                .publicFormToken(tenant.getPublicFormToken())
                .build();
    }

    public void changePassword(ChangePasswordCommand command) {
        int currentId = currentTenantProvider.getCurrent().tenantId();
        Tenant tenant = tenantRepository.findById(currentId)
                .orElseThrow(() -> new EventApiException("Tenant not found", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(command.getCurrentPassword(), tenant.getPasswordHash())) {
            throw new EventApiException("Nieprawidłowe obecne hasło", HttpStatus.BAD_REQUEST);
        }

        tenant.setPasswordHash(passwordEncoder.encode(command.getNewPassword()));
        tenantRepository.save(tenant);
    }

    public TenantEmailSettingsDTO getEmailSettings() {
        return TenantEmailSettingsDTO.builder()
                .emailMessage(currentTenant().getEmailMessage())
                .build();
    }

    public TenantEmailSettingsDTO updateEmailSettings(TenantEmailSettingsUpdateCommand command) {
        Tenant tenant = currentTenant();
        tenant.setEmailMessage(command.getEmailMessage());
        tenantRepository.save(tenant);
        return getEmailSettings();
    }

    private Tenant currentTenant() {
        int currentId = currentTenantProvider.getCurrent().tenantId();
        return tenantRepository.findById(currentId)
                .orElseThrow(() -> new EventApiException("Tenant not found", HttpStatus.NOT_FOUND));
    }
}
