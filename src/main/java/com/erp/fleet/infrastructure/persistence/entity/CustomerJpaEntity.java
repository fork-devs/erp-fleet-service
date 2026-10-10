package com.erp.fleet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "customers")
public class CustomerJpaEntity {
    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "full_name", nullable = false)
    private String fullName;
    @Column
    private String email;
    @Column
    private String phone;
    @Column(name = "doc_type", nullable = false, length = 10)
    private String docType;
    @Column(name = "doc_number", nullable = false, length = 20)
    private String docNumber;
    @Column(name = "validation_status", nullable = false, length = 20)
    private String validationStatus;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "created_at")
    private Instant createdAt;

    public CustomerJpaEntity() {}

    public CustomerJpaEntity(UUID id, UUID tenantId, String fullName, String email, String phone,
                              String docType, String docNumber, String validationStatus, String status, Instant createdAt) {
        this.id = id; this.tenantId = tenantId; this.fullName = fullName;
        this.email = email; this.phone = phone; this.docType = docType;
        this.docNumber = docNumber; this.validationStatus = validationStatus;
        this.status = status; this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getDocType() { return docType; }
    public void setDocType(String docType) { this.docType = docType; }
    public String getDocNumber() { return docNumber; }
    public void setDocNumber(String docNumber) { this.docNumber = docNumber; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
