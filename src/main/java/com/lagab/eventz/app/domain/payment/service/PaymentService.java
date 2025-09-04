package com.lagab.eventz.app.domain.payment.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.lagab.eventz.app.domain.order.model.Order;
import com.lagab.eventz.app.domain.payment.exception.RefundException;
import com.lagab.eventz.app.domain.payment.model.Payment;
import com.lagab.eventz.app.domain.payment.model.PaymentMethod;
import com.lagab.eventz.app.domain.payment.model.PaymentStatus;
import com.lagab.eventz.app.domain.payment.repository.PaymentRepository;
import com.stripe.model.checkout.Session;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    public Payment buildPaymentSuccess(Order order, Session stripeSession) {
        // Create payment record
        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setAmount(order.getTotalAmount());
        payment.setMethod(PaymentMethod.STRIPE);
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setCreatedAt(LocalDateTime.now());
        payment.setPaymentIntentId(stripeSession.getPaymentIntent());
        payment.setTransactionId(stripeSession.getId());
        payment.setProcessedAt(LocalDateTime.now());

        payment = paymentRepository.save(payment);

        return paymentRepository.save(payment);
    }

    // Todo: to implement
    public Payment refundPayment(Order order) {
        Payment originalPayment = paymentRepository.findByOrderIdAndStatus(order.getId(), PaymentStatus.COMPLETED)
                                                   .orElseThrow(() -> new RuntimeException("Original payment not found"));

        try {
            /*PaymentResult refundResult = stripeProvider.refundPayment(
                    originalPayment.getPaymentIntentId(),
                    originalPayment.getAmount()
            );*/

            originalPayment.setRefundedAmount(originalPayment.getAmount());
            originalPayment.setStatus(PaymentStatus.REFUNDED);

            return paymentRepository.save(originalPayment);

        } catch (Exception e) {
            throw new RefundException("Error while processing refund: " + e.getMessage());
        }
    }
}
