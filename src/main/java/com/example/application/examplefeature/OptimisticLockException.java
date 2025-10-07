package com.example.application.examplefeature;

/**
 * Exceção usada quando há conflito de versão (optimistic locking).
 */
public class OptimisticLockException extends RuntimeException {
    public OptimisticLockException(String message) {
        super(message);
    }
}