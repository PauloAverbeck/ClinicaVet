package com.example.application.examplefeature;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "company")
public class CompanyEntity extends AbstractEntity {

    @Column(name = "document_type", nullable = false, length = 150)
    private String documentType;


    @Column(name = "document", nullable = false, length = 150, unique = true)
    private String document;

    @Column(name = "name", nullable = false, length = 150)
    private String name;


    protected CompanyEntity() {
    }

    public String getDocumentType() {
        return documentType;
    }

    public CompanyEntity setDocumentType(String documentType) {
        this.documentType = documentType;
        return this;
    }

    public String getDocument() {
        return document;
    }

    public CompanyEntity setDocument(String document) {
        this.document = document;
        return this;
    }

    public String getName() {
        return name;
    }

    public CompanyEntity setName(String name) {
        this.name = name;
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

        CompanyEntity other = (CompanyEntity) obj;
        return getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
