package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.dto.command.CreateCustomerRegistrationCommand;
import com.erp.fleet.application.dto.command.UpdateCustomerRegistrationStatusCommand;
import com.erp.fleet.application.dto.result.CustomerRegistrationResult;
import com.erp.fleet.application.usecase.GetCustomerRegistrationsUseCase;
import com.erp.fleet.application.usecase.ManageCustomerRegistrationUseCase;
import com.erp.fleet.infrastructure.security.MultiTenancySecurityService;
import com.erp.fleet.interfaces.rest.dto.request.CreateCustomerRegistrationRequest;
import com.erp.fleet.interfaces.rest.dto.request.UpdateCustomerRegistrationRequest;
import com.erp.fleet.interfaces.rest.dto.response.CustomerRegistrationResponse;
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
@RequestMapping("/api/v1/customer-registrations")
@Validated
@Tag(name = "Customer Registrations", description = "Branch customer linking and lifecycle")
public class CustomerRegistrationController {

    private final ManageCustomerRegistrationUseCase manageUseCase;
    private final GetCustomerRegistrationsUseCase getUseCase;
    private final MultiTenancySecurityService securityService;

    public CustomerRegistrationController(ManageCustomerRegistrationUseCase manageUseCase,
                                          GetCustomerRegistrationsUseCase getUseCase,
                                          MultiTenancySecurityService securityService) {
        this.manageUseCase = manageUseCase;
        this.getUseCase = getUseCase;
        this.securityService = securityService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated() and (hasAnyRole('ADMIN', 'SUPERADMIN', 'OWNER', 'WORKSHOP_ADMIN', 'ADMIN_TALLER') or @multiTenancySecurityService.isAuthorizedForBranch(#request.branchId()))")
    @Operation(summary = "Register customer at branch")
    public ResponseEntity<CustomerRegistrationResponse> register(@Valid @RequestBody CreateCustomerRegistrationRequest request) {
        CreateCustomerRegistrationCommand command = new CreateCustomerRegistrationCommand(
                CurrentTenant.id(), request.branchId(), request.customerId()
        );
        CustomerRegistrationResult result = manageUseCase.register(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerRegistrationResponse.from(result));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "List customer registrations with filters")
    public ResponseEntity<List<CustomerRegistrationResponse>> list(
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) String status
    ) {
        if (branchId == null && customerId == null && (status == null || status.trim().isEmpty())) {
            throw new IllegalArgumentException("At least one filter (branchId, customerId, or status) is required");
        }
        if (branchId != null && !securityService.isAuthorizedForBranch(branchId)) {
            throw new AccessDeniedException("Access denied for branch: " + branchId);
        }

        List<CustomerRegistrationResult> results = getUseCase.list(CurrentTenant.id(), branchId, customerId, status);
        return ResponseEntity.ok(results.stream().map(CustomerRegistrationResponse::from).toList());
    }

    @GetMapping("/{registrationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get customer registration by ID")
    public ResponseEntity<CustomerRegistrationResponse> getById(@PathVariable UUID registrationId) {
        CustomerRegistrationResult result = getUseCase.getById(registrationId, CurrentTenant.id());
        if (!securityService.isAuthorizedForBranch(result.branchId())) {
            throw new AccessDeniedException("Access denied for branch: " + result.branchId());
        }
        return ResponseEntity.ok(CustomerRegistrationResponse.from(result));
    }

    @PutMapping("/{registrationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update customer registration status")
    public ResponseEntity<CustomerRegistrationResponse> updateStatus(
            @PathVariable UUID registrationId,
            @Valid @RequestBody UpdateCustomerRegistrationRequest request
    ) {
        CustomerRegistrationResult existing = getUseCase.getById(registrationId, CurrentTenant.id());
        if (!securityService.isAuthorizedForBranch(existing.branchId())) {
            throw new AccessDeniedException("Access denied for branch: " + existing.branchId());
        }

        UpdateCustomerRegistrationStatusCommand command = new UpdateCustomerRegistrationStatusCommand(
                registrationId, CurrentTenant.id(), request.status()
        );
        CustomerRegistrationResult updated = manageUseCase.updateStatus(command);
        return ResponseEntity.ok(CustomerRegistrationResponse.from(updated));
    }

    @DeleteMapping("/{registrationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Deactivate customer registration")
    public ResponseEntity<Void> deactivate(@PathVariable UUID registrationId) {
        CustomerRegistrationResult existing = getUseCase.getById(registrationId, CurrentTenant.id());
        if (!securityService.isAuthorizedForBranch(existing.branchId())) {
            throw new AccessDeniedException("Access denied for branch: " + existing.branchId());
        }

        manageUseCase.deactivate(registrationId, CurrentTenant.id());
        return ResponseEntity.noContent().build();
    }
}
