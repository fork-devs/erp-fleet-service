package com.erp.fleet.domain.port;

import java.util.List;
import java.util.UUID;

public interface WorkshopBayRepositoryPort {
    record Bay(int number, String name) {}

    List<Bay> list(UUID tenantId, UUID branchId);
    boolean exists(UUID tenantId, UUID branchId, int number);
    void save(UUID tenantId, UUID branchId, Bay bay);
}
