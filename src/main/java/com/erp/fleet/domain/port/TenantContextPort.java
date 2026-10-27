package com.erp.fleet.domain.port;

import java.util.UUID;

/**
 * Port to execute an operation within an explicit tenant context.
 */
public interface TenantContextPort {

    /**
     * Executes the operation within the context of the given tenant, restoring the previous context upon completion.
     *
     * @param tenantId  Target tenant identifier.
     * @param operation Runnable to execute.
     */
    void executeInContext(UUID tenantId, Runnable operation);

    // Compatibility alias
    default void ejecutarEnContexto(UUID tenantId, Runnable operation) {
        executeInContext(tenantId, operation);
    }
}
