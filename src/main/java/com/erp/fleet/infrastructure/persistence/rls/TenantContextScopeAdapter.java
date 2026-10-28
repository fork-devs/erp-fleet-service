package com.erp.fleet.infrastructure.persistence.rls;

import com.erp.fleet.domain.port.TenantContextPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adapter that sets and restores the tenant context around a block of code.
 */
@Component
public class TenantContextScopeAdapter implements TenantContextPort {

    @Override
    public void executeInContext(UUID tenantId, Runnable operation) {
        UUID previous = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenantId);
        try {
            operation.run();
        } finally {
            if (previous == null) {
                TenantContextHolder.clear();
            } else {
                TenantContextHolder.setTenantId(previous);
            }
        }
    }
}
