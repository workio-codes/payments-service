package com.payments.service;

import com.payments.config.RazorpayProperties;
import com.payments.dto.RazorpayOrderCreateRequest;
import com.payments.dto.RazorpayOrderCreateResponse;
import com.payments.dto.RazorpayVerifyRequest;
import com.payments.dto.RazorpayVerifyResponse;
import com.payments.events.PaymentStatusEvent;
import com.payments.kafka.PaymentStatusPublisher;
import com.payments.model.Payment;
import com.payments.repository.PaymentRepository;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final RazorpayProperties razorpayProperties;
    private final PaymentStatusPublisher paymentStatusPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          RazorpayProperties razorpayProperties,
                          PaymentStatusPublisher paymentStatusPublisher) {
        this.paymentRepository = paymentRepository;
        this.razorpayProperties = razorpayProperties;
        this.paymentStatusPublisher = paymentStatusPublisher;
    }

    @Transactional
    public Payment processPayment(Payment payment) {
        Optional<Payment> existing = paymentRepository.findByOrderId(payment.getOrderId());
        if (existing.isPresent()) {
            return existing.get();
        }

        payment.setStatus("Pending");
        payment.setTimestamp(LocalDateTime.now());
        Payment savedPayment = paymentRepository.save(payment);

        boolean success = simulatePaymentGateway();
        savedPayment.setStatus(success ? "Success" : "Failed");

        return paymentRepository.save(savedPayment);
    }

    @Transactional
    public RazorpayOrderCreateResponse createRazorpayOrder(RazorpayOrderCreateRequest request) throws RazorpayException {
        if (razorpayProperties.getKeyId() == null || razorpayProperties.getKeyId().isBlank()
                || razorpayProperties.getKeySecret() == null || razorpayProperties.getKeySecret().isBlank()) {
            throw new IllegalStateException("Razorpay keys are not configured");
        }
        if (request.getOrderId() == null || request.getOrderId().isBlank()) {
            throw new IllegalArgumentException("orderId is required");
        }
        if (request.getAmount() == null || request.getAmount() <= 0) {
            throw new IllegalArgumentException("amount must be greater than zero");
        }

        long amountInPaise = Math.round(request.getAmount() * 100);

        Optional<Payment> existing = paymentRepository.findByOrderId(request.getOrderId());
        if (existing.isPresent() && existing.get().getRazorpayOrderId() != null) {
            Payment payment = existing.get();
            return new RazorpayOrderCreateResponse(
                    payment.getOrderId(),
                    payment.getRazorpayOrderId(),
                    razorpayProperties.getKeyId(),
                    amountInPaise,
                    payment.getCurrency() == null ? razorpayProperties.getCurrency() : payment.getCurrency(),
                    payment.getStatus()
            );
        }

        RazorpayClient razorpayClient = new RazorpayClient(razorpayProperties.getKeyId(), razorpayProperties.getKeySecret());

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", razorpayProperties.getCurrency());
        orderRequest.put("receipt", request.getOrderId());

        Order razorpayOrder = razorpayClient.orders.create(orderRequest);
        String razorpayOrderId = razorpayOrder.get("id");

        Payment payment = existing.orElse(new Payment());
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setCurrency(razorpayProperties.getCurrency());
        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setStatus("CREATED");
        payment.setTimestamp(LocalDateTime.now());
        paymentRepository.save(payment);

        return new RazorpayOrderCreateResponse(
                request.getOrderId(),
                razorpayOrderId,
                razorpayProperties.getKeyId(),
                amountInPaise,
                razorpayProperties.getCurrency(),
                "CREATED"
        );
    }

    @Transactional
    public RazorpayVerifyResponse verifyRazorpayPayment(RazorpayVerifyRequest request) {
        if (request.getOrderId() == null || request.getOrderId().isBlank()
                || request.getRazorpayOrderId() == null || request.getRazorpayOrderId().isBlank()
                || request.getRazorpayPaymentId() == null || request.getRazorpayPaymentId().isBlank()
                || request.getRazorpaySignature() == null || request.getRazorpaySignature().isBlank()) {
            return new RazorpayVerifyResponse(false, "INVALID_REQUEST", "Missing Razorpay verification fields");
        }

        Optional<Payment> paymentOpt = paymentRepository.findByOrderId(request.getOrderId());
        if (paymentOpt.isEmpty()) {
            return new RazorpayVerifyResponse(false, "NOT_FOUND", "Order payment record not found");
        }

        Payment payment = paymentOpt.get();

        if (request.getRazorpayOrderId() == null || !request.getRazorpayOrderId().equals(payment.getRazorpayOrderId())) {
            payment.setStatus("Failed");
            payment.setTimestamp(LocalDateTime.now());
            paymentRepository.save(payment);
            paymentStatusPublisher.publish(toEvent(payment, "FAILED", "Razorpay order id mismatch"));
            return new RazorpayVerifyResponse(false, "FAILED", "Razorpay order id mismatch");
        }

        String payload = request.getRazorpayOrderId() + "|" + request.getRazorpayPaymentId();
        String expectedSignature = hmacSha256(payload, razorpayProperties.getKeySecret());

        boolean verified = MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                request.getRazorpaySignature().getBytes(StandardCharsets.UTF_8)
        );

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setRazorpaySignature(request.getRazorpaySignature());
        payment.setTimestamp(LocalDateTime.now());

        if (verified) {
            payment.setStatus("Success");
            paymentRepository.save(payment);
            paymentStatusPublisher.publish(toEvent(payment, "SUCCESS", "Payment signature verified"));
            return new RazorpayVerifyResponse(true, "VERIFIED", "Payment signature verified");
        }

        payment.setStatus("Failed");
        paymentRepository.save(payment);
        paymentStatusPublisher.publish(toEvent(payment, "FAILED", "Invalid payment signature"));
        return new RazorpayVerifyResponse(false, "FAILED", "Invalid payment signature");
    }

    private PaymentStatusEvent toEvent(Payment payment, String status, String reason) {
        return new PaymentStatusEvent(
                payment.getOrderId(),
                payment.getRazorpayPaymentId(),
                payment.getRazorpayOrderId(),
                status,
                reason,
                System.currentTimeMillis()
        );
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to compute payment signature", e);
        }
    }

    public Optional<Payment> getPaymentByOrderId(String orderId) {
        return paymentRepository.findByOrderId(orderId);
    }

    @Transactional
    public Payment refundPayment(String orderId) {
        Optional<Payment> existingOpt = paymentRepository.findByOrderId(orderId);
        if (existingOpt.isEmpty()) {
            return null;
        }

        Payment payment = existingOpt.get();

        if ("REFUNDED".equalsIgnoreCase(payment.getStatus())) {
            return payment;
        }

        if (!"Success".equalsIgnoreCase(payment.getStatus())) {
            return payment;
        }

        payment.setStatus("REFUND_PENDING");
        payment.setTimestamp(LocalDateTime.now());
        paymentRepository.save(payment);

        boolean success = simulatePaymentGateway();
        payment.setStatus(success ? "REFUNDED" : "REFUND_FAILED");

        return paymentRepository.save(payment);
    }

    private boolean simulatePaymentGateway() {
        return true;
    }

    public Optional<Payment> getPaymentById(Long paymentId) {
        return paymentRepository.findById(paymentId);
    }
}
