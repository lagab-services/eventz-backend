package com.lagab.eventz.app.domain.promotion.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.lagab.eventz.app.domain.promotion.model.Discount;

public interface DiscountRepository extends JpaRepository<Discount, String> {
    Optional<Discount> findByCodeIgnoreCase(String code);

    Page<Discount> findByEventId(Long eventId, Pageable pageable);

    Optional<Discount> findByIdAndEventId(String id, Long eventId);

    boolean existsByIdAndEventId(String id, Long eventId);
}
