package com.erp.fleet.infrastructure.persistence.platform;

import com.erp.fleet.domain.model.PendingRecord;
import com.erp.fleet.domain.port.PendingValidationsPort;
import com.erp.fleet.infrastructure.persistence.rls.PlatformScopeQueryExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Platform-scoped adapter retrieving customer and vehicle records pending revalidation.
 */
@Component
public class PendingValidationsAdapter implements PendingValidationsPort {

    private static final String PENDING_CUSTOMERS_SQL = """
            SELECT tenant_id, id FROM (
                SELECT tenant_id, id,
                       ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY next_validation_at) AS ranking
                  FROM customers
                 WHERE validation_status = 'PENDING'
                   AND next_validation_at IS NOT NULL
                   AND next_validation_at <= now()
            ) pending
            WHERE ranking <= ?
            """;

    private static final String PENDING_VEHICLES_SQL = """
            SELECT tenant_id, id FROM (
                SELECT tenant_id, id,
                       ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY next_validation_at) AS ranking
                  FROM vehicles
                 WHERE validation_status = 'PENDING'
                   AND next_validation_at IS NOT NULL
                   AND next_validation_at <= now()
            ) pending
            WHERE ranking <= ?
            """;

    private final PlatformScopeQueryExecutor platformScope;

    public PendingValidationsAdapter(PlatformScopeQueryExecutor platformScope) {
        this.platformScope = platformScope;
    }

    @Override
    public List<PendingRecord> pendingCustomers(int maxPerTenant) {
        return mapRows(platformScope.query(PENDING_CUSTOMERS_SQL, maxPerTenant));
    }

    @Override
    public List<PendingRecord> pendingVehicles(int maxPerTenant) {
        return mapRows(platformScope.query(PENDING_VEHICLES_SQL, maxPerTenant));
    }

    private List<PendingRecord> mapRows(List<Map<String, Object>> rows) {
        List<PendingRecord> records = new ArrayList<>(rows.size());
        for (Map<String, Object> row : rows) {
            records.add(new PendingRecord((UUID) row.get("tenant_id"), (UUID) row.get("id")));
        }
        return records;
    }
}
