package com.eventsApp.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String companyName;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantRole role;

    @Column(nullable = false)
    private boolean active;

    private LocalDate createdDate;

    @Column(unique = true)
    private String publicFormToken;

    @Column(columnDefinition = "TEXT")
    private String emailMessage;

    /** Adres alertów o nowym zapytaniu; null znaczy „ten, na który założono konto”. */
    private String alertEmail;

    /** Builder.Default, bo nowy tenant ma dostawać alerty od razu — tak jak mówi wartość domyślna kolumny. */
    @Builder.Default
    private boolean alertEmailEnabled = true;

    /** Adres, na który faktycznie idą alerty. */
    public String resolveAlertEmail() {
        return alertEmail != null && !alertEmail.isBlank() ? alertEmail : email;
    }
}
