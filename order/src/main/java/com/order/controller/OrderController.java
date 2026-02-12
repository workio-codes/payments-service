package com.order.controller;

import com.order.dto.OrderRequest;
import com.order.dto.OrderResponse;
import com.order.dto.PaymentResponse;
import com.order.model.Order;
import com.order.repository.OrderRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final RestTemplate restTemplate;
    private final OrderRepository orderRepository;

    public OrderController(RestTemplate restTemplate,
                           OrderRepository orderRepository) {
        this.restTemplate = restTemplate;
        this.orderRepository = orderRepository;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@RequestBody OrderRequest request) {
        String orderId = (request.getOrderId() == null || request.getOrderId().isBlank())
                ? "ORD" + System.currentTimeMillis()
                : request.getOrderId();

        Order order = new Order();
        order.setOrderId(orderId);
        order.setCustomerName(request.getCustomerName());
        order.setTotalAmount(request.getTotalAmount());
        order.setStatus("Pending");

        orderRepository.save(order);
        return ResponseEntity.ok(new OrderResponse(order.getOrderId(), order.getStatus()));
    }

    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable String orderId) {
        return updateStatus(orderId, "Confirmed");
    }

    @PostMapping("/{orderId}/payment-failed")
    public ResponseEntity<OrderResponse> markPaymentFailed(@PathVariable String orderId) {
        return updateStatus(orderId, "Payment Failed");
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable String orderId) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    if (!"Confirmed".equalsIgnoreCase(order.getStatus())) {
                        return ResponseEntity.status(409)
                                .body(new OrderResponse(order.getOrderId(), order.getStatus()));
                    }

                    order.setStatus("CANCELLATION_PENDING");
                    orderRepository.save(order);

                    try {
                        String refundUrl = "http://payments/api/payments/refund/" + order.getOrderId();
                        ResponseEntity<PaymentResponse> refundResponse =
                                restTemplate.postForEntity(refundUrl, null, PaymentResponse.class);

                        PaymentResponse refundBody = refundResponse.getBody();
                        if (refundResponse.getStatusCode().is2xxSuccessful() && refundBody != null) {
                            String refundStatus = refundBody.getStatus();
                            if ("REFUNDED".equalsIgnoreCase(refundStatus)) {
                                order.setStatus("CANCELLED");
                            } else {
                                order.setStatus("CANCELLATION_FAILED");
                            }
                        } else {
                            order.setStatus("CANCELLATION_FAILED");
                        }
                    } catch (Exception e) {
                        order.setStatus("CANCELLATION_FAILED");
                    }

                    orderRepository.save(order);
                    return ResponseEntity.ok(new OrderResponse(order.getOrderId(), order.getStatus()));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<java.util.List<Order>> getAllOrders() {
        java.util.List<Order> orders = orderRepository.findAll();
        return ResponseEntity.ok(orders);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<Order> getOrderById(@PathVariable String orderId) {
        return orderRepository.findById(orderId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private ResponseEntity<OrderResponse> updateStatus(String orderId, String newStatus) {
        return orderRepository.findById(orderId)
                .map(order -> {
                    order.setStatus(newStatus);
                    orderRepository.save(order);
                    return ResponseEntity.ok(new OrderResponse(order.getOrderId(), order.getStatus()));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
