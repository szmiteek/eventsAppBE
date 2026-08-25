package com.eventsApp.auth;

import com.eventsApp.auth.model.Tenant;
import com.eventsApp.auth.model.TenantRole;
import com.eventsApp.auth.model.command.TenantCreateCommand;
import com.eventsApp.auth.model.dto.TenantDTO;
import com.eventsApp.exceptions.EventApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentTenantProvider currentTenantProvider;

    public TenantDTO create(TenantCreateCommand command) {
        tenantRepository.findByEmail(command.getEmail()).ifPresent(existing -> {
            throw new EventApiException("Email already in use", HttpStatus.CONFLICT);
        });
        Tenant tenant = Tenant.builder()
                .companyName(command.getCompanyName())
                .email(command.getEmail())
                .passwordHash(passwordEncoder.encode(command.getPassword()))
                .role(TenantRole.TENANT)
                .active(true)
                .createdDate(LocalDate.now())
                .publicFormToken(UUID.randomUUID().toString())
                .build();
        return TenantMapper.mapToDTO(tenantRepository.save(tenant));
    }

    public TenantDTO regenerateOwnPublicFormToken() {
        int currentId = currentTenantProvider.getCurrent().tenantId();
        Tenant tenant = tenantRepository.findById(currentId)
                .orElseThrow(() -> new EventApiException("Tenant not found", HttpStatus.NOT_FOUND));
        tenant.setPublicFormToken(UUID.randomUUID().toString());
        return TenantMapper.mapToDTO(tenantRepository.save(tenant));
    }

    public List<TenantDTO> getAll() {
        return tenantRepository.findAll().stream()
                .filter(tenant -> tenant.getRole() == TenantRole.TENANT)
                .map(TenantMapper::mapToDTO)
                .toList();
    }

    public TenantDTO setActive(int id, boolean active) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new EventApiException("Tenant not found", HttpStatus.NOT_FOUND));
        tenant.setActive(active);
        return TenantMapper.mapToDTO(tenantRepository.save(tenant));
    }
}
