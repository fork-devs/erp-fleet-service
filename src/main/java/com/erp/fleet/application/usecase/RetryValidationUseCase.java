package com.erp.fleet.application.usecase;

import java.util.UUID;

/**
 * Use case to retry a pending validation for a customer or vehicle.
 */
public interface RetryValidationUseCase {

    boolean retryCustomer(UUID customerId, UUID tenantId);

    boolean retryVehicle(UUID vehicleId, UUID tenantId);

    // Compatibility aliases
    default boolean reintentarCliente(UUID customerId, UUID tenantId) {
        return retryCustomer(customerId, tenantId);
    }

    default boolean reintentarVehiculo(UUID vehicleId, UUID tenantId) {
        return retryVehicle(vehicleId, tenantId);
    }
}
