package com.erp.fleet.infrastructure.persistence.repository;

import com.erp.fleet.infrastructure.persistence.entity.VehicleJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataVehicleRepository extends JpaRepository<VehicleJpaEntity, UUID> {
    List<VehicleJpaEntity> findAllByTenantId(UUID tenantId);
    Optional<VehicleJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    List<VehicleJpaEntity> findByCustomerIdAndTenantId(UUID customerId, UUID tenantId);
    boolean existsByPlateAndTenantId(String plate, UUID tenantId);
}
