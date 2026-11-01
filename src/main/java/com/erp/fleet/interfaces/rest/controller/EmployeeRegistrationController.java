package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.dto.command.CreateEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.command.RequestJoinEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.command.UpdateEmployeeRegistrationCommand;
import com.erp.fleet.application.dto.result.EmployeeRegistrationResult;
import com.erp.fleet.application.usecase.GetEmployeeRegistrationsUseCase;
import com.erp.fleet.application.usecase.ManageEmployeeRegistrationUseCase;
import com.erp.fleet.infrastructure.security.MultiTenancySecurityService;
import com.erp.fleet.interfaces.rest.dto.request.CreateEmployeeRegistrationRequest;
import com.erp.fleet.interfaces.rest.dto.request.RejectEmployeeRegistrationRequest;
import com.erp.fleet.interfaces.rest.dto.request.RequestJoinEmployeeRegistrationRequest;
import com.erp.fleet.interfaces.rest.dto.request.UpdateEmployeeRegistrationRequest;
import com.erp.fleet.interfaces.rest.dto.response.EmployeeRegistrationResponse;
import com.erp.fleet.interfaces.rest.support.CurrentTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/employee-registrations")
@Validated
@Tag(name = "Employee Registrations", description = "Hierarchical onboarding and staff branch assignments")
public class EmployeeRegistrationController {

    private final ManageEmployeeRegistrationUseCase manageUseCase;
    private final GetEmployeeRegistrationsUseCase getUseCase;
    private final MultiTenancySecurityService securityService;

    public EmployeeRegistrationController(ManageEmployeeRegistrationUseCase manageUseCase,
                                          GetEmployeeRegistrationsUseCase getUseCase,
                                          MultiTenancySecurityService securityService) {
        this.manageUseCase = manageUseCase;
        this.getUseCase = getUseCase;
        this.securityService = securityService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Direct staff onboarding with hierarchy validation")
    public ResponseEntity<EmployeeRegistrationResponse> registerDirect(
            @Valid @RequestBody CreateEmployeeRegistrationRequest request
    ) {
        securityService.validateStaffManagement(request.role(), request.branchId());

        CreateEmployeeRegistrationCommand command = new CreateEmployeeRegistrationCommand(
                CurrentTenant.id(), request.branchId(), request.employeeId(),
                request.speciality(), request.specialityName(), request.salary(), request.role()
        );
        EmployeeRegistrationResult result = manageUseCase.registerDirect(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(EmployeeRegistrationResponse.from(result));
    }

    @PostMapping("/request-join")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Mechanic requests to join a workshop branch")
    public ResponseEntity<EmployeeRegistrationResponse> requestJoin(
            @Valid @RequestBody RequestJoinEmployeeRegistrationRequest request
    ) {
        RequestJoinEmployeeRegistrationCommand command = new RequestJoinEmployeeRegistrationCommand(
                CurrentTenant.id(), request.branchId(), request.employeeId(),
                request.speciality(), request.specialityName(), request.salary()
        );
        EmployeeRegistrationResult result = manageUseCase.requestJoin(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(EmployeeRegistrationResponse.from(result));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Approve pending employee join request")
    public ResponseEntity<EmployeeRegistrationResponse> approve(@PathVariable UUID id) {
        EmployeeRegistrationResult existing = getUseCase.getById(id, CurrentTenant.id());
        securityService.validateStaffManagement(existing.role(), existing.branchId());

        EmployeeRegistrationResult approved = manageUseCase.approve(id, CurrentTenant.id());
        return ResponseEntity.ok(EmployeeRegistrationResponse.from(approved));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Reject pending employee join request with reason")
    public ResponseEntity<EmployeeRegistrationResponse> reject(
            @PathVariable UUID id,
            @RequestBody(required = false) RejectEmployeeRegistrationRequest request
    ) {
        EmployeeRegistrationResult existing = getUseCase.getById(id, CurrentTenant.id());
        if (!securityService.isAuthorizedForBranch(existing.branchId())) {
            throw new AccessDeniedException("Access denied for branch: " + existing.branchId());
        }

        String reason = request != null ? request.reason() : null;
        EmployeeRegistrationResult rejected = manageUseCase.reject(id, CurrentTenant.id(), reason);
        return ResponseEntity.ok(EmployeeRegistrationResponse.from(rejected));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List employee registrations with filters")
    public ResponseEntity<List<EmployeeRegistrationResponse>> list(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID employeeId,
            @RequestParam(required = false) String status
    ) {
        if (branchId != null && !securityService.isAuthorizedForBranch(branchId)) {
            throw new AccessDeniedException("Access denied for branch: " + branchId);
        }

        List<EmployeeRegistrationResult> results = getUseCase.list(CurrentTenant.id(), branchId, employeeId, status);
        return ResponseEntity.ok(results.stream().map(EmployeeRegistrationResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get employee registration by ID")
    public ResponseEntity<EmployeeRegistrationResponse> getById(@PathVariable UUID id) {
        EmployeeRegistrationResult result = getUseCase.getById(id, CurrentTenant.id());
        if (!securityService.isAuthorizedForBranch(result.branchId())) {
            throw new AccessDeniedException("Access denied for branch: " + result.branchId());
        }
        return ResponseEntity.ok(EmployeeRegistrationResponse.from(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update employee registration specialty and salary")
    public ResponseEntity<EmployeeRegistrationResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateEmployeeRegistrationRequest request
    ) {
        EmployeeRegistrationResult existing = getUseCase.getById(id, CurrentTenant.id());
        if (!securityService.isAuthorizedForBranch(existing.branchId())) {
            throw new AccessDeniedException("Access denied for branch: " + existing.branchId());
        }

        UpdateEmployeeRegistrationCommand command = new UpdateEmployeeRegistrationCommand(
                id, CurrentTenant.id(), request.speciality(), request.specialityName(), request.salary()
        );
        EmployeeRegistrationResult updated = manageUseCase.update(command);
        return ResponseEntity.ok(EmployeeRegistrationResponse.from(updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Deactivate employee registration")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        EmployeeRegistrationResult existing = getUseCase.getById(id, CurrentTenant.id());
        if (!securityService.isAuthorizedForBranch(existing.branchId())) {
            throw new AccessDeniedException("Access denied for branch: " + existing.branchId());
        }

        manageUseCase.deactivate(id, CurrentTenant.id());
        return ResponseEntity.noContent().build();
    }
}
