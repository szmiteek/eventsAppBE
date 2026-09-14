package com.eventsApp.offerSettings.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tenant_offer_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantOfferSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private Integer tenantId;

    /** Page background as #RRGGBB. */
    @Column(name = "background_color", nullable = false, length = 7)
    private String backgroundColor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PdfOrientation orientation;

    /** Fields printed on the "Informacje ogólne" page, in the order the tenant arranged them. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "info_fields", columnDefinition = "json", nullable = false)
    @Builder.Default
    private List<OfferInfoField> infoFields = new ArrayList<>();

    @Column(name = "logo_filename")
    private String logoFilename;

    @Column(name = "logo_content_type", length = 100)
    private String logoContentType;

    @Lob
    @Column(name = "logo_data", columnDefinition = "LONGBLOB")
    private byte[] logoData;
}
