package com.eventsApp.offerSettings.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * The tenant's own PDF that the generated offer pages are appended to. Kept in its own table so that
 * reading the offer settings never loads the whole file.
 */
@Entity
@Table(name = "tenant_offer_cover_pdf")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TenantOfferCoverPdf {

    @Id
    @Column(name = "tenant_id", nullable = false)
    private Integer tenantId;

    @Lob
    @Column(name = "data", nullable = false, columnDefinition = "LONGBLOB")
    private byte[] data;
}
