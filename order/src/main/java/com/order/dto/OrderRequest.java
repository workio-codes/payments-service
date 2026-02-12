package com.order.dto;

public class OrderRequest {
    private String customerName;
    private Double totalAmount;
    private String orderId; // optional, auto-generated if null
    private String paymentMethod; // optional, defaulted if null

    public OrderRequest() {}

    // getters and setters
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }
}