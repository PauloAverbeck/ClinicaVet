package com.example.application.classes;

import java.time.LocalDateTime;


public abstract class AbstractEntity {
    private Long id;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;
    private Integer version;

    public Long getId() {
        return id;
    }

    public AbstractEntity setId(Long id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public AbstractEntity setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
        return this;
    }

    public LocalDateTime getUpdateDate() {
        return updateDate;
    }

    public AbstractEntity setUpdateDate(LocalDateTime updateDate) {
        this.updateDate = updateDate;
        return this;
    }

    public Integer getVersion() {
        return version;
    }

    public AbstractEntity setVersion(Integer version) {
        this.version = version;
        return this;
    }

    /* Helpers para DAO */
    public void markNew() {
        LocalDateTime now = LocalDateTime.now();
        this.creationDate = now;
        this.updateDate = now;
        if (this.version == null) this.version = 0;
    }

    public void markUpdated() {
        this.updateDate = LocalDateTime.now();
        if (this.version == null) this.version = 0;
        this.version += 1;
    }
}
