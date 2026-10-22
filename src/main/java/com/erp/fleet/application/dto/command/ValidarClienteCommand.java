package com.erp.fleet.application.dto.command;

import java.util.UUID;

/**
 * Peticion de validacion de un cliente contra el hub.
 */
public record ValidarClienteCommand(UUID customerId, UUID tenantId) {
}
