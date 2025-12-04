package com.example.application.classes.service;

public class PetValidationException extends RuntimeException {
    public PetValidationException(String message) {
        super(message);
    }
}