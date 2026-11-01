package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.CustomerRegistration;
import com.erp.fleet.domain.model.CustomerRegistrationStatus;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.CustomerRegistrationId;
import com.erp.fleet.domain.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

public interface CustomerRegistrationRepositoryPort {
    CustomerRegistration save(CustomerRegistration registration);

    Optional<CustomerRegistration> findById(CustomerRegistrationId id, TenantId tenantId);

    List<CustomerRegistration> findByFilters(TenantId tenantId, BranchId branchId, CustomerId customerId, CustomerRegistrationStatus status);

    boolean existsActiveRegistration(TenantId tenantId, BranchId branchId, CustomerId customerId);
}
