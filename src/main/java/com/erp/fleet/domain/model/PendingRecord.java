package com.erp.fleet.domain.model;

import java.util.UUID;

/**
 * Record holding tenant and entity identifier awaiting automated validation.
 */
public final class PendingRecord {

    private final UUID tenantId;
    private final UUID id;

    public PendingRecord(UUID tenantId, UUID id) {
        this.tenantId = tenantId;
        this.id = id;
    }

    public UUID getTenantId() {
        return tenantId;
    }

    public UUID getId() {
        return id;
    }
}
