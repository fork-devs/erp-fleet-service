package com.erp.fleet.domain.model;

import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.EmployeeId;
import com.erp.fleet.domain.valueobject.StaffCareerHistoryId;
import com.erp.fleet.domain.valueobject.TenantId;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public class StaffCareerHistory implements Serializable {
    private final StaffCareerHistoryId id;
    private final TenantId tenantId;
    private final BranchId branchId;
    private final EmployeeId employeeId;
    private final String speciality;
    private final String specialityName;
    private final BigDecimal salary;
    private final EmployeeRole role;
    private final EmployeeRegistrationStatus status;
    private final String changeReason;
    private final Instant changedAt;

    public StaffCareerHistory(StaffCareerHistoryId id,
                              TenantId tenantId,
                              BranchId branchId,
                              EmployeeId employeeId,
                              String speciality,
                              String specialityName,
                              BigDecimal salary,
                              EmployeeRole role,
                              EmployeeRegistrationStatus status,
                              String changeReason,
                              Instant changedAt) {
        if (id == null || tenantId == null || branchId == null || employeeId == null) {
            throw new IllegalArgumentException("Required fields of StaffCareerHistory cannot be null");
        }
        this.id = id;
        this.tenantId = tenantId;
        this.branchId = branchId;
        this.employeeId = employeeId;
        this.speciality = speciality != null ? speciality.trim() : "";
        this.specialityName = specialityName != null ? specialityName.trim() : "";
        this.salary = salary != null ? salary : BigDecimal.ZERO;
        this.role = role != null ? role : EmployeeRole.ROLE_EMPLOYEE;
        this.status = status != null ? status : EmployeeRegistrationStatus.ACTIVE;
        this.changeReason = changeReason;
        this.changedAt = changedAt != null ? changedAt : Instant.now();
    }

    public static StaffCareerHistory record(TenantId tenantId,
                                            BranchId branchId,
                                            EmployeeId employeeId,
                                            String speciality,
                                            String specialityName,
                                            BigDecimal salary,
                                            EmployeeRole role,
                                            EmployeeRegistrationStatus status,
                                            String changeReason) {
        return new StaffCareerHistory(
                StaffCareerHistoryId.generate(),
                tenantId,
                branchId,
                employeeId,
                speciality,
                specialityName,
                salary,
                role,
                status,
                changeReason,
                Instant.now()
        );
    }

    public StaffCareerHistoryId getId() {
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

    public String getChangeReason() {
        return changeReason;
    }

    public Instant getChangedAt() {
        return changedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StaffCareerHistory that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
