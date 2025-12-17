package com.example.application.classes.service;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import org.springframework.stereotype.Component;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Component
@VaadinSessionScope
public class CurrentCompanyHolder implements Serializable {
    @Serial private static final long serialVersionUID = 1L;

    private Long companyId;
    private String companyName;
    private boolean admin;
    private LocalDateTime selectedAt;

    public void set(Long companyId, String companyName, boolean admin) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.admin = admin;
        this.selectedAt = LocalDateTime.now();
    }

    public void clear() {
        this.companyId = null;
        this.companyName = null;
        this.admin = false;
        this.selectedAt = null;
    }

    public boolean isSelected() { return companyId != null && companyId > 0; }
    public Long getCompanyId()            { return companyId; }
    public String getCompanyName()        { return companyName; }
    public boolean isAdmin()              { return admin; }
    public LocalDateTime getSelectedAt()  { return selectedAt; }
}