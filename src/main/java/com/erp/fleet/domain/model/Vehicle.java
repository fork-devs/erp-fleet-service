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

    public Vehicle(VehicleId id, TenantId tenantId, CustomerId customerId, Plate plate,
                   String brand, String model, Integer year, String vin,
                   ValidationStatus validationStatus, VehicleStatus status, Instant createdAt) {
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
    }

    public static Vehicle create(TenantId tenantId, CustomerId customerId, Plate plate,
                                  String brand, String model, Integer year, String vin) {
        return new Vehicle(VehicleId.generate(), tenantId, customerId, plate, brand, model, year, vin,
                ValidationStatus.PENDING, VehicleStatus.ACTIVO, Instant.now());
    }

    public void markValidated() { this.validationStatus = ValidationStatus.VALIDATED; }
    public void markValidationFailed() { this.validationStatus = ValidationStatus.FAILED; }
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

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vehicle v)) return false;
        return Objects.equals(id, v.id);
    }
    @Override public int hashCode() { return Objects.hash(id); }
}
