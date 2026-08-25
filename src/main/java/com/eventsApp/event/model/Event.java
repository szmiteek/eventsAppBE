package com.eventsApp.event.model;

import com.eventsApp.eventWork.model.EventWork;
import com.eventsApp.offer.model.Offer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private String comment;
    private Integer offerId;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL)
    private Set<EventWork> eventWorks;

}
