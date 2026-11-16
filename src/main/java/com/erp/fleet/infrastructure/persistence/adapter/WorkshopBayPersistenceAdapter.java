package com.erp.fleet.infrastructure.persistence.adapter;

import com.erp.fleet.domain.port.WorkshopBayRepositoryPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

@Repository
public class WorkshopBayPersistenceAdapter implements WorkshopBayRepositoryPort {
    private final JdbcClient jdbc;

    public WorkshopBayPersistenceAdapter(DataSource dataSource) {
        jdbc = JdbcClient.create(dataSource);
    }

    @Override
    public List<Bay> list(UUID tenantId, UUID branchId) {
        return jdbc.sql("""
                SELECT number, name FROM workshop_bays
                WHERE tenant_id = ? AND branch_id = ? ORDER BY number
                """).params(tenantId, branchId).query(Bay.class).list();
    }

    @Override
    public boolean exists(UUID tenantId, UUID branchId, int number) {
        return jdbc.sql("""
                SELECT EXISTS (SELECT 1 FROM workshop_bays WHERE tenant_id = ? AND branch_id = ? AND number = ?)
                """).params(tenantId, branchId, number).query(Boolean.class).single();
    }

    @Override
    public void save(UUID tenantId, UUID branchId, Bay bay) {
        jdbc.sql("""
                INSERT INTO workshop_bays (tenant_id, branch_id, number, name) VALUES (?, ?, ?, ?)
                ON CONFLICT (tenant_id, branch_id, number) DO UPDATE SET name = excluded.name
                """).params(tenantId, branchId, bay.number(), bay.name()).update();
    }
}
