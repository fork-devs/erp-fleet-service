package com.erp.fleet.application.service;

import com.erp.fleet.application.dto.result.StaffCareerHistoryResult;
import com.erp.fleet.application.usecase.GetStaffCareerHistoryUseCase;
import com.erp.fleet.domain.port.StaffCareerHistoryRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.EmployeeId;
import com.erp.fleet.domain.valueobject.TenantId;

import java.util.List;
import java.util.UUID;

public class StaffCareerHistoryService implements GetStaffCareerHistoryUseCase {

    private final StaffCareerHistoryRepositoryPort repository;
    private final UnitOfWork unitOfWork;

    public StaffCareerHistoryService(StaffCareerHistoryRepositoryPort repository, UnitOfWork unitOfWork) {
        this.repository = repository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public List<StaffCareerHistoryResult> getHistory(UUID tenantId, UUID branchId, UUID employeeId) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tId = new TenantId(tenantId);
            BranchId bId = new BranchId(branchId);
            EmployeeId eId = new EmployeeId(employeeId);

            return repository.findByEmployeeAndBranch(tId, bId, eId).stream()
                    .map(StaffCareerHistoryResult::from)
                    .toList();
        });
    }
}
