package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.Customer;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

public interface CustomerRepositoryPort {
    Customer save(Customer customer);
    Optional<Customer> findById(CustomerId id, TenantId tenantId);
    List<Customer> findAllByTenantId(TenantId tenantId);
    boolean existsByDocNumber(String docNumber, TenantId tenantId);
}
