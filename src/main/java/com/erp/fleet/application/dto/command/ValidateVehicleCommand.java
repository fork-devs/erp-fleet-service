package com.erp.fleet.application.dto.command;

import java.util.UUID;

/**
 * Request to validate a vehicle plate against the integration hub.
 *
 * @param vehicleId Identifier of the vehicle to validate.
 * @param tenantId  Tenant scope of the vehicle.
 */
public record ValidateVehicleCommand(UUID vehicleId, UUID tenantId) {
}
