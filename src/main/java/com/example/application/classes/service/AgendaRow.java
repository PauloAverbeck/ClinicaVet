package com.example.application.classes.service;

import java.time.LocalDateTime;

public record AgendaRow (
        long id,
        LocalDateTime mainDateTime,
        boolean done,
        String petName,
        String species,
        String clientName,
        String description
) {
}
