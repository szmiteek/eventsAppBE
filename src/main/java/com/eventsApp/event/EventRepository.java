package com.eventsApp.event;

import com.eventsApp.event.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EventRepository extends JpaRepository<Event, Integer>, JpaSpecificationExecutor<Event> {
}
