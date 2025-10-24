package com.example.application.classes.model;

import com.example.application.classes.DocumentType;

import java.time.LocalDateTime;

public class Company {
    private long id;
    private int version;
    private LocalDateTime creationDate;
    private LocalDateTime updateDate;

    private String name;
    private DocumentType documentType;
    private String document;

    public long getId() { return id; }
    public Company setId(long id) { this.id = id; return this; }

    public int getVersion() { return version; }
    public Company setVersion(int version) { this.version = version; return this; }

    public LocalDateTime getCreationDate() { return creationDate; }
    public Company setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; return this; }

    public LocalDateTime getUpdateDate() { return updateDate; }
    public Company setUpdateDate(LocalDateTime updateDate) { this.updateDate = updateDate; return this; }

    public String getName() { return name; }
    public Company setName(String name) { this.name = name; return this; }

    public DocumentType getDocumentType() { return documentType; }
    public Company setDocumentType(DocumentType documentType) { this.documentType = documentType; return this; }

    public String getDocument() { return document; }
    public Company setDocument(String document) { this.document = document; return this; }
}