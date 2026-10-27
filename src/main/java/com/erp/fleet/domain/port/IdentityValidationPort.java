package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.IdentityValidationResult;

/**
 * Port to validate customer identity documents against the integration hub.
 */
public interface IdentityValidationPort {

    IdentityValidationResult validateDocument(String ruc);

    // Compatibility alias
    default IdentityValidationResult validarDocumento(String ruc) {
        return validateDocument(ruc);
    }
}
