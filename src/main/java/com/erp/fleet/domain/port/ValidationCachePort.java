package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.IdentityValidationResult;
import com.erp.fleet.domain.model.VehicleValidationResult;

import java.util.Optional;

/**
 * Port for distributed caching of successful entity validations.
 */
public interface ValidationCachePort {

    Optional<IdentityValidationResult> getRuc(String ruc);

    void putRuc(String ruc, IdentityValidationResult result);

    Optional<VehicleValidationResult> getPlate(String plate);

    void putPlate(String plate, VehicleValidationResult result);

    void evictRuc(String ruc);

    void evictPlate(String plate);

    // Compatibility aliases
    default Optional<IdentityValidationResult> leerRuc(String ruc) {
        return getRuc(ruc);
    }

    default void guardarRuc(String ruc, IdentityValidationResult result) {
        putRuc(ruc, result);
    }

    default Optional<VehicleValidationResult> leerPlaca(String plate) {
        return getPlate(plate);
    }

    default void guardarPlaca(String plate, VehicleValidationResult result) {
        putPlate(plate, result);
    }

    default void invalidarRuc(String ruc) {
        evictRuc(ruc);
    }

    default void invalidarPlaca(String plate) {
        evictPlate(plate);
    }
}
