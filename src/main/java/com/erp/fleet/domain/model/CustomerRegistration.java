package com.erp.fleet.domain.model;

import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.CustomerId;
import com.erp.fleet.domain.valueobject.CustomerRegistrationId;
import com.erp.fleet.domain.valueobject.TenantId;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

public class CustomerRegistration implements Serializable {
    private final CustomerRegistrationId id;
    private final TenantId tenantId;
    private final BranchId branchId;
    private final CustomerId customerId;
    private CustomerRegistrationStatus status;
    private final Instant createdAt;
    private Instant updatedAt;

    public CustomerRegistration(CustomerRegistrationId id,
                                TenantId tenantId,
                                BranchId branchId,
                                CustomerId customerId,
                                CustomerRegistrationStatus status,
                                Instant createdAt,
                                Instant updatedAt) {
        if (id == null || tenantId == null || branchId == null || customerId == null) {
            throw new IllegalArgumentException("Required fields of CustomerRegistration cannot be null");
        }
        this.id = id;
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.customerId = customerId;
        this.status = status != null ? status : CustomerRegistrationStatus.ACTIVE;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static CustomerRegistration create(TenantId tenantId, BranchId branchId, CustomerId customerId) {
        Instant now = Instant.now();
        return new CustomerRegistration(
                CustomerRegistrationId.generate(),
                tenantId,
                branchId,
                customerId,
                CustomerRegistrationStatus.ACTIVE,
                now,
                now
        );
    }

    public void activate() {
        this.status = CustomerRegistrationStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = CustomerRegistrationStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public CustomerRegistrationId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public BranchId getBranchId() {
        return branchId;
    }

    public CustomerId getCustomerId() {
        return customerId;
    }

    public CustomerRegistrationStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerRegistration that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
