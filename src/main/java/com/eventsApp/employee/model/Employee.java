package com.eventsApp.employee.model;

import com.eventsApp.eventWork.model.EventWork;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.Set;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private Integer tenantId;
    private String firstName;
    private String lastName;
    private double hourlyRate;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL)
    private Set<EventWork> events;

}
