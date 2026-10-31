package com.erp.fleet.interfaces.rest.controller;

import com.erp.fleet.application.dto.result.StaffCareerHistoryResult;
import com.erp.fleet.application.usecase.GetStaffCareerHistoryUseCase;
import com.erp.fleet.interfaces.rest.dto.response.StaffCareerHistoryResponse;
import com.erp.fleet.interfaces.rest.support.CurrentTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/branches/{branchId}/employees/{employeeId}/history")
@Validated
@Tag(name = "Staff Career History", description = "Chronological auditing of staff role, specialty, and salary changes")
public class StaffCareerHistoryController {

    private final GetStaffCareerHistoryUseCase getUseCase;

    public StaffCareerHistoryController(GetStaffCareerHistoryUseCase getUseCase) {
        this.getUseCase = getUseCase;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated() and (hasAnyRole('ADMIN', 'SUPERADMIN', 'OWNER', 'WORKSHOP_ADMIN', 'ADMIN_TALLER') or @multiTenancySecurityService.isAuthorizedForBranch(#branchId))")
    @Operation(summary = "Get employee career and salary revision history in branch")
    public ResponseEntity<List<StaffCareerHistoryResponse>> getHistory(
            @PathVariable UUID branchId,
            @PathVariable UUID employeeId
    ) {
        List<StaffCareerHistoryResult> results = getUseCase.getHistory(CurrentTenant.id(), branchId, employeeId);
        return ResponseEntity.ok(results.stream().map(StaffCareerHistoryResponse::from).toList());
    }
}
