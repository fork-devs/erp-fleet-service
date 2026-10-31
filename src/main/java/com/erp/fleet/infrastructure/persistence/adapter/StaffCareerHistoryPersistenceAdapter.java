package com.erp.fleet.infrastructure.persistence.adapter;

import com.erp.fleet.domain.model.StaffCareerHistory;
import com.erp.fleet.domain.port.StaffCareerHistoryRepositoryPort;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.EmployeeId;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.infrastructure.persistence.mapper.StaffCareerHistoryPersistenceMapper;
import com.erp.fleet.infrastructure.persistence.repository.SpringDataStaffCareerHistoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StaffCareerHistoryPersistenceAdapter implements StaffCareerHistoryRepositoryPort {

    private final SpringDataStaffCareerHistoryRepository repo;

    public StaffCareerHistoryPersistenceAdapter(SpringDataStaffCareerHistoryRepository repo) {
        this.repo = repo;
    }

    @Override
    public StaffCareerHistory save(StaffCareerHistory history) {
        var entity = StaffCareerHistoryPersistenceMapper.toEntity(history);
        var saved = repo.save(entity);
        return StaffCareerHistoryPersistenceMapper.toDomain(saved);
    }

    @Override
    public List<StaffCareerHistory> findByEmployeeAndBranch(TenantId tenantId, BranchId branchId, EmployeeId employeeId) {
        return repo.findByTenantIdAndBranchIdAndEmployeeIdOrderByChangedAtAsc(
                tenantId.getValue(), branchId.getValue(), employeeId.getValue()
        ).stream().map(StaffCareerHistoryPersistenceMapper::toDomain).toList();
    }
}
