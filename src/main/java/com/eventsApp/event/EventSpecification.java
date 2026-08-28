package com.eventsApp.event;

import com.eventsApp.event.model.Event;
import com.eventsApp.event.model.dto.EventFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EventSpecification {

    public static Specification<Event> filterByDate(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("date"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("date"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Event> filterByClient(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("clientPersonalData")), "%" + value.toLowerCase() + "%");
        };
    }

    public static Specification<Event> filterByVenue(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("venue")), "%" + value.toLowerCase() + "%");
        };
    }

    public static Specification<Event> filterByTenant(Integer tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<Event> build(EventFilter filters, Integer tenantId) {
        Specification<Event> spec = filterByTenant(tenantId);

        // Either bound on its own is a valid filter — "to" alone must narrow the results too.
        if (filters.getDateFrom() != null || filters.getDateTo() != null) {
            spec = spec.and(filterByDate(filters.getDateFrom(), filters.getDateTo()));
        }
        if (filters.getClient() != null) {
            spec = spec.and(filterByClient(filters.getClient()));
        }
        if (filters.getVenue() != null) {
            spec = spec.and(filterByVenue(filters.getVenue()));
        }
        return spec;
    }
}
