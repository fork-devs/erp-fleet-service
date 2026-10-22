package com.erp.fleet.infrastructure.persistence.rls;

import com.erp.fleet.domain.port.TenantContextPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Ejecuta una operacion con el contexto de un taller establecido.
 *
 * <p>El contexto se establece con el mecanismo que usa el filtro de
 * autenticacion, y se restaura al terminar: el hilo se reutiliza, y un
 * contexto que quedara establecido operaria sobre el taller de la operacion
 * anterior.
 */
@Component
public class TenantContextScopeAdapter implements TenantContextPort {

    @Override
    public void ejecutarEnContexto(UUID tenantId, Runnable operacion) {
        UUID anterior = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(tenantId);
        try {
            operacion.run();
        } finally {
            if (anterior == null) {
                TenantContextHolder.clear();
            } else {
                TenantContextHolder.setTenantId(anterior);
            }
        }
    }
}
