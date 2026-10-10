package com.erp.fleet.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
public class VehicleJpaEntity {
    @Id
    private UUID id;
    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;
    @Column(nullable = false, length = 10)
    private String plate;
    @Column
    private String brand;
    @Column
    private String model;
    @Column
    private Integer year;
    @Column
    private String vin;
    @Column(name = "validation_status", nullable = false, length = 20)
    private String validationStatus;
    @Column(nullable = false, length = 20)
    private String status;
    @Column(name = "created_at")
    private Instant createdAt;

    public VehicleJpaEntity() {}

    public VehicleJpaEntity(UUID id, UUID tenantId, UUID customerId, String plate, String brand,
                             String model, Integer year, String vin, String validationStatus, String status, Instant createdAt) {
        this.id = id; this.tenantId = tenantId; this.customerId = customerId; this.plate = plate;
        this.brand = brand; this.model = model; this.year = year; this.vin = vin;
        this.validationStatus = validationStatus; this.status = status; this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public UUID getCustomerId() { return customerId; }
    public void setCustomerId(UUID customerId) { this.customerId = customerId; }
    public String getPlate() { return plate; }
    public void setPlate(String plate) { this.plate = plate; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    public String getVin() { return vin; }
    public void setVin(String vin) { this.vin = vin; }
    public String getValidationStatus() { return validationStatus; }
    public void setValidationStatus(String validationStatus) { this.validationStatus = validationStatus; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
