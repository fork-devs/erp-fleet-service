package com.erp.fleet.application.dto.command;

import java.util.UUID;

/**
 * Peticion de validacion de un vehiculo contra el hub.
 */
public record ValidarVehiculoCommand(UUID vehicleId, UUID tenantId) {
}
