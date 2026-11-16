package com.erp.fleet.application.service;

import com.erp.fleet.domain.exception.AppointmentConflictException;
import com.erp.fleet.domain.model.AppointmentResources;
import com.erp.fleet.domain.model.EmployeeRegistration;
import com.erp.fleet.domain.model.EmployeeRegistrationStatus;
import com.erp.fleet.domain.model.EmployeeRole;
import com.erp.fleet.domain.port.AppointmentRepositoryPort;
import com.erp.fleet.domain.port.EmployeeRegistrationRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.port.WorkshopBayRepositoryPort;
import com.erp.fleet.domain.port.WorkshopBayRepositoryPort.Bay;
import com.erp.fleet.domain.valueobject.AppointmentId;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.EmployeeId;
import com.erp.fleet.domain.valueobject.TenantId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class WorkshopAppointmentResources {
    private final WorkshopBayRepositoryPort bays;
    private final EmployeeRegistrationRepositoryPort employees;
    private final AppointmentRepositoryPort appointments;
    private final UnitOfWork unitOfWork;

    public WorkshopAppointmentResources(WorkshopBayRepositoryPort bays, EmployeeRegistrationRepositoryPort employees,
                                        AppointmentRepositoryPort appointments, UnitOfWork unitOfWork) {
        this.bays = bays;
        this.employees = employees;
        this.appointments = appointments;
        this.unitOfWork = unitOfWork;
    }

    public record Technician(UUID id, String specialty) {}
    public record Options(List<Bay> bays, List<Technician> technicians) {}

    public Options list(UUID tenantId, UUID branchId) {
        return unitOfWork.callInTransaction(() -> new Options(
                bays.list(tenantId, branchId),
                employees.findByFilters(new TenantId(tenantId), new BranchId(branchId), null,
                                EmployeeRegistrationStatus.ACTIVE).stream()
                        .filter(WorkshopAppointmentResources::isTechnician)
                        .map(e -> new Technician(e.getEmployeeId().getValue(), e.getSpecialityName()))
                        .toList()));
    }

    public Bay saveBay(UUID tenantId, UUID branchId, int number, String name) {
        Bay bay = new Bay(number, name.strip());
        unitOfWork.runInTransaction(() -> bays.save(tenantId, branchId, bay));
        return bay;
    }

    public void reserve(TenantId tenantId, BranchId branchId, AppointmentResources selected,
                        Instant start, Instant end, AppointmentId excludeId) {
        appointments.lockSchedule(tenantId, "bay:" + branchId.getValue() + ":" + selected.bayNumber());
        appointments.lockSchedule(tenantId, "technician:" + selected.technicianId());
        if (!bays.exists(tenantId.getValue(), branchId.getValue(), selected.bayNumber())) {
            throw new AppointmentConflictException("La bahía no está registrada en la sede seleccionada");
        }
        boolean active = employees.findByFilters(tenantId, branchId, new EmployeeId(selected.technicianId()),
                        EmployeeRegistrationStatus.ACTIVE).stream()
                .anyMatch(WorkshopAppointmentResources::isTechnician);
        if (!active) {
            throw new AppointmentConflictException("El técnico no está activo en la sede seleccionada");
        }
        if (appointments.hasOverlappingResources(tenantId, branchId, selected, start, end, excludeId)) {
            throw new AppointmentConflictException("La bahía o el técnico ya tienen una cita en ese horario");
        }
    }

    private static boolean isTechnician(EmployeeRegistration employee) {
        return employee.getRole() == EmployeeRole.ROLE_EMPLOYEE;
    }
}
