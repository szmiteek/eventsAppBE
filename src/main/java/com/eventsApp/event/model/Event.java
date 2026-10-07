package com.eventsApp.event.model;

import com.eventsApp.eventElement.model.EventElement;
import com.eventsApp.eventWork.model.EventWork;
import com.eventsApp.offer.model.Offer;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private Integer tenantId;
    private String clientPersonalData;
    private String venue;
    private LocalDate date;
    private String email;
    private String phone;
    private Integer budget;
    private Integer guests;
    private BigDecimal price;
    /** Null means the deposit has not been paid — the amount is the whole state, so the two can't disagree. */
    private BigDecimal depositAmount;
    private String comment;
    private Integer offerId;

    private boolean afterWeddingParty;

    @Column(columnDefinition = "TEXT")
    private String decorationDescription;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private Set<EventWork> eventWorks;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC, id ASC")
    @Builder.Default
    private List<EventElement> eventElements = new ArrayList<>();

}
