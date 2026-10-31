package com.erp.fleet.domain;

import com.erp.fleet.domain.exception.AppointmentConflictException;
import com.erp.fleet.domain.model.Appointment;
import com.erp.fleet.domain.model.AppointmentStatus;
import com.erp.fleet.domain.valueobject.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AppointmentDomainTest {

    private final TenantId tenantId = new TenantId(UUID.randomUUID());
    private final BranchId branchId = new BranchId(UUID.randomUUID());
    private final CustomerId customerId = new CustomerId(UUID.randomUUID());
    private final VehicleId vehicleId = new VehicleId(UUID.randomUUID());

    @Test
    @DisplayName("Should successfully create appointment with default 1 hour duration")
    void shouldCreateAppointmentWithDefaults() {
        Instant start = Instant.now().plusSeconds(7200);

        Appointment appointment = Appointment.create(
                null, tenantId, branchId, customerId, vehicleId, start, null, "Regular maintenance"
        );

        assertNotNull(appointment.getId());
        assertEquals(tenantId, appointment.getTenantId());
        assertEquals(branchId, appointment.getBranchId());
        assertEquals(customerId, appointment.getCustomerId());
        assertEquals(vehicleId, appointment.getVehicleId());
        assertEquals(start, appointment.getScheduledStart());
        assertEquals(start.plusSeconds(3600), appointment.getScheduledEnd());
        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());
        assertEquals("Regular maintenance", appointment.getNotes());
        assertNull(appointment.getCancellationReason());
        assertNotNull(appointment.getCreatedAt());
        assertNotNull(appointment.getUpdatedAt());
    }

    @Test
    @DisplayName("Should validate required fields and dates ordering")
    void shouldValidateInvariants() {
        Instant now = Instant.now();
        Instant future = now.plusSeconds(3600);

        assertThrows(IllegalArgumentException.class, () -> new Appointment(
                null, null, branchId, customerId, vehicleId, now, future, AppointmentStatus.SCHEDULED, null, null, null, null
        ));
        assertThrows(IllegalArgumentException.class, () -> new Appointment(
                new AppointmentId(UUID.randomUUID()), tenantId, null, customerId, vehicleId, now, future, AppointmentStatus.SCHEDULED, null, null, null, null
        ));
        assertThrows(IllegalArgumentException.class, () -> new Appointment(
                new AppointmentId(UUID.randomUUID()), tenantId, branchId, null, vehicleId, now, future, AppointmentStatus.SCHEDULED, null, null, null, null
        ));
        assertThrows(IllegalArgumentException.class, () -> new Appointment(
                new AppointmentId(UUID.randomUUID()), tenantId, branchId, customerId, null, now, future, AppointmentStatus.SCHEDULED, null, null, null, null
        ));
        assertThrows(IllegalArgumentException.class, () -> new Appointment(
                new AppointmentId(UUID.randomUUID()), tenantId, branchId, customerId, vehicleId, null, future, AppointmentStatus.SCHEDULED, null, null, null, null
        ));
        assertThrows(IllegalArgumentException.class, () -> new Appointment(
                new AppointmentId(UUID.randomUUID()), tenantId, branchId, customerId, vehicleId, now, null, AppointmentStatus.SCHEDULED, null, null, null, null
        ));
        // End before start
        assertThrows(IllegalArgumentException.class, () -> new Appointment(
                new AppointmentId(UUID.randomUUID()), tenantId, branchId, customerId, vehicleId, future, now, AppointmentStatus.SCHEDULED, null, null, null, null
        ));
        // End equals start
        assertThrows(IllegalArgumentException.class, () -> new Appointment(
                new AppointmentId(UUID.randomUUID()), tenantId, branchId, customerId, vehicleId, now, now, AppointmentStatus.SCHEDULED, null, null, null, null
        ));
    }

    @Test
    @DisplayName("Should handle lifecycle transitions: confirm, checkIn, complete")
    void shouldHandleLifecycleTransitions() {
        Instant start = Instant.now().plusSeconds(3600);
        Appointment appointment = Appointment.create(
                null, tenantId, branchId, customerId, vehicleId, start, start.plusSeconds(3600), "Tire change"
        );

        assertEquals(AppointmentStatus.SCHEDULED, appointment.getStatus());

        appointment.confirm();
        assertEquals(AppointmentStatus.CONFIRMED, appointment.getStatus());

        appointment.checkIn();
        assertEquals(AppointmentStatus.IN_PROGRESS, appointment.getStatus());

        appointment.complete();
        assertEquals(AppointmentStatus.COMPLETED, appointment.getStatus());

        // Once completed, cannot check-in or cancel
        assertThrows(AppointmentConflictException.class, appointment::checkIn);
        assertThrows(AppointmentConflictException.class, () -> appointment.cancel("Too late"));
    }

    @Test
    @DisplayName("Should handle cancellation and reject subsequent operations")
    void shouldHandleCancellation() {
        Instant start = Instant.now().plusSeconds(3600);
        Appointment appointment = Appointment.create(
                null, tenantId, branchId, customerId, vehicleId, start, start.plusSeconds(3600), "Brakes check"
        );

        appointment.cancel("Customer called to cancel");
        assertEquals(AppointmentStatus.CANCELLED, appointment.getStatus());
        assertEquals("Customer called to cancel", appointment.getCancellationReason());

        assertThrows(AppointmentConflictException.class, appointment::confirm);
        assertThrows(AppointmentConflictException.class, appointment::checkIn);
        assertThrows(AppointmentConflictException.class, () -> appointment.cancel("Again"));
    }

    @Test
    @DisplayName("Should reschedule appointment dates and branch")
    void shouldRescheduleAppointment() {
        Instant start = Instant.now().plusSeconds(3600);
        Appointment appointment = Appointment.create(
                null, tenantId, branchId, customerId, vehicleId, start, start.plusSeconds(3600), "Initial"
        );

        BranchId newBranch = new BranchId(UUID.randomUUID());
        Instant newStart = start.plusSeconds(86400);
        Instant newEnd = newStart.plusSeconds(7200);

        appointment.reschedule(newBranch, newStart, newEnd, "Rescheduled to new branch");

        assertEquals(newBranch, appointment.getBranchId());
        assertEquals(newStart, appointment.getScheduledStart());
        assertEquals(newEnd, appointment.getScheduledEnd());
        assertEquals("Rescheduled to new branch", appointment.getNotes());

        // Cannot reschedule if IN_PROGRESS
        appointment.checkIn();
        assertThrows(AppointmentConflictException.class, () -> appointment.reschedule(newBranch, newStart, newEnd, "Invalid"));
    }

    @Test
    @DisplayName("Value objects: AppointmentId, BranchId, AppointmentStatus")
    void testValueObjects() {
        UUID u1 = UUID.randomUUID();
        AppointmentId a1 = new AppointmentId(u1);
        AppointmentId a2 = new AppointmentId(u1);
        AppointmentId a3 = AppointmentId.generate();

        assertEquals(a1, a2);
        assertNotEquals(a1, a3);
        assertEquals(a1.hashCode(), a2.hashCode());
        assertEquals(u1.toString(), a1.toString());
        assertEquals(u1, a1.getValue());
        assertEquals(a1, AppointmentId.fromString(u1.toString()));

        UUID bUid = UUID.randomUUID();
        BranchId b1 = new BranchId(bUid);
        BranchId b2 = new BranchId(bUid);
        assertEquals(b1, b2);
        assertEquals(b1.hashCode(), b2.hashCode());
        assertEquals(bUid.toString(), b1.toString());
        assertEquals(bUid, b1.getValue());
        assertEquals(b1, BranchId.fromString(bUid.toString()));

        assertTrue(AppointmentStatus.SCHEDULED.isActive());
        assertTrue(AppointmentStatus.CONFIRMED.isActive());
        assertTrue(AppointmentStatus.IN_PROGRESS.isActive());
        assertFalse(AppointmentStatus.COMPLETED.isActive());
        assertFalse(AppointmentStatus.CANCELLED.isActive());

        assertThrows(IllegalArgumentException.class, () -> new AppointmentId(null));
        assertThrows(IllegalArgumentException.class, () -> new BranchId(null));
    }
}
