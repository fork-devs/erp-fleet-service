package com.erp.fleet.infrastructure.client;

import com.erp.fleet.domain.exception.TenantContextMissingException;
import com.erp.fleet.domain.exception.ValidationUnavailableException;
import com.erp.fleet.domain.model.IdentityValidationResult;
import com.erp.fleet.domain.model.VehicleValidationResult;
import com.erp.fleet.domain.port.IdentityValidationPort;
import com.erp.fleet.domain.port.VehicleValidationPort;
import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

/**
 * REST adapter communicating with the integration hub for identity and vehicle validation.
 */
public class HubValidationAdapter implements IdentityValidationPort, VehicleValidationPort {

    private static final Logger log = LoggerFactory.getLogger(HubValidationAdapter.class);

    private final RestClient restClient;
    private final String serviceKey;
    private final String serviceName;

    public HubValidationAdapter(RestClient restClient, String serviceKey, String serviceName) {
        this.restClient = restClient;
        this.serviceKey = serviceKey;
        this.serviceName = serviceName;
    }

    @Override
    public IdentityValidationResult validateDocument(String ruc) {
        UUID tenantId = currentTenant();
        try {
            RucResponse response = restClient.get()
                    .uri("/api/v1/integrations/validate/ruc/{ruc}", ruc)
                    .header("X-Internal-Service-Key", serviceKey)
                    .header("X-Internal-Service-Name", serviceName)
                    .header("X-Tenant-ID", tenantId.toString())
                    .retrieve()
                    .body(RucResponse.class);

            if (response == null) {
                throw new ValidationUnavailableException(
                        "Integration hub returned empty response for RUC " + ruc);
            }

            if (!response.isValid()) {
                return IdentityValidationResult.observed();
            }

            return IdentityValidationResult.valid(
                    response.legalName(),
                    response.taxStatus(),
                    response.taxCondition(),
                    response.fiscalAddress()
            );
        } catch (RestClientException e) {
            log.warn("Integration hub call failed for RUC {}: {}", ruc, e.getMessage());
            throw new ValidationUnavailableException(
                    "Failed to communicate with integration hub for RUC " + ruc, e);
        }
    }

    @Override
    public VehicleValidationResult validatePlate(String plate) {
        UUID tenantId = currentTenant();
        try {
            PlateResponse response = restClient.get()
                    .uri("/api/v1/integrations/validate/placa/{placa}", plate)
                    .header("X-Internal-Service-Key", serviceKey)
                    .header("X-Internal-Service-Name", serviceName)
                    .header("X-Tenant-ID", tenantId.toString())
                    .retrieve()
                    .body(PlateResponse.class);

            if (response == null) {
                throw new ValidationUnavailableException(
                        "Integration hub returned empty response for plate " + plate);
            }

            if (!response.isValid()) {
                return VehicleValidationResult.observed();
            }

            return VehicleValidationResult.valid(
                    response.vin(),
                    response.engineNumber(),
                    response.brand(),
                    response.model(),
                    response.color(),
                    response.fuelType()
            );
        } catch (RestClientException e) {
            log.warn("Integration hub call failed for plate {}: {}", plate, e.getMessage());
            throw new ValidationUnavailableException(
                    "Failed to communicate with integration hub for plate " + plate, e);
        }
    }

    private UUID currentTenant() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new TenantContextMissingException("Tenant context required for hub validation");
        }
        return tenantId;
    }
}
