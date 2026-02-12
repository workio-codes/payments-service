package com.payments.controller;

import com.payments.dto.PaymentRequest;
import com.payments.dto.PaymentResponse;
import com.payments.dto.RazorpayOrderCreateRequest;
import com.payments.dto.RazorpayOrderCreateResponse;
import com.payments.dto.RazorpayVerifyRequest;
import com.payments.dto.RazorpayVerifyResponse;
import com.payments.model.Payment;
import com.payments.service.PaymentService;
import com.razorpay.RazorpayException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> makePayment(@RequestBody PaymentRequest request) {
        Payment payment = new Payment(request.getOrderId(), request.getAmount(), request.getPaymentMethod());
        Payment result = paymentService.processPayment(payment);
        PaymentResponse response = new PaymentResponse(
                result.getPaymentId(),
                result.getStatus(),
                result.getTimestamp() == null ? null : result.getTimestamp().toString()
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/razorpay/order")
    public ResponseEntity<?> createRazorpayOrder(@RequestBody RazorpayOrderCreateRequest request) {
        try {
            RazorpayOrderCreateResponse response = paymentService.createRazorpayOrder(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new RazorpayVerifyResponse(false, "INVALID_REQUEST", e.getMessage()));
        } catch (RazorpayException e) {
            return ResponseEntity.status(502).body(new RazorpayVerifyResponse(false, "RAZORPAY_ERROR", e.getMessage()));
        }
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<RazorpayVerifyResponse> verifyRazorpayPayment(@RequestBody RazorpayVerifyRequest request) {
        RazorpayVerifyResponse response = paymentService.verifyRazorpayPayment(request);
        if (response.isVerified()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(400).body(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long paymentId) {
        return paymentService.getPaymentById(paymentId)
                .map(p -> new PaymentResponse(
                        p.getPaymentId(),
                        p.getStatus(),
                        p.getTimestamp() == null ? null : p.getTimestamp().toString()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentByOrderId(@PathVariable String orderId) {
        return paymentService.getPaymentByOrderId(orderId)
                .map(p -> new PaymentResponse(
                        p.getPaymentId(),
                        p.getStatus(),
                        p.getTimestamp() == null ? null : p.getTimestamp().toString()
                ))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/refund/{orderId}")
    public ResponseEntity<PaymentResponse> refundPayment(@PathVariable String orderId) {
        Payment result = paymentService.refundPayment(orderId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        PaymentResponse response = new PaymentResponse(
                result.getPaymentId(),
                result.getStatus(),
                result.getTimestamp() == null ? null : result.getTimestamp().toString()
        );
        return ResponseEntity.ok(response);
    }
}
