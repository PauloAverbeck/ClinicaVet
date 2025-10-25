package com.example.application.classes;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.time.LocalDateTime;

@Component
@Scope(value = "vaadin-session", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CurrentCompanyHolder implements Serializable {

    private Long companyId;
    private String companyName;
    private LocalDateTime selectedAt;

    public Long getCompanyId() {return companyId;}
    public String getCompanyName() {return companyName;}
    public LocalDateTime getSelectedAt() {return selectedAt;}

    public void setCompanyId(Long companyId,  String companyName) {
        this.companyId = companyId;
        this.companyName = companyName;
        this.selectedAt = LocalDateTime.now();
    }

    public void clear() {
        this.companyId = null;
        this.companyName = null;
        this.selectedAt = null;
    }

    public boolean isEmpty() {
        return this.companyId == null;
    }
}
