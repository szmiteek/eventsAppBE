package com.eventsApp.eventWork;

import com.eventsApp.employee.model.Employee;
import com.eventsApp.event.Event;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"event_id", "employee_id"})})
public class EventWork {
    @Id
    @GeneratedValue
    private int id;

    @ManyToOne
    private Event event;

    @ManyToOne
    private Employee employee;

    private double hoursWorked;

}
