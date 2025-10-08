package com.example.application.classes;

import org.jspecify.annotations.Nullable;
import java.time.LocalDateTime;

public class AppUser extends AbstractEntity {
    @Nullable
    private LocalDateTime emailConfirmationTime;
    private String email;
    private String name;
    private String passwordHash;
    @Nullable
    private String provisionalPasswordHash;

    public AppUser() {
    }

    @Nullable
    public LocalDateTime getEmailConfirmationTime() {
        return emailConfirmationTime;
    }

    public AppUser setEmailConfirmationTime(@Nullable LocalDateTime emailConfirmationTime) {
        this.emailConfirmationTime = emailConfirmationTime;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public AppUser setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getName() {
        return name;
    }

    public AppUser setName(String name) {
        this.name = name;
        return this;
    }

   public String getPasswordHash() {
        return passwordHash;
   }

   public AppUser setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
   }

   @Nullable
   public String getProvisionalPasswordHash() {
        return provisionalPasswordHash;
   }

   public AppUser setProvisionalPasswordHash(@Nullable String provisionalPasswordHash) {
        this.provisionalPasswordHash = provisionalPasswordHash;
        return this;
   }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AppUser other = (AppUser) obj;
        return getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }
}
