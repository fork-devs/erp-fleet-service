package com.erp.fleet.infrastructure.persistence.repository;

import com.erp.fleet.infrastructure.persistence.entity.CustomerRegistrationJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCustomerRegistrationRepository extends JpaRepository<CustomerRegistrationJpaEntity, UUID> {

    Optional<CustomerRegistrationJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);

    boolean existsByTenantIdAndBranchIdAndCustomerIdAndStatus(UUID tenantId, UUID branchId, UUID customerId, String status);

    @Query("""
        SELECT cr FROM CustomerRegistrationJpaEntity cr
        WHERE cr.tenantId = :tenantId
          AND (:branchId IS NULL OR cr.branchId = :branchId)
          AND (:customerId IS NULL OR cr.customerId = :customerId)
          AND (:status IS NULL OR cr.status = :status)
        ORDER BY cr.createdAt DESC
    """)
    List<CustomerRegistrationJpaEntity> findWithFilters(
            @Param("tenantId") UUID tenantId,
            @Param("branchId") UUID branchId,
            @Param("customerId") UUID customerId,
            @Param("status") String status
    );
}
