package com.example.application.classes.service;

public class CompanyUserRow {
    private final long userId;
    private final String name;
    private final String email;
    private final boolean admin;

    public CompanyUserRow(long userId, String name, String email, boolean admin) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.admin = admin;
    }

    public long getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public boolean isAdmin() { return admin; }
}
