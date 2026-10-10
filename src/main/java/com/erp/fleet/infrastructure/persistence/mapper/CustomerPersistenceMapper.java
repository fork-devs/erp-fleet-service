package com.erp.fleet.infrastructure.persistence.mapper;

import com.erp.fleet.domain.model.Customer;
import com.erp.fleet.domain.model.CustomerStatus;
import com.erp.fleet.domain.model.ValidationStatus;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.DocNumber;
import com.erp.fleet.domain.valueobject.TenantId;
import com.erp.fleet.infrastructure.persistence.entity.CustomerJpaEntity;

public class CustomerPersistenceMapper {
    private CustomerPersistenceMapper() {}

    public static CustomerJpaEntity toEntity(Customer c) {
        return new CustomerJpaEntity(
                c.getId().getValue(), c.getTenantId().getValue(),
                c.getFullName(), c.getEmail(), c.getPhone(),
                c.getDocNumber().getDocType(), c.getDocNumber().getValue(),
                c.getValidationStatus().name(), c.getStatus().name(), c.getCreatedAt()
        );
    }

    public static Customer toDomain(CustomerJpaEntity e) {
        return new Customer(
                new CustomerId(e.getId()), new TenantId(e.getTenantId()),
                e.getFullName(), e.getEmail(), e.getPhone(),
                DocNumber.of(e.getDocNumber(), e.getDocType()),
                ValidationStatus.valueOf(e.getValidationStatus()),
                CustomerStatus.valueOf(e.getStatus()), e.getCreatedAt()
        );
    }
}
