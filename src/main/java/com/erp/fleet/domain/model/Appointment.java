package com.erp.fleet.domain.model;

import com.erp.fleet.domain.exception.AppointmentConflictException;
import com.erp.fleet.domain.valueobject.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class Appointment implements Serializable {
    private final AppointmentId id;
    private final TenantId tenantId;
    private BranchId branchId;
    private final CustomerId customerId;
    private final VehicleId vehicleId;
    private Instant scheduledStart;
    private Instant scheduledEnd;
    private AppointmentStatus status;
    private String notes;
    private String cancellationReason;
    private final Instant createdAt;
    private Instant updatedAt;

    public Appointment(AppointmentId id, TenantId tenantId, BranchId branchId,
                       CustomerId customerId, VehicleId vehicleId,
                       Instant scheduledStart, Instant scheduledEnd,
                       AppointmentStatus status, String notes,
                       String cancellationReason, Instant createdAt, Instant updatedAt) {
        if (id == null || tenantId == null || branchId == null || customerId == null || vehicleId == null) {
            throw new IllegalArgumentException("Campos obligatorios de Appointment no pueden ser nulos");
        }
        if (scheduledStart == null || scheduledEnd == null) {
            throw new IllegalArgumentException("scheduledStart y scheduledEnd no pueden ser nulos");
        }
        if (!scheduledEnd.isAfter(scheduledStart)) {
            throw new IllegalArgumentException("scheduledEnd debe ser posterior a scheduledStart");
        }

        this.id = id;
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.customerId = customerId;
        this.vehicleId = vehicleId;
        this.scheduledStart = scheduledStart;
        this.scheduledEnd = scheduledEnd;
        this.status = status != null ? status : AppointmentStatus.SCHEDULED;
        this.notes = notes != null && notes.length() > 500 ? notes.substring(0, 500) : notes;
        this.cancellationReason = cancellationReason;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static Appointment create(AppointmentId id, TenantId tenantId, BranchId branchId,
                                    CustomerId customerId, VehicleId vehicleId,
                                    Instant scheduledStart, Instant scheduledEnd, String notes) {
        Instant effectiveEnd = scheduledEnd != null ? scheduledEnd : scheduledStart.plusSeconds(3600);
        return new Appointment(
                id != null ? id : AppointmentId.generate(),
                tenantId,
                branchId,
                customerId,
                vehicleId,
                scheduledStart,
                effectiveEnd,
                AppointmentStatus.SCHEDULED,
                notes,
                null,
                Instant.now(),
                Instant.now()
        );
    }

    public void confirm() {
        if (this.status == AppointmentStatus.CANCELLED || this.status == AppointmentStatus.COMPLETED) {
            throw new AppointmentConflictException("No se puede confirmar una cita en estado: " + this.status);
        }
        this.status = AppointmentStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void checkIn() {
        if (this.status != AppointmentStatus.SCHEDULED && this.status != AppointmentStatus.CONFIRMED) {
            throw new AppointmentConflictException("Solo se puede registrar check-in en citas programadas o confirmadas (estado actual: " + this.status + ")");
        }
        this.status = AppointmentStatus.IN_PROGRESS;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (this.status != AppointmentStatus.IN_PROGRESS) {
            throw new AppointmentConflictException("Solo se puede completar una cita en curso (estado actual: " + this.status + ")");
        }
        this.status = AppointmentStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (this.status == AppointmentStatus.COMPLETED || this.status == AppointmentStatus.CANCELLED) {
            throw new AppointmentConflictException("No se puede cancelar una cita en estado: " + this.status);
        }
        this.status = AppointmentStatus.CANCELLED;
        this.cancellationReason = reason != null ? reason.trim() : null;
        this.updatedAt = Instant.now();
    }

    public void reschedule(BranchId newBranchId, Instant newStart, Instant newEnd, String newNotes) {
        if (this.status != AppointmentStatus.SCHEDULED && this.status != AppointmentStatus.CONFIRMED) {
            throw new AppointmentConflictException("Solo se pueden reprogramar citas en estado SCHEDULED o CONFIRMED");
        }
        if (newStart == null || newEnd == null) {
            throw new IllegalArgumentException("Nuevas fechas no pueden ser nulas");
        }
        if (!newEnd.isAfter(newStart)) {
            throw new IllegalArgumentException("scheduledEnd debe ser posterior a scheduledStart");
        }
        if (newBranchId != null) {
            this.branchId = newBranchId;
        }
        this.scheduledStart = newStart;
        this.scheduledEnd = newEnd;
        if (newNotes != null) {
            this.notes = newNotes.length() > 500 ? newNotes.substring(0, 500) : newNotes;
        }
        this.updatedAt = Instant.now();
    }

    public AppointmentId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public BranchId getBranchId() {
        return branchId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public VehicleId getVehicleId() {
        return vehicleId;
    }

    public Instant getScheduledStart() {
        return scheduledStart;
    }

    public Instant getScheduledEnd() {
        return scheduledEnd;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public String getNotes() {
        return notes;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Appointment that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
