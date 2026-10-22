package com.erp.fleet.infrastructure.persistence.platform;

import com.erp.fleet.domain.model.RegistroPendiente;
import com.erp.fleet.domain.port.PendientesValidacionPort;
import com.erp.fleet.infrastructure.persistence.rls.PlatformScopeQueryExecutor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registros pendientes de revalidar, en alcance de plataforma.
 *
 * <p>Vive FUERA del paquete de adapters de persistencia a proposito: el
 * aspecto de Row-Level Security exige que toda operacion de ese paquete se
 * ejecute dentro de una transaccion y con contexto de taller, y esta consulta
 * es de alcance de plataforma por definicion: recorre los talleres y no
 * conoce todavia el de cada registro.
 *
 * <p>La consulta es de alcance de PLATAFORMA porque el barrido todavia no
 * conoce el taller de cada registro: con el rol de la aplicacion, que esta
 * sujeto a la politica de Row-Level Security, no veria nada.
 *
 * <p>El limite por taller se aplica en la propia consulta, con una funcion
 * de ventana: un taller con muchos registros pendientes no puede agotar la
 * cuota de validacion del hub, que se comparte entre talleres.
 */
@Component
public class PendientesValidacionAdapter implements PendientesValidacionPort {

    private static final String PENDIENTES_CLIENTES = """
            SELECT tenant_id, id FROM (
                SELECT tenant_id, id,
                       ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY next_validation_at) AS posicion
                  FROM customers
                 WHERE validation_status = 'PENDING'
                   AND next_validation_at IS NOT NULL
                   AND next_validation_at <= now()
            ) pendientes
            WHERE posicion <= ?
            """;

    private static final String PENDIENTES_VEHICULOS = """
            SELECT tenant_id, id FROM (
                SELECT tenant_id, id,
                       ROW_NUMBER() OVER (PARTITION BY tenant_id ORDER BY next_validation_at) AS posicion
                  FROM vehicles
                 WHERE validation_status = 'PENDING'
                   AND next_validation_at IS NOT NULL
                   AND next_validation_at <= now()
            ) pendientes
            WHERE posicion <= ?
            """;

    private final PlatformScopeQueryExecutor platformScope;

    public PendientesValidacionAdapter(PlatformScopeQueryExecutor platformScope) {
        this.platformScope = platformScope;
    }

    @Override
    public List<RegistroPendiente> clientesPendientes(int maxPorTaller) {
        return leer(platformScope.query(PENDIENTES_CLIENTES, maxPorTaller));
    }

    @Override
    public List<RegistroPendiente> vehiculosPendientes(int maxPorTaller) {
        return leer(platformScope.query(PENDIENTES_VEHICULOS, maxPorTaller));
    }

    private List<RegistroPendiente> leer(List<Map<String, Object>> filas) {
        List<RegistroPendiente> registros = new ArrayList<>(filas.size());
        for (Map<String, Object> fila : filas) {
            registros.add(new RegistroPendiente((UUID) fila.get("tenant_id"), (UUID) fila.get("id")));
        }
        return registros;
    }
}
