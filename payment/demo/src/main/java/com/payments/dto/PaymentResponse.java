package com.payments.dto;

public class PaymentResponse {
    private Long paymentId;
    private String status;
    private String timestamp;

    public PaymentResponse() {}

    public PaymentResponse(Long paymentId, String status, String timestamp) {
        this.paymentId = paymentId;
        this.status = status;
        this.timestamp = timestamp;
    }

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}