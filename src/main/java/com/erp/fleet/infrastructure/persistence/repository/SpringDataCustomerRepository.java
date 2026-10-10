package com.erp.fleet.infrastructure.persistence.repository;

import com.erp.fleet.infrastructure.persistence.entity.CustomerJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataCustomerRepository extends JpaRepository<CustomerJpaEntity, UUID> {
    List<CustomerJpaEntity> findAllByTenantId(UUID tenantId);
    Optional<CustomerJpaEntity> findByIdAndTenantId(UUID id, UUID tenantId);
    boolean existsByDocNumberAndTenantId(String docNumber, UUID tenantId);
}
