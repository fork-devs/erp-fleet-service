package com.erp.fleet.interfaces.rest.support;

import com.erp.fleet.infrastructure.persistence.rls.TenantContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * El contexto de taller no sobrevive a la peticion.
 *
 * <p>Los hilos de los contenedores de servlet se reutilizan: si el contexto
 * quedara establecido al terminar una peticion, la siguiente que reutilice el
 * mismo hilo operaria sobre el taller de la anterior.
 */
class TenantContextScopeTest {

    @AfterEach
    void limpiar() {
        TenantContextHolder.clear();
    }

    @Test
    void elContextoSeEstableceYSeRestauraAlCerrarElAmbito() {
        UUID taller = UUID.randomUUID();

        try (TenantContextScope scope = TenantContextScope.forTenant(taller)) {
            assertEquals(taller, TenantContextHolder.getTenantId());
        }

        assertNull(TenantContextHolder.getTenantId(),
                "El contexto debe quedar limpio al terminar la operacion");
    }

    @Test
    void losAmbitosAnidadosRestauranElContextoAnterior() {
        UUID tallerExterior = UUID.randomUUID();
        UUID tallerInterior = UUID.randomUUID();

        try (TenantContextScope exterior = TenantContextScope.forTenant(tallerExterior)) {
            try (TenantContextScope interior = TenantContextScope.forTenant(tallerInterior)) {
                assertEquals(tallerInterior, TenantContextHolder.getTenantId());
            }
            assertEquals(tallerExterior, TenantContextHolder.getTenantId(),
                    "Al cerrar el ambito interior debe volver el contexto exterior");
        }

        assertNull(TenantContextHolder.getTenantId());
    }
}
