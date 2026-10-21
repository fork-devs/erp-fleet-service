package com.erp.fleet.interfaces.rest.support;

import com.erp.fleet.domain.exception.TenantContextMissingException;
import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;

import java.util.UUID;

/**
 * Taller de la peticion en curso.
 *
 * <p>El contexto lo establece el filtro de autenticacion a partir del token
 * verificado. La cabecera {@code X-Tenant-ID} es una pista que el filtro
 * contrasta con el token: nunca decide.
 */
public final class CurrentTenant {

    private CurrentTenant() {
    }

    public static UUID id() {
        UUID tenantId = TenantContextHolder.getTenantId();
        if (tenantId == null) {
            throw new TenantContextMissingException(
                    "La peticion no tiene contexto de taller: el token verificado no lo declara.");
        }
        return tenantId;
    }
}
