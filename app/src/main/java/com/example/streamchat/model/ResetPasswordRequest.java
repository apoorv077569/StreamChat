package com.example.streamchat.model;

public class ResetPasswordRequest {
    private String email;
    private String newPassword;

    public ResetPasswordRequest(String email, final String newPassword) {
        this.email = email;
        this.newPassword = newPassword;
    }
}
