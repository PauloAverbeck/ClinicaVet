package com.example.application.examplefeature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;

@Entity
@Table(name = "app_user")
public class UserEntity extends AbstractEntity {

    @Column(name = "email_confirmation_time")
    @Nullable
    private LocalDateTime emailConfirmationTime;

    @Column(name = "email", nullable = false, length = 254, unique = true)
    private String email;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "provisional_password_hash", length = 100)
    private String provisionalPasswordHash;

    protected UserEntity() {
    }

    public @Nullable LocalDateTime getEmailConfirmationTime() {
        return emailConfirmationTime;
    }

    public UserEntity setEmailConfirmationTime(@Nullable LocalDateTime emailConfirmationTime) {
        this.emailConfirmationTime = emailConfirmationTime;
        return this;
    }

    public String getEmail() {
        return email;
    }

    public UserEntity setEmail(String email) {
        this.email = email;
        return this;
    }

    public String getName() {
        return name;
    }

    public UserEntity setName(String name) {
        this.name = name;
        return this;
    }

   public String getPasswordHash() {
        return passwordHash;
   }

   public UserEntity setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
   }

   public String getProvisionalPasswordHash() {
        return provisionalPasswordHash;
   }

   public UserEntity setProvisionalPasswordHash(@Nullable String provisionalPasswordHash) {
        this.provisionalPasswordHash = provisionalPasswordHash;
        return this;
   }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        UserEntity other = (UserEntity) obj;
        return getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
