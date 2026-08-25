package com.eventsApp.eventWork;

import com.eventsApp.eventWork.model.EventWork;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventWorkRepository extends JpaRepository<EventWork, Integer> {

    List<EventWork> findAllByEmployeeId(Integer employeeId);

    List<EventWork> findAllByEventId(Integer eventId);
}
