package com.erp.fleet.application.service;

import com.erp.fleet.application.dto.command.CreateEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.command.RequestJoinEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.command.UpdateEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.result.EmployeeRegistrationResult;
import com.erp.fleet.application.usecase.GetEmployeeRegistrationsUseCase;
import com.erp.fleet.application.usecase.ManageEmployeeRegistrationUseCase;
import com.erp.fleet.domain.exception.ActiveEmployeeRegistrationAlreadyExistsException;
import com.erp.fleet.domain.exception.EmployeeRegistrationNotFoundException;
import com.erp.fleet.domain.model.EmployeeRegistration;
import com.erp.fleet.domain.model.EmployeeRegistrationStatus;
import com.erp.fleet.domain.model.EmployeeRole;
import com.erp.fleet.domain.model.StaffCareerHistory;
import com.erp.fleet.domain.port.EmployeeRegistrationRepositoryPort;
import com.erp.fleet.domain.port.StaffCareerHistoryRepositoryPort;
import com.erp.fleet.domain.port.UnitOfWork;
import com.erp.fleet.domain.valueobject.BranchId;
import com.erp.fleet.domain.valueobject.EmployeeId;
import com.erp.fleet.domain.valueobject.EmployeeRegistrationId;
import com.erp.fleet.domain.valueobject.TenantId;

import java.util.List;
import java.util.UUID;

public class EmployeeRegistrationService implements ManageEmployeeRegistrationUseCase, GetEmployeeRegistrationsUseCase {

    private final EmployeeRegistrationRepositoryPort repository;
    private final StaffCareerHistoryRepositoryPort careerHistoryRepository;
    private final UnitOfWork unitOfWork;

    public EmployeeRegistrationService(EmployeeRegistrationRepositoryPort repository,
                                     StaffCareerHistoryRepositoryPort careerHistoryRepository,
                                     UnitOfWork unitOfWork) {
        this.repository = repository;
        this.careerHistoryRepository = careerHistoryRepository;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public EmployeeRegistrationResult registerDirect(CreateEmployeeRegistrationCommand command) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(command.tenantId());
            BranchId branchId = new BranchId(command.branchId());
            EmployeeId employeeId = new EmployeeId(command.employeeId());

            if (repository.existsActiveOrPendingRegistration(tenantId, branchId, employeeId)) {
                throw new ActiveEmployeeRegistrationAlreadyExistsException(
                        employeeId.toString(), branchId.toString()
                );
            }

            EmployeeRole role = EmployeeRole.fromString(command.role());
            EmployeeRegistration registration = EmployeeRegistration.createDirect(
                    tenantId, branchId, employeeId, command.speciality(), command.specialityName(),
                    command.salary(), role
            );

            EmployeeRegistration saved = repository.save(registration);

            // Audit in staff career history
            StaffCareerHistory history = StaffCareerHistory.record(
                    tenantId, branchId, employeeId, saved.getSpeciality(), saved.getSpecialityName(),
                    saved.getSalary(), saved.getRole(), saved.getStatus(), "Direct staff onboarding"
            );
            careerHistoryRepository.save(history);

            return EmployeeRegistrationResult.from(saved);
        });
    }

    @Override
    public EmployeeRegistrationResult requestJoin(RequestJoinEmployeeRegistrationCommand command) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tenantId = new TenantId(command.tenantId());
            BranchId branchId = new BranchId(command.branchId());
            EmployeeId employeeId = new EmployeeId(command.employeeId());

            if (repository.existsActiveOrPendingRegistration(tenantId, branchId, employeeId)) {
                throw new ActiveEmployeeRegistrationAlreadyExistsException(
                        employeeId.toString(), branchId.toString()
                );
            }

            EmployeeRegistration registration = EmployeeRegistration.createRequestJoin(
                    tenantId, branchId, employeeId, command.speciality(), command.specialityName(),
                    command.salary()
            );

            EmployeeRegistration saved = repository.save(registration);
            return EmployeeRegistrationResult.from(saved);
        });
    }

    @Override
    public EmployeeRegistrationResult approve(UUID id, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tId = new TenantId(tenantId);
            EmployeeRegistrationId rId = new EmployeeRegistrationId(id);

            EmployeeRegistration registration = repository.findById(rId, tId)
                    .orElseThrow(() -> new EmployeeRegistrationNotFoundException(rId.toString()));

            registration.approve();
            EmployeeRegistration saved = repository.save(registration);

            StaffCareerHistory history = StaffCareerHistory.record(
                    tId, saved.getBranchId(), saved.getEmployeeId(), saved.getSpeciality(),
                    saved.getSpecialityName(), saved.getSalary(), saved.getRole(), saved.getStatus(),
                    "Staff join request approved"
            );
            careerHistoryRepository.save(history);

            return EmployeeRegistrationResult.from(saved);
        });
    }

    @Override
    public EmployeeRegistrationResult reject(UUID id, UUID tenantId, String reason) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tId = new TenantId(tenantId);
            EmployeeRegistrationId rId = new EmployeeRegistrationId(id);

            EmployeeRegistration registration = repository.findById(rId, tId)
                    .orElseThrow(() -> new EmployeeRegistrationNotFoundException(rId.toString()));

            registration.reject(reason);
            EmployeeRegistration saved = repository.save(registration);
            return EmployeeRegistrationResult.from(saved);
        });
    }

    @Override
    public EmployeeRegistrationResult update(UpdateEmployeeRegistrationCommand command) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tId = new TenantId(command.tenantId());
            EmployeeRegistrationId rId = new EmployeeRegistrationId(command.id());

            EmployeeRegistration registration = repository.findById(rId, tId)
                    .orElseThrow(() -> new EmployeeRegistrationNotFoundException(rId.toString()));

            registration.updateDetails(command.speciality(), command.specialityName(), command.salary());
            EmployeeRegistration saved = repository.save(registration);

            StaffCareerHistory history = StaffCareerHistory.record(
                    tId, saved.getBranchId(), saved.getEmployeeId(), saved.getSpeciality(),
                    saved.getSpecialityName(), saved.getSalary(), saved.getRole(), saved.getStatus(),
                    "Salary and technical specialty revision"
            );
            careerHistoryRepository.save(history);

            return EmployeeRegistrationResult.from(saved);
        });
    }

    @Override
    public void deactivate(UUID id, UUID tenantId) {
        unitOfWork.runInTransaction(() -> {
            TenantId tId = new TenantId(tenantId);
            EmployeeRegistrationId rId = new EmployeeRegistrationId(id);

            EmployeeRegistration registration = repository.findById(rId, tId)
                    .orElseThrow(() -> new EmployeeRegistrationNotFoundException(rId.toString()));

            registration.deactivate();
            EmployeeRegistration saved = repository.save(registration);

            StaffCareerHistory history = StaffCareerHistory.record(
                    tId, saved.getBranchId(), saved.getEmployeeId(), saved.getSpeciality(),
                    saved.getSpecialityName(), saved.getSalary(), saved.getRole(), saved.getStatus(),
                    "Staff deactivation"
            );
            careerHistoryRepository.save(history);
        });
    }

    @Override
    public EmployeeRegistrationResult getById(UUID id, UUID tenantId) {
        return unitOfWork.callInTransaction(() -> {
            EmployeeRegistration registration = repository.findById(new EmployeeRegistrationId(id), new TenantId(tenantId))
                    .orElseThrow(() -> new EmployeeRegistrationNotFoundException(id.toString()));
            return EmployeeRegistrationResult.from(registration);
        });
    }

    @Override
    public List<EmployeeRegistrationResult> list(UUID tenantId, UUID branchId, UUID employeeId, String status) {
        return unitOfWork.callInTransaction(() -> {
            TenantId tId = new TenantId(tenantId);
            BranchId bId = branchId != null ? new BranchId(branchId) : null;
            EmployeeId eId = employeeId != null ? new EmployeeId(employeeId) : null;
            EmployeeRegistrationStatus st = (status != null && !status.trim().isEmpty())
                    ? EmployeeRegistrationStatus.valueOf(status.trim().toUpperCase()) : null;

            return repository.findByFilters(tId, bId, eId, st).stream()
                    .map(EmployeeRegistrationResult::from)
                    .toList();
        });
    }
}
