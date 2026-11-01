package com.erp.fleet.application.service;

import com.erp.fleet.application.dto.command.RescheduleAppointmentCommand;
import com.erp.fleet.application.dto.command.ScheduleAppointmentCommand;
import com.erp.fleet.application.dto.result.AppointmentResult;
import com.erp.fleet.domain.exception.AppointmentConflictException;
import com.erp.fleet.domain.exception.AppointmentNotFoundException;
import com.erp.fleet.domain.exception.CustomerNotFoundException;
import com.erp.fleet.domain.exception.VehicleNotFoundException;
import com.erp.fleet.domain.model.Appointment;
import com.erp.fleet.domain.model.AppointmentStatus;
import com.erp.fleet.domain.model.Customer;
import com.erp.fleet.domain.model.Vehicle;
import com.erp.fleet.domain.port.AppointmentRepositoryPort;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.port.VehicleRepositoryPort;
import com.erp.fleet.domain.valueobject.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentApplicationServiceTest {

    private AppointmentRepositoryPort appointmentRepository;
    private CustomerRepositoryPort customerRepository;
    private VehicleRepositoryPort vehicleRepository;
    private UnitOfWork unitOfWork;
    private AppointmentApplicationService service;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        appointmentRepository = Mockito.mock(AppointmentRepositoryPort.class);
        customerRepository = Mockito.mock(CustomerRepositoryPort.class);
        vehicleRepository = Mockito.mock(VehicleRepositoryPort.class);
        unitOfWork = Mockito.mock(UnitOfWork.class);

        // Passthrough UnitOfWork
        when(unitOfWork.callInTransaction(any())).thenAnswer(invocation -> {
            Supplier<?> supplier = invocation.getArgument(0);
            return supplier.get();
        });

        service = new AppointmentApplicationService(appointmentRepository, customerRepository, vehicleRepository, unitOfWork);
    }

    @Test
    @DisplayName("Should successfully schedule appointment")
    void shouldScheduleAppointment() {
        Instant start = Instant.now().plusSeconds(3600);
        ScheduleAppointmentCommand cmd = new ScheduleAppointmentCommand(
                tenantId, branchId, customerId, vehicleId, start, null, "Check oil"
        );

        when(customerRepository.findById(any(CustomerId.class), any(TenantId.class)))
                .thenReturn(Optional.of(Mockito.mock(Customer.class)));
        when(vehicleRepository.findById(any(VehicleId.class), any(TenantId.class)))
                .thenReturn(Optional.of(Mockito.mock(Vehicle.class)));
        when(appointmentRepository.hasOverlappingAppointment(any(), any(), any(), any(), any()))
                .thenReturn(false);
        when(appointmentRepository.save(any(Appointment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResult result = service.schedule(cmd);

        assertNotNull(result);
        assertEquals(tenantId, result.tenantId());
        assertEquals(branchId, result.branchId());
        assertEquals(customerId, result.customerId());
        assertEquals(vehicleId, result.vehicleId());
        assertEquals(start, result.scheduledStart());
        assertEquals(start.plusSeconds(3600), result.scheduledEnd());
        assertEquals("SCHEDULED", result.status());
        assertEquals("Check oil", result.notes());
    }

    @Test
    @DisplayName("Should reject schedule when customer does not exist")
    void shouldRejectScheduleWhenCustomerNotFound() {
        ScheduleAppointmentCommand cmd = new ScheduleAppointmentCommand(
                tenantId, branchId, customerId, vehicleId, Instant.now().plusSeconds(3600), null, null
        );

        when(customerRepository.findById(any(CustomerId.class), any(TenantId.class)))
                .thenReturn(Optional.empty());

        assertThrows(CustomerNotFoundException.class, () -> service.schedule(cmd));
    }

    @Test
    @DisplayName("Should reject schedule when vehicle does not exist")
    void shouldRejectScheduleWhenVehicleNotFound() {
        ScheduleAppointmentCommand cmd = new ScheduleAppointmentCommand(
                tenantId, branchId, customerId, vehicleId, Instant.now().plusSeconds(3600), null, null
        );

        when(customerRepository.findById(any(CustomerId.class), any(TenantId.class)))
                .thenReturn(Optional.of(Mockito.mock(Customer.class)));
        when(vehicleRepository.findById(any(VehicleId.class), any(TenantId.class)))
                .thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> service.schedule(cmd));
    }

    @Test
    @DisplayName("Should reject schedule when dates overlap with existing appointment")
    void shouldRejectScheduleWhenOverlap() {
        ScheduleAppointmentCommand cmd = new ScheduleAppointmentCommand(
                tenantId, branchId, customerId, vehicleId, Instant.now().plusSeconds(3600), null, null
        );

        when(customerRepository.findById(any(CustomerId.class), any(TenantId.class)))
                .thenReturn(Optional.of(Mockito.mock(Customer.class)));
        when(vehicleRepository.findById(any(VehicleId.class), any(TenantId.class)))
                .thenReturn(Optional.of(Mockito.mock(Vehicle.class)));
        when(appointmentRepository.hasOverlappingAppointment(any(), any(), any(), any(), any()))
                .thenReturn(true);

        assertThrows(AppointmentConflictException.class, () -> service.schedule(cmd));
    }

    @Test
    @DisplayName("Should reject schedule when start date is in the past")
    void shouldRejectScheduleWhenStartInPast() {
        ScheduleAppointmentCommand cmd = new ScheduleAppointmentCommand(
                tenantId, branchId, customerId, vehicleId, Instant.now().minusSeconds(3600), null, null
        );

        when(customerRepository.findById(any(CustomerId.class), any(TenantId.class)))
                .thenReturn(Optional.of(Mockito.mock(Customer.class)));
        when(vehicleRepository.findById(any(VehicleId.class), any(TenantId.class)))
                .thenReturn(Optional.of(Mockito.mock(Vehicle.class)));

        assertThrows(IllegalArgumentException.class, () -> service.schedule(cmd));
    }

    @Test
    @DisplayName("Should get appointment by ID")
    void shouldGetAppointmentById() {
        Appointment appointment = Appointment.create(
                new AppointmentId(appointmentId), new TenantId(tenantId), new BranchId(branchId),
                new CustomerId(customerId), new VehicleId(vehicleId), Instant.now().plusSeconds(3600), null, "Note"
        );

        when(appointmentRepository.findById(new AppointmentId(appointmentId), new TenantId(tenantId)))
                .thenReturn(Optional.of(appointment));

        AppointmentResult result = service.getById(appointmentId, tenantId);

        assertNotNull(result);
        assertEquals(appointmentId, result.id());
    }

    @Test
    @DisplayName("Should throw when appointment not found by ID")
    void shouldThrowWhenAppointmentNotFound() {
        when(appointmentRepository.findById(any(), any())).thenReturn(Optional.empty());

        assertThrows(AppointmentNotFoundException.class, () -> service.getById(appointmentId, tenantId));
    }

    @Test
    @DisplayName("Should list appointments with filters")
    void shouldListAppointments() {
        Appointment appointment = Appointment.create(
                new AppointmentId(appointmentId), new TenantId(tenantId), new BranchId(branchId),
                new CustomerId(customerId), new VehicleId(vehicleId), Instant.now().plusSeconds(3600), null, "Note"
        );

        when(appointmentRepository.findWithFilters(any(), any(), any(), any(), any()))
                .thenReturn(List.of(appointment));

        List<AppointmentResult> results = service.list(tenantId, branchId, null, null, "SCHEDULED");

        assertEquals(1, results.size());
        assertEquals(appointmentId, results.get(0).id());
    }

    @Test
    @DisplayName("Should reschedule appointment")
    void shouldRescheduleAppointment() {
        Appointment appointment = Appointment.create(
                new AppointmentId(appointmentId), new TenantId(tenantId), new BranchId(branchId),
                new CustomerId(customerId), new VehicleId(vehicleId), Instant.now().plusSeconds(3600), null, "Note"
        );

        when(appointmentRepository.findById(any(), any())).thenReturn(Optional.of(appointment));
        when(appointmentRepository.hasOverlappingAppointment(any(), any(), any(), any(), any())).thenReturn(false);
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UUID newBranch = UUID.randomUUID();
        Instant newStart = Instant.now().plusSeconds(7200);
        Instant newEnd = newStart.plusSeconds(3600);

        RescheduleAppointmentCommand cmd = new RescheduleAppointmentCommand(
                appointmentId, tenantId, newBranch, newStart, newEnd, "New Notes"
        );

        AppointmentResult result = service.reschedule(cmd);

        assertEquals(newBranch, result.branchId());
        assertEquals(newStart, result.scheduledStart());
        assertEquals(newEnd, result.scheduledEnd());
        assertEquals("New Notes", result.notes());
    }

    @Test
    @DisplayName("Should handle confirm, checkIn, complete, cancel lifecycle")
    void shouldHandleLifecycle() {
        Appointment appointment = Appointment.create(
                new AppointmentId(appointmentId), new TenantId(tenantId), new BranchId(branchId),
                new CustomerId(customerId), new VehicleId(vehicleId), Instant.now().plusSeconds(3600), null, "Note"
        );

        when(appointmentRepository.findById(any(), any())).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResult confirmed = service.confirm(appointmentId, tenantId);
        assertEquals("CONFIRMED", confirmed.status());

        AppointmentResult checkedIn = service.checkIn(appointmentId, tenantId);
        assertEquals("IN_PROGRESS", checkedIn.status());

        AppointmentResult completed = service.complete(appointmentId, tenantId);
        assertEquals("COMPLETED", completed.status());

        // Cancel test with fresh appointment
        Appointment cancelable = Appointment.create(
                new AppointmentId(appointmentId), new TenantId(tenantId), new BranchId(branchId),
                new CustomerId(customerId), new VehicleId(vehicleId), Instant.now().plusSeconds(3600), null, "Note"
        );
        when(appointmentRepository.findById(any(), any())).thenReturn(Optional.of(cancelable));

        AppointmentResult cancelled = service.cancel(appointmentId, tenantId, "Customer cannot attend");
        assertEquals("CANCELLED", cancelled.status());
        assertEquals("Customer cannot attend", cancelled.cancellationReason());
    }
}
