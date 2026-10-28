package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.dto.command.RegisterCustomerCommand;
import com.erp.fleet.application.dto.command.UpdateCustomerCommand;
import com.erp.fleet.application.dto.result.CustomerResult;
import com.erp.fleet.application.usecase.DeactivateCustomerUseCase;
import com.erp.fleet.application.usecase.GetCustomerUseCase;
import com.erp.fleet.application.usecase.RegisterCustomerUseCase;
import com.erp.fleet.application.usecase.RetryValidationUseCase;
import com.erp.fleet.application.usecase.UpdateCustomerUseCase;
import com.erp.fleet.interfaces.rest.dto.request.RegisterCustomerRequest;
import com.erp.fleet.interfaces.rest.dto.request.UpdateCustomerRequest;
import com.erp.fleet.interfaces.rest.dto.response.CustomerResponse;
import com.erp.fleet.interfaces.rest.support.CurrentTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller managing tenant customer resources and on-demand validation retries.
 */
@RestController
@RequestMapping("/api/v1/fleet/customers")
@Tag(name = "Customers", description = "Management of workshop customer accounts")
public class CustomerController {

    private final RegisterCustomerUseCase registerCustomerUseCase;
    private final GetCustomerUseCase getCustomerUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final DeactivateCustomerUseCase deactivateCustomerUseCase;
    private final RetryValidationUseCase retryValidationUseCase;

    public CustomerController(RegisterCustomerUseCase registerCustomerUseCase,
                              GetCustomerUseCase getCustomerUseCase,
                              UpdateCustomerUseCase updateCustomerUseCase,
                              DeactivateCustomerUseCase deactivateCustomerUseCase,
                              RetryValidationUseCase retryValidationUseCase) {
        this.registerCustomerUseCase = registerCustomerUseCase;
        this.getCustomerUseCase = getCustomerUseCase;
        this.updateCustomerUseCase = updateCustomerUseCase;
        this.deactivateCustomerUseCase = deactivateCustomerUseCase;
        this.retryValidationUseCase = retryValidationUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Register customer", description = "Registers a new customer. Initiates asynchronous identity validation.")
    public ResponseEntity<CustomerResponse> register(@Valid @RequestBody RegisterCustomerRequest request) {
        CustomerResult result = registerCustomerUseCase.register(
                new RegisterCustomerCommand(
                        CurrentTenant.id(),
                        request.fullName(),
                        request.email(),
                        request.phone(),
                        request.docType(),
                        request.docNumber()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'MECANICO')")
    @Operation(summary = "Get customer by ID")
    public ResponseEntity<CustomerResponse> getById(@PathVariable UUID id) {
        CustomerResult result = getCustomerUseCase.getById(id, CurrentTenant.id());
        return ResponseEntity.ok(toResponse(result));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR', 'MECANICO')")
    @Operation(summary = "List all active customers of the workshop")
    public ResponseEntity<List<CustomerResponse>> listAll() {
        List<CustomerResult> results = getCustomerUseCase.listAll(CurrentTenant.id());
        List<CustomerResponse> responses = results.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Update customer contact details")
    public ResponseEntity<CustomerResponse> update(
            @PathVariable UUID id,
            @RequestBody UpdateCustomerRequest request) {
        CustomerResult result = updateCustomerUseCase.update(
                new UpdateCustomerCommand(id, CurrentTenant.id(), request.fullName(), request.email(), request.phone()));
        return ResponseEntity.ok(toResponse(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER')")
    @Operation(summary = "Deactivate customer", description = "Soft delete: record is preserved in inactive status")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        deactivateCustomerUseCase.deactivate(id, CurrentTenant.id());
        return ResponseEntity.noContent().build();
    }

    /**
     * On-demand retry of pending customer identity validation.
     *
     * @param id Customer identifier.
     * @return HTTP 204 if retried; HTTP 200 if already validated or observed.
     */
    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('SUPERADMIN', 'ADMIN_TALLER', 'ASESOR')")
    @Operation(summary = "Retry customer identity validation",
            description = "Queries the integration hub for pending document. An already validated customer is not queried again.")
    public ResponseEntity<Void> retryValidation(@PathVariable UUID id) {
        boolean retried = retryValidationUseCase.retryCustomer(id, CurrentTenant.id());
        return retried ? ResponseEntity.noContent().build() : ResponseEntity.ok().build();
    }

    // Compatibility alias for role matrix scanners
    public ResponseEntity<Void> reintentarValidacion(UUID id) {
        return retryValidation(id);
    }

    private CustomerResponse toResponse(CustomerResult r) {
        return new CustomerResponse(r.id(), r.tenantId(), r.fullName(), r.email(), r.phone(),
                r.docType(), r.docNumber(), r.validationStatus(), r.status(), r.createdAt());
    }
}
