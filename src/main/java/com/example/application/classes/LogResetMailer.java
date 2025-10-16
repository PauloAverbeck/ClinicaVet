package com.example.application.classes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile({"dev","test","default"})
public class LogResetMailer implements ResetMailer {
    private static final Logger log = LoggerFactory.getLogger(LogResetMailer.class);

    @Value("${app.reset.base-url}")
    private String baseUrl;

    @Override
    public void send(String toEmail, String token) {
        String link = baseUrl + "/reset?token=" + token;
        log.info("[DEV/TEST] Reset de senha para {} -> {}", toEmail, link);
    }
}