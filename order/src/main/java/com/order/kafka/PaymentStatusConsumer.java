package com.order.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.order.events.PaymentStatusEvent;
import com.order.model.Order;
import com.order.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentStatusConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusConsumer.class);

    private final ObjectMapper objectMapper;
    private final OrderRepository orderRepository;

    public PaymentStatusConsumer(ObjectMapper objectMapper, OrderRepository orderRepository) {
        this.objectMapper = objectMapper;
        this.orderRepository = orderRepository;
    }

    @KafkaListener(topics = "${app.kafka.payment-status-topic}")
    public void consume(String payload) {
        try {
            PaymentStatusEvent event = objectMapper.readValue(payload, PaymentStatusEvent.class);
            if (event.getOrderId() == null || event.getOrderId().isBlank()) {
                log.warn("Skipping payment status event without orderId: {}", payload);
                return;
            }

            Order order = orderRepository.findById(event.getOrderId()).orElse(null);
            if (order == null) {
                log.warn("Order not found for payment event: orderId={}, status={}", event.getOrderId(), event.getStatus());
                return;
            }

            String normalizedStatus = event.getStatus() == null ? "" : event.getStatus().trim().toUpperCase();
            String newStatus;
            switch (normalizedStatus) {
                case "SUCCESS":
                case "VERIFIED":
                    newStatus = "Confirmed";
                    break;
                case "FAILED":
                case "FAILURE":
                case "CANCELLED":
                    newStatus = "Payment Failed";
                    break;
                default:
                    log.info("Ignoring unsupported payment event status for orderId={}: {}", event.getOrderId(), event.getStatus());
                    return;
            }

            order.setStatus(newStatus);
            orderRepository.save(order);
            log.info("Order status updated via Kafka: orderId={}, status={}, reason={}",
                    order.getOrderId(), newStatus, event.getReason());
        } catch (Exception e) {
            log.error("Failed to process payment status event payload={}", payload, e);
        }
    }
}
