package com.erp.fleet.domain.port;

import java.util.UUID;

/**
 * Ejecuta una operacion en el contexto de un taller.
 *
 * <p>El contexto de taller es lo que hace que la politica de Row-Level
 * Security filtre por el taller correcto. Lo normal es que lo establezca el
 * filtro de autenticacion a partir del token, pero hay operaciones de
 * PLATAFORMA —el barrido de revalidacion— que recorren varios talleres y
 * tienen que establecer el contexto de cada registro por separado.
 *
 * <p>Es un puerto y no una clase concreta porque la capa de aplicacion no
 * puede depender de como se guarda el contexto.
 */
public interface TenantContextPort {

    /**
     * Ejecuta la operacion con el contexto del taller indicado, y lo restaura
     * al terminar.
     */
    void ejecutarEnContexto(UUID tenantId, Runnable operacion);
}
