package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.result.VehicleResult;

import java.util.List;
import java.util.UUID;

public interface GetVehicleUseCase {
    VehicleResult getById(UUID vehicleId, UUID tenantId);
    List<VehicleResult> listAll(UUID tenantId);
    List<VehicleResult> listByCustomer(UUID customerId, UUID tenantId);
}
