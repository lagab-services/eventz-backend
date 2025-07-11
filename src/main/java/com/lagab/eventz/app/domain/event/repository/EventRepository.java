package com.lagab.eventz.app.domain.event.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import com.lagab.eventz.app.domain.event.model.Event;

@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    // Count events by organizer
    long countByOrganizerId(Long organizerId);

    // Find events by organizer with pagination
    Page<Event> findByOrganizerId(Long organizerId, Pageable pageable);
}
