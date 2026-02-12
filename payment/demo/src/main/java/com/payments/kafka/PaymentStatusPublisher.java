package com.payments.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payments.events.PaymentStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class PaymentStatusPublisher {

    private static final Logger log = LoggerFactory.getLogger(PaymentStatusPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final String topicName;

    public PaymentStatusPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper,
            @Value("${app.kafka.payment-status-topic}") String topicName
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicName = topicName;
    }

    public void publish(PaymentStatusEvent event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topicName, event.getOrderId(), payload);
            log.info("Published payment status event: orderId={}, status={}, topic={}",
                    event.getOrderId(), event.getStatus(), topicName);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize payment status event for orderId={}", event.getOrderId(), e);
        }
    }
}
