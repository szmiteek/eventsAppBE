package com.eventsApp.eventElement;

import com.eventsApp.eventElement.model.EventElement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EventElementRepository extends JpaRepository<EventElement, Integer> {

    /** Sorted by position (id breaks ties), so the app and the generated PDF always show the same row order. */
    List<EventElement> findAllByOfferIdOrderByPositionAscIdAsc(int offerId);

    List<EventElement> findAllByEventIdOrderByPositionAscIdAsc(int eventId);

    @Query("select coalesce(max(e.position), -1) from EventElement e where e.offer.id = :offerId")
    int findMaxPositionByOfferId(@Param("offerId") int offerId);

    @Query("select coalesce(max(e.position), -1) from EventElement e where e.event.id = :eventId")
    int findMaxPositionByEventId(@Param("eventId") int eventId);
}
