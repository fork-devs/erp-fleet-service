package com.erp.fleet.infrastructure.persistence;

import com.erp.fleet.domain.model.Appointment;
import com.erp.fleet.domain.model.AppointmentStatus;
import com.erp.fleet.domain.valueobject.*;
import com.erp.fleet.infrastructure.persistence.adapter.AppointmentPersistenceAdapter;
import com.erp.fleet.infrastructure.persistence.entity.AppointmentJpaEntity;
import com.erp.fleet.infrastructure.persistence.mapper.AppointmentPersistenceMapper;
import com.erp.fleet.infrastructure.persistence.repository.SpringDataAppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AppointmentPersistenceAdapterTest {

    private SpringDataAppointmentRepository repository;
    private AppointmentPersistenceAdapter adapter;

    private final UUID tenantId = UUID.randomUUID();
    private final UUID branchId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();
    private final UUID vehicleId = UUID.randomUUID();
    private final UUID appointmentId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(SpringDataAppointmentRepository.class);
        adapter = new AppointmentPersistenceAdapter(repository);
    }

    @Test
    @DisplayName("Should save appointment and map back to domain")
    void shouldSaveAppointment() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        Appointment appointment = Appointment.create(
                new AppointmentId(appointmentId), new TenantId(tenantId), new BranchId(branchId),
                new CustomerId(customerId), new VehicleId(vehicleId), start, end, "Notes"
        );

        when(repository.save(any(AppointmentJpaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Appointment saved = adapter.save(appointment);

        assertNotNull(saved);
        assertEquals(appointmentId, saved.getId().getValue());
        assertEquals(tenantId, saved.getTenantId().getValue());
        assertEquals(branchId, saved.getBranchId().getValue());
        assertEquals("SCHEDULED", saved.getStatus().name());
        verify(repository).save(any(AppointmentJpaEntity.class));
    }

    @Test
    @DisplayName("Should find appointment by ID and tenantId")
    void shouldFindById() {
        Instant start = Instant.now().plusSeconds(3600);
        AppointmentJpaEntity entity = new AppointmentJpaEntity(
                appointmentId, tenantId, branchId, customerId, vehicleId,
                start, start.plusSeconds(3600), "CONFIRMED", "Notes", null, start, start
        );

        when(repository.findByIdAndTenantId(appointmentId, tenantId)).thenReturn(Optional.of(entity));

        Optional<Appointment> result = adapter.findById(new AppointmentId(appointmentId), new TenantId(tenantId));

        assertTrue(result.isPresent());
        assertEquals(appointmentId, result.get().getId().getValue());
        assertEquals(AppointmentStatus.CONFIRMED, result.get().getStatus());
    }

    @Test
    @DisplayName("Should find appointments with filters")
    void shouldFindWithFilters() {
        Instant start = Instant.now().plusSeconds(3600);
        AppointmentJpaEntity entity = new AppointmentJpaEntity(
                appointmentId, tenantId, branchId, customerId, vehicleId,
                start, start.plusSeconds(3600), "SCHEDULED", "Notes", null, start, start
        );

        when(repository.findWithFilters(eq(tenantId), eq(branchId), eq(customerId), eq(vehicleId), eq("SCHEDULED")))
                .thenReturn(List.of(entity));

        List<Appointment> results = adapter.findWithFilters(
                new TenantId(tenantId), new BranchId(branchId), new CustomerId(customerId),
                new VehicleId(vehicleId), AppointmentStatus.SCHEDULED
        );

        assertEquals(1, results.size());
        assertEquals(appointmentId, results.get(0).getId().getValue());
    }

    @Test
    @DisplayName("Should check overlapping appointment query")
    void shouldCheckOverlapping() {
        Instant start = Instant.now().plusSeconds(3600);
        Instant end = start.plusSeconds(3600);

        when(repository.hasOverlappingAppointment(eq(tenantId), eq(vehicleId), eq(start), eq(end), eq(appointmentId)))
                .thenReturn(true);

        boolean overlaps = adapter.hasOverlappingAppointment(
                new TenantId(tenantId), new VehicleId(vehicleId), start, end, new AppointmentId(appointmentId)
        );

        assertTrue(overlaps);
        verify(repository).hasOverlappingAppointment(tenantId, vehicleId, start, end, appointmentId);
    }
}
