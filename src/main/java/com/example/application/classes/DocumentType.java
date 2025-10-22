package com.example.application.classes;

public enum DocumentType {
    CPF, CNPJ, PASSPORT;

    public static DocumentType fromString(String s) {
        if (s == null) return null;
        return DocumentType.valueOf(s.trim().toUpperCase());
    }
}
