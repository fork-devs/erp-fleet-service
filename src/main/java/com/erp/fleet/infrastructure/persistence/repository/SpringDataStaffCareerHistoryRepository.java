package com.erp.fleet.infrastructure.persistence.repository;

import com.erp.fleet.infrastructure.persistence.entity.StaffCareerHistoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SpringDataStaffCareerHistoryRepository extends JpaRepository<StaffCareerHistoryJpaEntity, UUID> {

    List<StaffCareerHistoryJpaEntity> findByTenantIdAndBranchIdAndEmployeeIdOrderByChangedAtAsc(
            UUID tenantId, UUID branchId, UUID employeeId
    );
}
