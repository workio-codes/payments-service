package com.payments.dto;

public class RazorpayVerifyResponse {
    private boolean verified;
    private String status;
    private String message;

    public RazorpayVerifyResponse() {
    }

    public RazorpayVerifyResponse(boolean verified, String status, String message) {
        this.verified = verified;
        this.status = status;
        this.message = message;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
