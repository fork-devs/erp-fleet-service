package com.erp.fleet.infrastructure.persistence.repository;

import com.erp.fleet.infrastructure.persistence.entity.AppointmentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataAppointmentRepository extends JpaRepository<AppointmentJpaEntity, UUID> {

    Optional<AppointmentJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    @Query("""
        SELECT COUNT(a) > 0 FROM AppointmentJpaEntity a
        WHERE a.tenantId = :tenantId
          AND a.vehicleId = :vehicleId
          AND a.status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS')
          AND (:excludeId IS NULL OR a.id != :excludeId)
          AND a.scheduledStart < :end
          AND a.scheduledEnd > :start
    """)
    boolean hasOverlappingAppointment(
            @Param("tenantId") UUID tenantId,
            @Param("vehicleId") UUID vehicleId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            @Param("excludeId") UUID excludeId
    );

    @Query("""
        SELECT a FROM AppointmentJpaEntity a
        WHERE a.tenantId = :tenantId
          AND (:branchId IS NULL OR a.branchId = :branchId)
          AND (:customerId IS NULL OR a.customerId = :customerId)
          AND (:vehicleId IS NULL OR a.vehicleId = :vehicleId)
          AND (:status IS NULL OR a.status = :status)
        ORDER BY a.scheduledStart ASC
    """)
    List<AppointmentJpaEntity> findWithFilters(
            @Param("tenantId") UUID tenantId,
            @Param("branchId") UUID branchId,
            @Param("customerId") UUID customerId,
            @Param("vehicleId") UUID vehicleId,
            @Param("status") String status
    );
}
