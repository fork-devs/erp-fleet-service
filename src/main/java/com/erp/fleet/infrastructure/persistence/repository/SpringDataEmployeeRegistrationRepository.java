package com.erp.fleet.infrastructure.persistence.repository;

import com.erp.fleet.infrastructure.persistence.entity.EmployeeRegistrationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataEmployeeRegistrationRepository extends JpaRepository<EmployeeRegistrationJpaEntity, UUID> {

    Optional<EmployeeRegistrationJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndBranchIdAndEmployeeIdAndStatusIn(
            UUID tenantId, UUID branchId, UUID employeeId, Collection<String> statuses
    );

    @Query("""
        SELECT er FROM EmployeeRegistrationJpaEntity er
        WHERE er.tenantId = :tenantId
          AND (:branchId IS NULL OR er.branchId = :branchId)
          AND (:employeeId IS NULL OR er.employeeId = :employeeId)
          AND (:status IS NULL OR er.status = :status)
        ORDER BY er.createdAt DESC
    """)
    List<EmployeeRegistrationJpaEntity> findWithFilters(
            @Param("tenantId") UUID tenantId,
            @Param("branchId") UUID branchId,
            @Param("employeeId") UUID employeeId,
            @Param("status") String status
    );
}
