package com.eventsApp.offer.model;

import com.eventsApp.eventElement.model.EventElement;
import com.eventsApp.offer.OfferStatus;
import com.eventsApp.offerImage.model.OfferImage;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;
    private Integer tenantId;
    private LocalDate createdDate;
    private String personalData;
    private String venue;
    private LocalDate eventDate;
    private String email;
    private String phone;
    private Integer budget;
    private Integer guests;

    private BigDecimal price;
    private String comment;

    @Enumerated(EnumType.STRING)
    private OfferStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> eventType;

    private String mainTableType;
    private String mainTableSeats;
    private String guestsTableType;
    private boolean appetizersOnTable;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<String> decorationType;

    private String flowersType;
    private String colors;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String decorationDescription;

    private LocalDateTime pdfGeneratedDate;

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OfferImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "offer", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EventElement> eventElements = new ArrayList<>();

}
