package com.example.application.examplefeature;

import java.util.Objects;

public class Company extends AbstractEntity {
    private String documentType;
    private String document;
    private String name;

    public Company() {}

    public String getDocumentType() {
        return documentType;
    }

    public Company setDocumentType(String documentType) {
        this.documentType = documentType;
        return this;
    }

    public String getDocument() {
        return document;
    }

    public Company setDocument(String document) {
        this.document = document;
        return this;
    }

    public String getName() {
        return name;
    }

    public Company setName(String name) {
        this.name = name;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Company other = (Company) obj;
        return getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}