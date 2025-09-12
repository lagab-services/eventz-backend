package com.lagab.eventz.app.domain.payment.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.lagab.eventz.app.domain.payment.model.Payment;
import com.lagab.eventz.app.domain.payment.model.PaymentStatus;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByOrderIdAndStatus(Long id, PaymentStatus paymentStatus);
}
