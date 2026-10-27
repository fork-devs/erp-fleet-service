package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.PendingRecord;

import java.util.List;

/**
 * Port to query customer and vehicle records awaiting revalidation.
 */
public interface PendingValidationsPort {

    List<PendingRecord> pendingCustomers(int maxPerTenant);

    List<PendingRecord> pendingVehicles(int maxPerTenant);

    // Compatibility aliases
    default List<PendingRecord> clientesPendientes(int maxPorTaller) {
        return pendingCustomers(maxPorTaller);
    }

    default List<PendingRecord> vehiculosPendientes(int maxPorTaller) {
        return pendingVehicles(maxPorTaller);
    }
}
