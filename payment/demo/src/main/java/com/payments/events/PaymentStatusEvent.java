package com.payments.events;

public class PaymentStatusEvent {
    private String orderId;
    private String paymentId;
    private String razorpayOrderId;
    private String status;
    private String reason;
    private long timestamp;

    public PaymentStatusEvent() {
    }

    public PaymentStatusEvent(String orderId, String paymentId, String razorpayOrderId, String status, String reason, long timestamp) {
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.razorpayOrderId = razorpayOrderId;
        this.status = status;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(String paymentId) {
        this.paymentId = paymentId;
    }

    public String getRazorpayOrderId() {
        return razorpayOrderId;
    }

    public void setRazorpayOrderId(String razorpayOrderId) {
        this.razorpayOrderId = razorpayOrderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
