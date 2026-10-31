package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.EmployeeRegistration;
import com.erp.fleet.domain.model.EmployeeRegistrationStatus;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.EmployeeId;
import com.erp.fleet.domain.valueobject.EmployeeRegistrationId;
import com.erp.fleet.domain.valueobject.TenantId;

import java.util.List;
import java.util.Optional;

public interface EmployeeRegistrationRepositoryPort {
    EmployeeRegistration save(EmployeeRegistration registration);

    Optional<EmployeeRegistration> findById(EmployeeRegistrationId id, TenantId tenantId);

    List<EmployeeRegistration> findByFilters(TenantId tenantId, BranchId branchId, EmployeeId employeeId, EmployeeRegistrationStatus status);

    boolean existsActiveOrPendingRegistration(TenantId tenantId, BranchId branchId, EmployeeId employeeId);
}
