package com.eventsApp.eventElement.model.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EventElementDTO {
    private Integer id;
    private Integer offerId;
    private String name;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal sum;
}
