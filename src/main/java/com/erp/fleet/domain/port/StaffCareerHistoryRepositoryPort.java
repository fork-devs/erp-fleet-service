package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.StaffCareerHistory;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.EmployeeId;
import com.erp.fleet.domain.valueobject.TenantId;

import java.util.List;

public interface StaffCareerHistoryRepositoryPort {
    StaffCareerHistory save(StaffCareerHistory history);

    List<StaffCareerHistory> findByEmployeeAndBranch(TenantId tenantId, BranchId branchId, EmployeeId employeeId);
}
