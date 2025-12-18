package com.example.streamchat.model;

public class VerifyOtpRequest {
    public String email;
    public String otp;

    public VerifyOtpRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }
}

