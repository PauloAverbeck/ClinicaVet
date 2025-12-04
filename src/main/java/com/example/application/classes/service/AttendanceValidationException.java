package com.example.application.classes.service;

public class AttendanceValidationException extends RuntimeException {
    public AttendanceValidationException(String message) {
        super(message);
    }
}