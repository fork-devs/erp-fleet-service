package com.erp.fleet.infrastructure.persistence.mapper;

import com.erp.fleet.domain.model.CustomerRegistration;
import com.erp.fleet.domain.model.CustomerRegistrationStatus;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.CustomerRegistrationId;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.infrastructure.persistence.entity.CustomerRegistrationJpaEntity;

public final class CustomerRegistrationPersistenceMapper {

    private CustomerRegistrationPersistenceMapper() {}

    public static CustomerRegistrationJpaEntity toEntity(CustomerRegistration r) {
        return new CustomerRegistrationJpaEntity(
                r.getId().getValue(),
                r.getTenantId().getValue(),
                r.getBranchId().getValue(),
                r.getCustomerId().getValue(),
                r.getStatus().name(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    public static CustomerRegistration toDomain(CustomerRegistrationJpaEntity e) {
        return new CustomerRegistration(
                new CustomerRegistrationId(e.getId()),
                new TenantId(e.getTenantId()),
                new BranchId(e.getBranchId()),
                new CustomerId(e.getCustomerId()),
                CustomerRegistrationStatus.valueOf(e.getStatus()),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}
