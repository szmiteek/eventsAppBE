package com.eventsApp.eventElement;

import com.eventsApp.eventElement.model.EventElement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EventElementRepository extends JpaRepository<EventElement, Integer> {

    List<EventElement> findAllByOfferId(int offerId);
}
