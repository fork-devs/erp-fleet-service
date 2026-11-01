package com.erp.fleet.domain.model;

import com.erp.fleet.domain.exception.AppointmentConflictException;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.EmployeeId;
import com.erp.fleet.domain.valueobject.EmployeeRegistrationId;
import com.erp.fleet.domain.valueobject.TenantId;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class EmployeeRegistration implements Serializable {
    private final EmployeeRegistrationId id;
    private final TenantId tenantId;
    private final BranchId branchId;
    private final EmployeeId employeeId;
    private String speciality;
    private String specialityName;
    private BigDecimal salary;
    private EmployeeRole role;
    private EmployeeRegistrationStatus status;
    private String rejectionReason;
    private final Instant createdAt;
    private Instant approvedAt;
    private Instant updatedAt;

    public EmployeeRegistration(EmployeeRegistrationId id,
                                TenantId tenantId,
                                BranchId branchId,
                                EmployeeId employeeId,
                                String speciality,
                                String specialityName,
                                BigDecimal salary,
                                EmployeeRole role,
                                EmployeeRegistrationStatus status,
                                String rejectionReason,
                                Instant createdAt,
                                Instant approvedAt,
                                Instant updatedAt) {
        if (id == null || tenantId == null || branchId == null || employeeId == null) {
            throw new IllegalArgumentException("Required fields of EmployeeRegistration cannot be null");
        }
        if (speciality == null || speciality.trim().isEmpty()) {
            throw new IllegalArgumentException("Speciality cannot be null or empty");
        }
        if (specialityName == null || specialityName.trim().isEmpty()) {
            throw new IllegalArgumentException("Speciality name cannot be null or empty");
        }
        this.id = id;
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.employeeId = employeeId;
        this.speciality = speciality.trim();
        this.specialityName = specialityName.trim();
        this.salary = salary != null ? salary : BigDecimal.ZERO;
        this.role = role != null ? role : EmployeeRole.ROLE_EMPLOYEE;
        this.status = status != null ? status : EmployeeRegistrationStatus.ACTIVE;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.approvedAt = approvedAt;
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
    }

    public static EmployeeRegistration createDirect(TenantId tenantId,
                                                     BranchId branchId,
                                                     EmployeeId employeeId,
                                                     String speciality,
                                                     String specialityName,
                                                     BigDecimal salary,
                                                     EmployeeRole role) {
        Instant now = Instant.now();
        return new EmployeeRegistration(
                EmployeeRegistrationId.generate(),
                tenantId,
                branchId,
                employeeId,
                speciality,
                specialityName,
                salary,
                role != null ? role : EmployeeRole.ROLE_EMPLOYEE,
                EmployeeRegistrationStatus.ACTIVE,
                null,
                now,
                null,
                now
        );
    }

    public static EmployeeRegistration createRequestJoin(TenantId tenantId,
                                                         BranchId branchId,
                                                         EmployeeId employeeId,
                                                         String speciality,
                                                         String specialityName,
                                                         BigDecimal salary) {
        Instant now = Instant.now();
        return new EmployeeRegistration(
                EmployeeRegistrationId.generate(),
                tenantId,
                branchId,
                employeeId,
                speciality,
                specialityName,
                salary,
                EmployeeRole.ROLE_EMPLOYEE,
                EmployeeRegistrationStatus.PENDING_APPROVAL,
                null,
                now,
                null,
                now
        );
    }

    public void approve() {
        if (this.status != EmployeeRegistrationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only registrations in PENDING_APPROVAL status can be approved (current: " + this.status + ")");
        }
        this.status = EmployeeRegistrationStatus.ACTIVE;
        this.approvedAt = Instant.now();
        this.updatedAt = this.approvedAt;
    }

    public void reject(String reason) {
        if (this.status != EmployeeRegistrationStatus.PENDING_APPROVAL) {
            throw new IllegalStateException("Only registrations in PENDING_APPROVAL status can be rejected (current: " + this.status + ")");
        }
        this.status = EmployeeRegistrationStatus.REJECTED;
        this.rejectionReason = reason != null ? reason.trim() : null;
        this.updatedAt = Instant.now();
    }

    public void updateDetails(String speciality, String specialityName, BigDecimal salary) {
        if (speciality != null && !speciality.trim().isEmpty()) {
            this.speciality = speciality.trim();
        }
        if (specialityName != null && !specialityName.trim().isEmpty()) {
            this.specialityName = specialityName.trim();
        }
        if (salary != null) {
            this.salary = salary;
        }
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.status = EmployeeRegistrationStatus.INACTIVE;
        this.updatedAt = Instant.now();
    }

    public EmployeeRegistrationId getId() {
        return id;
    }

    public TenantId getTenantId() {
        return tenantId;
    }

    public BranchId getBranchId() {
        return branchId;
    }

    public EmployeeId getEmployeeId() {
        return employeeId;
    }

    public String getSpeciality() {
        return speciality;
    }

    public String getSpecialityName() {
        return specialityName;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public EmployeeRegistrationStatus getStatus() {
        return status;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeRegistration that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
