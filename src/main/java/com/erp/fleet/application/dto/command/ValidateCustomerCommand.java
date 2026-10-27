package com.erp.fleet.application.dto.command;

import java.util.UUID;

/**
 * Request to validate a customer against the integration hub.
 *
 * @param customerId Identifier of the customer to validate.
 * @param tenantId   Tenant scope of the customer.
 */
public record ValidateCustomerCommand(UUID customerId, UUID tenantId) {
}
