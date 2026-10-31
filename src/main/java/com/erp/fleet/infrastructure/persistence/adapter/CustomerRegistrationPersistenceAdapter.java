package com.erp.fleet.infrastructure.persistence.adapter;

import com.erp.fleet.domain.model.CustomerRegistration;
import com.erp.fleet.domain.model.CustomerRegistrationStatus;
import com.erp.fleet.domain.port.CustomerRegistrationRepositoryPort;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.CustomerRegistrationId;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.infrastructure.persistence.mapper.CustomerRegistrationPersistenceMapper;
import com.erp.fleet.infrastructure.persistence.repository.SpringDataCustomerRegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CustomerRegistrationPersistenceAdapter implements CustomerRegistrationRepositoryPort {

    private final SpringDataCustomerRegistrationRepository repo;

    public CustomerRegistrationPersistenceAdapter(SpringDataCustomerRegistrationRepository repo) {
        this.repo = repo;
    }

    @Override
    public CustomerRegistration save(CustomerRegistration registration) {
        var entity = CustomerRegistrationPersistenceMapper.toEntity(registration);
        var saved = repo.save(entity);
        return CustomerRegistrationPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<CustomerRegistration> findById(CustomerRegistrationId id, TenantId tenantId) {
        return repo.findByIdAndTenantId(id.getValue(), tenantId.getValue())
                .map(CustomerRegistrationPersistenceMapper::toDomain);
    }

    @Override
    public List<CustomerRegistration> findByFilters(TenantId tenantId, BranchId branchId,
                                                    CustomerId customerId, CustomerRegistrationStatus status) {
        UUID branchUid = branchId != null ? branchId.getValue() : null;
        UUID customerUid = customerId != null ? customerId.getValue() : null;
        String statusStr = status != null ? status.name() : null;

        return repo.findWithFilters(tenantId.getValue(), branchUid, customerUid, statusStr).stream()
                .map(CustomerRegistrationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveRegistration(TenantId tenantId, BranchId branchId, CustomerId customerId) {
        return repo.existsByTenantIdAndBranchIdAndCustomerIdAndStatus(
                tenantId.getValue(), branchId.getValue(), customerId.getValue(), CustomerRegistrationStatus.ACTIVE.name()
        );
    }
}
