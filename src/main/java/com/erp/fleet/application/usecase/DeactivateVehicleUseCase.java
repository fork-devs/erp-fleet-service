package com.erp.fleet.application.usecase;

import java.util.UUID;

public interface DeactivateVehicleUseCase {
    void deactivate(UUID vehicleId, UUID tenantId);
}
