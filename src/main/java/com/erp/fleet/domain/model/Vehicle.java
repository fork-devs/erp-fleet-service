package com.erp.fleet.domain.model;

import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.Plate;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.domain.valueobject.VehicleId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class Vehicle implements Serializable {
    private final VehicleId id;
    private final TenantId tenantId;
    private final CustomerId customerId;
    private final Plate plate;
    private String brand;
    private String model;
    private Integer year;
    private String vin;
    private ValidationStatus validationStatus;
    private VehicleStatus status;
    private final Instant createdAt;

    // Datos que devuelve el integration-hub al validar la placa.
    private String numeroMotor;
    private String color;
    private String combustible;
    private Instant validatedAt;

    // Estado del reintento programado de la validacion.
    private int validationAttempts;
    private Instant nextValidationAt;

    public Vehicle(VehicleId id, TenantId tenantId, CustomerId customerId, Plate plate,
                   String brand, String model, Integer year, String vin,
                   ValidationStatus validationStatus, VehicleStatus status, Instant createdAt) {
        this(id, tenantId, customerId, plate, brand, model, year, vin, validationStatus, status,
                createdAt, null, null, null, null, 0, null);
    }

    public Vehicle(VehicleId id, TenantId tenantId, CustomerId customerId, Plate plate,
                   String brand, String model, Integer year, String vin,
                   ValidationStatus validationStatus, VehicleStatus status, Instant createdAt,
                   String numeroMotor, String color, String combustible, Instant validatedAt,
                   int validationAttempts, Instant nextValidationAt) {
        if (id == null || tenantId == null || customerId == null || plate == null)
            throw new IllegalArgumentException("Campos obligatorios del Vehicle no pueden ser nulos");
        this.id = id;
        this.tenantId = tenantId;
        this.customerId = customerId;
        this.plate = plate;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.vin = vin;
        this.validationStatus = validationStatus != null ? validationStatus : ValidationStatus.PENDING;
        this.status = status != null ? status : VehicleStatus.ACTIVO;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.numeroMotor = numeroMotor;
        this.color = color;
        this.combustible = combustible;
        this.validatedAt = validatedAt;
        this.validationAttempts = validationAttempts;
        this.nextValidationAt = nextValidationAt;
    }

    public static Vehicle create(TenantId tenantId, CustomerId customerId, Plate plate,
                                  String brand, String model, Integer year, String vin) {
        return new Vehicle(VehicleId.generate(), tenantId, customerId, plate, brand, model, year, vin,
                ValidationStatus.PENDING, VehicleStatus.ACTIVO, Instant.now());
    }

    /**
     * El hub declara la placa valida: se guarda lo que informa.
     */
    public void markValidated(String vin, String numeroMotor, String brand, String model,
                               String color, String combustible, Instant validatedAt) {
        this.validationStatus = ValidationStatus.VALID;
        if (vin != null) this.vin = vin;
        this.numeroMotor = numeroMotor;
        this.color = color;
        this.combustible = combustible;
        if (brand != null) this.brand = brand;
        if (model != null) this.model = model;
        this.validatedAt = validatedAt;
    }

    /**
     * El hub no reconoce la placa: el registro se conserva y queda observado,
     * para que el taller lo revise.
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

    public void deactivate() { this.status = VehicleStatus.INACTIVO; }
    public boolean isActive() { return this.status == VehicleStatus.ACTIVO; }

    public void update(String brand, String model, Integer year, String vin) {
        if (brand != null) this.brand = brand;
        if (model != null) this.model = model;
        if (year != null) this.year = year;
        if (vin != null) this.vin = vin;
    }

    public VehicleId getId() { return id; }
    public TenantId getTenantId() { return tenantId; }
    public CustomerId getCustomerId() { return customerId; }
    public Plate getPlate() { return plate; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public Integer getYear() { return year; }
    public String getVin() { return vin; }
    public ValidationStatus getValidationStatus() { return validationStatus; }
    public VehicleStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public String getNumeroMotor() { return numeroMotor; }
    public String getColor() { return color; }
    public String getCombustible() { return combustible; }
    public Instant getValidatedAt() { return validatedAt; }
    public int getValidationAttempts() { return validationAttempts; }
    public Instant getNextValidationAt() { return nextValidationAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle v)) return false;
        return Objects.equals(id, v.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
