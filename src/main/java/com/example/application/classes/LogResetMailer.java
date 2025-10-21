package com.example.application.classes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"dev","test","default"})
public class LogResetMailer implements ResetMailer {
    private static final Logger log = LoggerFactory.getLogger(LogResetMailer.class);

    @Override
    public void sendProvisionalPassword(String toEmail, String provisionalPlain, String reason) {
        log.warn("[DEV/TEST] ({}) Senha provisória para {} -> {}", reason, toEmail, provisionalPlain);
    }
}