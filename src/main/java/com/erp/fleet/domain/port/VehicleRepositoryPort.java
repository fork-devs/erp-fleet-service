package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.Vehicle;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.domain.valueobject.VehicleId;

import java.util.List;
import java.util.Optional;

public interface VehicleRepositoryPort {
    Vehicle save(Vehicle vehicle);
    Optional<Vehicle> findById(VehicleId id, TenantId tenantId);
    List<Vehicle> findAllByTenantId(TenantId tenantId);
    List<Vehicle> findByCustomerId(CustomerId customerId, TenantId tenantId);
    boolean existsByPlate(String plate, TenantId tenantId);
}
