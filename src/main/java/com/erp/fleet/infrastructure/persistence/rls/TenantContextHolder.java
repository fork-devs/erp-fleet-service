package com.erp.fleet.infrastructure.persistence.rls;

import java.util.UUID;

/**
 * Contexto de taller de la peticion en curso.
 *
 * <p>Solo se establece desde el principal autenticado, y se limpia al terminar
 * la peticion. El taller lo decide el token verificado: la cabecera
 * {@code X-Tenant-ID} es una pista que se contrasta con el, nunca una
 * autoridad.
 *
 * <p>No existe ningun mecanismo para eludir el aislamiento.
 */
public final class TenantContextHolder {

    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void setTenantId(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static UUID getTenantId() {
        return CURRENT_TENANT.get();
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
