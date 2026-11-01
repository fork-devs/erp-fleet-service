package com.erp.fleet.application.service;

import com.erp.fleet.application.dto.command.ValidateCustomerCommand;
import com.erp.fleet.application.dto.command.ValidateVehicleCommand;
import com.erp.fleet.application.usecase.ValidateCustomerUseCase;
import com.erp.fleet.application.usecase.ValidateVehicleUseCase;
import com.erp.fleet.domain.model.PendingRecord;
import com.erp.fleet.domain.port.PendingValidationsPort;
import com.erp.fleet.domain.port.TenantContextPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Service orchestrating scheduled revalidation sweeps for pending entities.
 */
public class RevalidationService {

    private static final Logger log = LoggerFactory.getLogger(RevalidationService.class);

    private final PendingValidationsPort pendingValidations;
    private final TenantContextPort tenantContext;
    private final ValidateCustomerUseCase validateCustomer;
    private final ValidateVehicleUseCase validateVehicle;

    public RevalidationService(PendingValidationsPort pendingValidations,
                               TenantContextPort tenantContext,
                               ValidateCustomerUseCase validateCustomer,
                               ValidateVehicleUseCase validateVehicle) {
        this.pendingValidations = pendingValidations;
        this.tenantContext = tenantContext;
        this.validateCustomer = validateCustomer;
        this.validateVehicle = validateVehicle;
    }

    /**
     * Executes the revalidation sweep for pending customer and vehicle records whose backoff expired.
     *
     * @param maxPerTenant Maximum records to revalidate per tenant in one sweep.
     * @return Total number of revalidated records.
     */
    public int execute(int maxPerTenant) {
        int retried = 0;

        for (PendingRecord record : pendingValidations.pendingCustomers(maxPerTenant)) {
            retried += retryCustomer(record);
        }

        for (PendingRecord record : pendingValidations.pendingVehicles(maxPerTenant)) {
            retried += retryVehicle(record);
        }

        if (retried > 0) {
            log.info("Revalidation sweep: {} record(s) retried", retried);
        }
        return retried;
    }

    // Compatibility alias
    public int ejecutar(int maxPerTenant) {
        return execute(maxPerTenant);
    }

    private int retryCustomer(PendingRecord record) {
        UUID tenantId = record.getTenantId();
        UUID id = record.getId();
        try {
            tenantContext.executeInContext(tenantId, () ->
                    validateCustomer.validate(new ValidateCustomerCommand(id, tenantId)));
            return 1;
        } catch (Exception e) {
            log.warn("Failed to revalidate customer {}: {}", id, e.getMessage());
            return 0;
        }
    }

    private int retryVehicle(PendingRecord record) {
        UUID tenantId = record.getTenantId();
        UUID id = record.getId();
        try {
            tenantContext.executeInContext(tenantId, () ->
                    validateVehicle.validate(new ValidateVehicleCommand(id, tenantId)));
            return 1;
        } catch (Exception e) {
            log.warn("Failed to revalidate vehicle {}: {}", id, e.getMessage());
            return 0;
        }
    }
}
