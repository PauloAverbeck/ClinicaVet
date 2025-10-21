package com.example.application.classes;

public interface ResetMailer {
    void sendProvisionalPassword(String toEmail, String provisionalPlain, String reason);
}
