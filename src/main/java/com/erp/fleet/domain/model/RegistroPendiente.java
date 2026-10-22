package com.erp.fleet.domain.model;

import java.util.UUID;

/**
 * Registro pendiente de revalidacion.
 *
 * <p>El taller viaja con el registro porque la revalidacion se hace SIEMPRE
 * en el contexto de SU taller: el barrido puede recorrer todos los talleres,
 * pero cada escritura ocurre bajo la politica de aislamiento de su propio
 * taller.
 */
public final class RegistroPendiente {

    private final UUID tenantId;
    private final UUID id;

    public RegistroPendiente(UUID tenantId, UUID id) {
        this.tenantId = tenantId;
        this.id = id;
    }

    public UUID getTenantId() { return tenantId; }
    public UUID getId() { return id; }
}
