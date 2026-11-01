package com.erp.fleet.application.usecase;

import com.erp.fleet.application.dto.result.StaffCareerHistoryResult;

import java.util.List;
import java.util.UUID;

public interface GetStaffCareerHistoryUseCase {
    List<StaffCareerHistoryResult> getHistory(UUID tenantId, UUID branchId, UUID employeeId);
}
