package com.erp.fleet.domain.model;

import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.DocNumber;
import com.erp.fleet.domain.valueobject.TenantId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class Customer implements Serializable {
    private final CustomerId id;
    private final TenantId tenantId;
    private String fullName;
    private String email;
    private String phone;
    private final DocNumber docNumber;
    private ValidationStatus validationStatus;
    private CustomerStatus status;
    private final Instant createdAt;

    public Customer(CustomerId id, TenantId tenantId, String fullName,
                    String email, String phone, DocNumber docNumber,
                    ValidationStatus validationStatus, CustomerStatus status, Instant createdAt) {
        if (id == null || tenantId == null || fullName == null || docNumber == null)
            throw new IllegalArgumentException("Campos obligatorios del Customer no pueden ser nulos");
        if (fullName.trim().isEmpty())
            throw new IllegalArgumentException("El nombre completo no puede estar vacío");
        this.id = id;
        this.tenantId = tenantId;
        this.fullName = fullName.trim();
        this.email = email;
        this.phone = phone;
        this.docNumber = docNumber;
        this.validationStatus = validationStatus != null ? validationStatus : ValidationStatus.PENDING;
        this.status = status != null ? status : CustomerStatus.ACTIVO;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public static Customer create(TenantId tenantId, String fullName, String email,
                                   String phone, DocNumber docNumber) {
        return new Customer(CustomerId.generate(), tenantId, fullName, email, phone,
                docNumber, ValidationStatus.PENDING, CustomerStatus.ACTIVO, Instant.now());
    }

    public void markValidated() { this.validationStatus = ValidationStatus.VALIDATED; }
    public void markValidationFailed() { this.validationStatus = ValidationStatus.FAILED; }
    public void deactivate() { this.status = CustomerStatus.INACTIVO; }
    public void activate() { this.status = CustomerStatus.ACTIVO; }
    public boolean isActive() { return this.status == CustomerStatus.ACTIVO; }

    public void update(String fullName, String email, String phone) {
        if (fullName != null && !fullName.trim().isEmpty()) this.fullName = fullName.trim();
        if (email != null) this.email = email;
        if (phone != null) this.phone = phone;
    }

    public CustomerId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public DocNumber getDocNumber() { return docNumber; }
    public ValidationStatus getValidationStatus() { return validationStatus; }
    public CustomerStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer c)) return false;
        return Objects.equals(id, c.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
