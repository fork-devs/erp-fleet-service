package com.erp.fleet.infrastructure.persistence.adapter;

import com.erp.fleet.domain.model.EmployeeRegistration;
import com.erp.fleet.domain.model.EmployeeRegistrationStatus;
import com.erp.fleet.domain.port.EmployeeRegistrationRepositoryPort;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.EmployeeId;
import com.erp.fleet.domain.valueobject.EmployeeRegistrationId;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.infrastructure.persistence.mapper.EmployeeRegistrationPersistenceMapper;
import com.erp.fleet.infrastructure.persistence.repository.SpringDataEmployeeRegistrationRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class EmployeeRegistrationPersistenceAdapter implements EmployeeRegistrationRepositoryPort {

    private final SpringDataEmployeeRegistrationRepository repo;

    public EmployeeRegistrationPersistenceAdapter(SpringDataEmployeeRegistrationRepository repo) {
        this.repo = repo;
    }

    @Override
    public EmployeeRegistration save(EmployeeRegistration registration) {
        var entity = EmployeeRegistrationPersistenceMapper.toEntity(registration);
        var saved = repo.save(entity);
        return EmployeeRegistrationPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<EmployeeRegistration> findById(EmployeeRegistrationId id, TenantId tenantId) {
        return repo.findByIdAndTenantId(id.getValue(), tenantId.getValue())
                .map(EmployeeRegistrationPersistenceMapper::toDomain);
    }

    @Override
    public List<EmployeeRegistration> findByFilters(TenantId tenantId, BranchId branchId,
                                                    EmployeeId employeeId, EmployeeRegistrationStatus status) {
        UUID branchUid = branchId != null ? branchId.getValue() : null;
        UUID employeeUid = employeeId != null ? employeeId.getValue() : null;
        String statusStr = status != null ? status.name() : null;

        return repo.findWithFilters(tenantId.getValue(), branchUid, employeeUid, statusStr).stream()
                .map(EmployeeRegistrationPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveOrPendingRegistration(TenantId tenantId, BranchId branchId, EmployeeId employeeId) {
        return repo.existsByTenantIdAndBranchIdAndEmployeeIdAndStatusIn(
                tenantId.getValue(), branchId.getValue(), employeeId.getValue(),
                Set.of(EmployeeRegistrationStatus.ACTIVE.name(), EmployeeRegistrationStatus.PENDING_APPROVAL.name())
        );
    }
}
