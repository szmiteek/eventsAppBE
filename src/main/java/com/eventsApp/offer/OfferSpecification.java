package com.eventsApp.offer;

import com.eventsApp.offer.model.Offer;
import com.eventsApp.offer.model.dto.OfferFilter;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class OfferSpecification {

    public static Specification<Offer> filterByCreatedDate(LocalDate from, LocalDate to) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Offer> filterByEventDate(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("eventDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("eventDate"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Offer> filterByPersonalData(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("personalData")), "%" + value.toLowerCase() + "%");
        };
    }

    public static Specification<Offer> filterByVenue(String value) {
        return (root, query, cb) -> {
            if (value == null || value.isBlank()) {
                return cb.conjunction();
            }
            return cb.like(cb.lower(root.get("venue")), "%" + value.toLowerCase() + "%");
        };
    }


    public static Specification<Offer> filterByTenant(Integer tenantId) {
        return (root, query, cb) -> cb.equal(root.get("tenantId"), tenantId);
    }

    public static Specification<Offer> build(OfferFilter filters, Integer tenantId) {
        Specification<Offer> spec = filterByTenant(tenantId);

        // Either bound on its own is a valid filter — "to" alone must narrow the results too.
        if (filters.getCreatedDateFrom() != null || filters.getCreatedDateTo() != null) {
            spec = spec.and(filterByCreatedDate(filters.getCreatedDateFrom(), filters.getCreatedDateTo()));
        }
        if (filters.getEventDateFrom() != null || filters.getEventDateTo() != null) {
            spec = spec.and(filterByEventDate(filters.getEventDateFrom(), filters.getEventDateTo()));
        }
        if (filters.getClient() != null) {
            spec = spec.and(filterByPersonalData(filters.getClient()));
        }
        if (filters.getVenue() != null) {
            spec = spec.and(filterByVenue(filters.getVenue()));
        }
        return spec;
    }
}
