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

    // Datos que devuelve el integration-hub al validar el documento.
    private String razonSocial;
    private String estadoRuc;
    private String condicionRuc;
    private String direccionFiscal;
    private Instant validatedAt;

    // Estado del reintento programado de la validacion.
    private int validationAttempts;
    private Instant nextValidationAt;

    public Customer(CustomerId id, TenantId tenantId, String fullName,
                    String email, String phone, DocNumber docNumber,
                    ValidationStatus validationStatus, CustomerStatus status, Instant createdAt) {
        this(id, tenantId, fullName, email, phone, docNumber, validationStatus, status, createdAt,
                null, null, null, null, null, 0, null);
    }

    public Customer(CustomerId id, TenantId tenantId, String fullName,
                    String email, String phone, DocNumber docNumber,
                    ValidationStatus validationStatus, CustomerStatus status, Instant createdAt,
                    String razonSocial, String estadoRuc, String condicionRuc, String direccionFiscal,
                    Instant validatedAt, int validationAttempts, Instant nextValidationAt) {
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
        this.razonSocial = razonSocial;
        this.estadoRuc = estadoRuc;
        this.condicionRuc = condicionRuc;
        this.direccionFiscal = direccionFiscal;
        this.validatedAt = validatedAt;
        this.validationAttempts = validationAttempts;
        this.nextValidationAt = nextValidationAt;
    }

    public static Customer create(TenantId tenantId, String fullName, String email,
                                   String phone, DocNumber docNumber) {
        return new Customer(CustomerId.generate(), tenantId, fullName, email, phone,
                docNumber, ValidationStatus.PENDING, CustomerStatus.ACTIVO, Instant.now());
    }

    /**
     * El hub declara el documento valido: se guarda lo que informa.
     */
    public void markValidated(String razonSocial, String estadoRuc, String condicionRuc,
                               String direccionFiscal, Instant validatedAt) {
        this.validationStatus = ValidationStatus.VALID;
        this.razonSocial = razonSocial;
        this.estadoRuc = estadoRuc;
        this.condicionRuc = condicionRuc;
        this.direccionFiscal = direccionFiscal;
        this.validatedAt = validatedAt;
    }

    /**
     * El hub informa una discrepancia: el registro se conserva y queda
     * observado, para que el taller lo revise.
     */
    public void markObserved() {
        this.validationStatus = ValidationStatus.OBSERVED;
    }

    /**
     * Se registra un intento de validacion que no pudo completarse, y cuando
     * hay que volver a intentarlo.
     */
    public void registerValidationAttempt(Instant nextValidationAt) {
        this.validationAttempts = this.validationAttempts + 1;
        this.nextValidationAt = nextValidationAt;
    }

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
    public String getRazonSocial() { return razonSocial; }
    public String getEstadoRuc() { return estadoRuc; }
    public String getCondicionRuc() { return condicionRuc; }
    public String getDireccionFiscal() { return direccionFiscal; }
    public Instant getValidatedAt() { return validatedAt; }
    public int getValidationAttempts() { return validationAttempts; }
    public Instant getNextValidationAt() { return nextValidationAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer c)) return false;
        return Objects.equals(id, c.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
