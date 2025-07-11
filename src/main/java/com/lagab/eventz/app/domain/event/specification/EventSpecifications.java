package com.lagab.eventz.app.domain.event.specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.lagab.eventz.app.domain.event.dto.EventSearchDTO;
import com.lagab.eventz.app.domain.event.model.Address;
import com.lagab.eventz.app.domain.event.model.Event;
import com.lagab.eventz.app.domain.event.model.EventStatus;
import com.lagab.eventz.app.domain.event.model.EventType;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;

public class EventSpecifications {

    // Returns a specification to filter events containing the keyword in name or description
    public static Specification<Event> hasKeyword(String keyword) {
        return (root, query, criteriaBuilder) -> {
            if (keyword == null || keyword.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // No filter if keyword is empty
            }
            String likePattern = "%" + keyword.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern)
            );
        };
    }

    // Returns a specification to filter events by type
    public static Specification<Event> hasType(EventType type) {
        return (root, query, criteriaBuilder) -> {
            if (type == null) {
                return criteriaBuilder.conjunction(); // No filter if type is null
            }
            return criteriaBuilder.equal(root.get("type"), type);
        };
    }

    // Returns a specification to filter events by status
    public static Specification<Event> hasStatus(EventStatus status) {
        return (root, query, criteriaBuilder) -> {
            if (status == null) {
                return criteriaBuilder.conjunction(); // No filter if status is null
            }
            return criteriaBuilder.equal(root.get("status"), status);
        };
    }

    // Returns a specification to filter events by city (case insensitive)
    public static Specification<Event> hasCity(String city) {
        return (root, query, criteriaBuilder) -> {
            if (city == null || city.trim().isEmpty()) {
                return criteriaBuilder.conjunction(); // No filter if city is empty
            }
            Join<Event, Address> addressJoin = root.join("address", JoinType.LEFT);

            return criteriaBuilder.equal(
                    criteriaBuilder.lower(addressJoin.get("city")),
                    city.toLowerCase().trim()
            );
        };
    }

    // Returns a specification to filter events starting after the given date
    public static Specification<Event> startsAfter(LocalDateTime startDate) {
        return (root, query, criteriaBuilder) -> {
            if (startDate == null) {
                return criteriaBuilder.conjunction(); // No filter if date is null
            }
            return criteriaBuilder.greaterThanOrEqualTo(root.get("startDate"), startDate);
        };
    }

    // Returns a specification to filter events ending before the given date
    public static Specification<Event> endsBefore(LocalDateTime endDate) {
        return (root, query, criteriaBuilder) -> {
            if (endDate == null) {
                return criteriaBuilder.conjunction(); // No filter if date is null
            }
            return criteriaBuilder.lessThanOrEqualTo(root.get("endDate"), endDate);
        };
    }

    // Returns a specification to filter free/paid events
    public static Specification<Event> isFree(Boolean isFree) {
        return (root, query, criteriaBuilder) -> {
            if (isFree == null) {
                return criteriaBuilder.conjunction(); // No filter if null
            }
            return criteriaBuilder.equal(root.get("isFree"), isFree);
        };
    }

    // Returns a specification to filter only public events
    public static Specification<Event> isPublic() {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.isTrue(root.get("isPublic"));
    }

    // Returns a specification to filter events within a geographic radius
    public static Specification<Event> isWithinRadius(Double latitude, Double longitude, Double radius) {
        return (root, query, criteriaBuilder) -> {
            if (latitude == null || longitude == null || radius == null) {
                return criteriaBuilder.conjunction(); // No filter if any parameter is null
            }

            // Simplified Haversine distance formula
            // Note: For maximum precision, use a specialized database function
            return criteriaBuilder.lessThanOrEqualTo(
                    criteriaBuilder.function(
                            "SQRT",
                            Double.class,
                            criteriaBuilder.sum(
                                    criteriaBuilder.function(
                                            "POWER",
                                            Double.class,
                                            criteriaBuilder.diff(root.get("address").get("latitude"), latitude),
                                            criteriaBuilder.literal(2)
                                    ),
                                    criteriaBuilder.function(
                                            "POWER",
                                            Double.class,
                                            criteriaBuilder.diff(root.get("address").get("longitude"), longitude),
                                            criteriaBuilder.literal(2)
                                    )
                            )
                    ),
                    radius / 111.0 // Approximate conversion from km to degrees
            );
        };
    }

    // Returns a specification to filter events with available tickets
    public static Specification<Event> hasAvailableTickets() {
        return (root, query, criteriaBuilder) -> {
            var ticketTypesJoin = root.join("ticketTypes");
            return criteriaBuilder.greaterThan(
                    ticketTypesJoin.get("quantityAvailable"),
                    ticketTypesJoin.get("quantitySold")
            );
        };
    }

    // Returns a specification to filter events by organizer
    public static Specification<Event> hasOrganizer(Long organizerId) {
        return (root, query, criteriaBuilder) -> {
            if (organizerId == null) {
                return criteriaBuilder.conjunction(); // No filter if organizerId is null
            }
            return criteriaBuilder.equal(root.get("organizer").get("id"), organizerId);
        };
    }

    // Main method to combine all search criteria
    public static Specification<Event> withCriteria(EventSearchDTO searchDTO) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Keyword search
            if (searchDTO.keyword() != null && !searchDTO.keyword().trim().isEmpty()) {
                String likePattern = "%" + searchDTO.keyword().toLowerCase() + "%";
                Predicate keywordPredicate = criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern)
                );
                predicates.add(keywordPredicate);
            }

            // Type filter
            if (searchDTO.type() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), searchDTO.type()));
            }

            // Status filter
            if (searchDTO.status() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), searchDTO.status()));
            }

            // City filter
            if (searchDTO.city() != null && !searchDTO.city().trim().isEmpty()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("address").get("city")),
                        searchDTO.city().toLowerCase()
                ));
            }

            // Date range filters
            if (searchDTO.startDate() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("startDate"), searchDTO.startDate()
                ));
            }

            if (searchDTO.endDate() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("endDate"), searchDTO.endDate()
                ));
            }

            // Free events filter
            if (searchDTO.isFree() != null) {
                predicates.add(criteriaBuilder.equal(root.get("isFree"), searchDTO.isFree()));
            }

            // Organizer filter
            if (searchDTO.organizerId() != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("organizer").get("id"), searchDTO.organizerId()
                ));
            }

            // Geographic filter
            if (searchDTO.latitude() != null && searchDTO.longitude() != null && searchDTO.radius() != null) {
                // Simplified implementation - for real applications, use PostGIS or similar
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        criteriaBuilder.function(
                                "SQRT",
                                Double.class,
                                criteriaBuilder.sum(
                                        criteriaBuilder.function(
                                                "POWER",
                                                Double.class,
                                                criteriaBuilder.diff(root.get("address").get("latitude"), searchDTO.latitude()),
                                                criteriaBuilder.literal(2)
                                        ),
                                        criteriaBuilder.function(
                                                "POWER",
                                                Double.class,
                                                criteriaBuilder.diff(root.get("address").get("longitude"), searchDTO.longitude()),
                                                criteriaBuilder.literal(2)
                                        )
                                )
                        ),
                        searchDTO.radius() / 111.0
                ));
            }

            // Always filter for public events
            predicates.add(criteriaBuilder.isTrue(root.get("isPublic")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Event> publicEvents() {
        return isPublic();
    }

    public static Specification<Event> publishedEvents() {
        return hasStatus(EventStatus.PUBLISHED);
    }

    public static Specification<Event> upcomingEvents() {
        return startsAfter(LocalDateTime.now());
    }

    public static Specification<Event> freeEvents() {
        return isFree(true);
    }
}
