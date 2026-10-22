package com.erp.fleet.infrastructure.persistence.adapter;

import com.erp.fleet.domain.model.Customer;
import com.erp.fleet.domain.port.CustomerRepositoryPort;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.infrastructure.persistence.mapper.CustomerPersistenceMapper;
import com.erp.fleet.infrastructure.persistence.repository.SpringDataCustomerRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class CustomerPersistenceAdapter implements CustomerRepositoryPort {
    private final SpringDataCustomerRepository repo;

    public CustomerPersistenceAdapter(SpringDataCustomerRepository repo) { this.repo = repo; }

    @Override
    public Customer save(Customer customer) {
        return CustomerPersistenceMapper.toDomain(repo.save(CustomerPersistenceMapper.toEntity(customer)));
    }

    @Override
    public Optional<Customer> findById(CustomerId id, TenantId tenantId) {
        return repo.findByIdAndTenantId(id.getValue(), tenantId.getValue())
                .map(CustomerPersistenceMapper::toDomain);
    }

    @Override
    public List<Customer> findAllByTenantId(TenantId tenantId) {
        return repo.findAllByTenantId(tenantId.getValue()).stream()
                .map(CustomerPersistenceMapper::toDomain).collect(Collectors.toList());
    }

    @Override
    public boolean existsByDocNumber(String docNumber, TenantId tenantId) {
        return repo.existsByDocNumberAndTenantId(docNumber, tenantId.getValue());
    }
}
