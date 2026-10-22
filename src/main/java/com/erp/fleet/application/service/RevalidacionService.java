package com.erp.fleet.application.service;

import com.erp.fleet.application.usecase.ValidarClienteUseCase;
import com.erp.fleet.application.usecase.ValidarVehiculoUseCase;
import com.erp.fleet.application.dto.command.ValidarClienteCommand;
import com.erp.fleet.application.dto.command.ValidarVehiculoCommand;
import com.erp.fleet.domain.model.RegistroPendiente;
import com.erp.fleet.domain.port.PendientesValidacionPort;
import com.erp.fleet.domain.port.TenantContextPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

/**
 * Barrido de revalidacion de los registros pendientes.
 *
 * <p>El recorrido de los talleres es de alcance de PLATAFORMA: el barrido
 * todavia no conoce el taller de cada registro, y la consulta con el rol de
 * la aplicacion no lo veria. La revalidacion, en cambio, se hace SIEMPRE en el
 * contexto del taller del registro, con el rol de la aplicacion: el barrido
 * puede recorrer todos los talleres, pero cada escritura ocurre bajo la
 * politica de aislamiento de SU taller.
 *
 * <p>El limite por taller es lo que evita que un taller con muchos registros
 * pendientes agote la cuota de validacion del hub, que se comparte entre
 * talleres.
 */
public class RevalidacionService {

    private static final Logger log = LoggerFactory.getLogger(RevalidacionService.class);

    private final PendientesValidacionPort pendientes;
    private final TenantContextPort tenantContext;
    private final ValidarClienteUseCase validarCliente;
    private final ValidarVehiculoUseCase validarVehiculo;

    public RevalidacionService(PendientesValidacionPort pendientes,
                               TenantContextPort tenantContext,
                               ValidarClienteUseCase validarCliente,
                               ValidarVehiculoUseCase validarVehiculo) {
        this.pendientes = pendientes;
        this.tenantContext = tenantContext;
        this.validarCliente = validarCliente;
        this.validarVehiculo = validarVehiculo;
    }

    /**
     * Revalida los registros pendientes que ya han vencido su espera.
     *
     * @return el numero de registros reintentados.
     */
    public int ejecutar(int maxPorTaller) {
        int reintentados = 0;

        for (RegistroPendiente registro : pendientes.clientesPendientes(maxPorTaller)) {
            reintentados += reintentarCliente(registro);
        }

        for (RegistroPendiente registro : pendientes.vehiculosPendientes(maxPorTaller)) {
            reintentados += reintentarVehiculo(registro);
        }

        if (reintentados > 0) {
            log.info("Barrido de revalidacion: {} registro(s) reintentados", reintentados);
        }
        return reintentados;
    }

    private int reintentarCliente(RegistroPendiente registro) {
        UUID tenantId = registro.getTenantId();
        UUID id = registro.getId();
        try {
            // La revalidacion se hace SIEMPRE en el contexto de SU taller:
            // el barrido puede recorrer todos, pero cada escritura ocurre
            // bajo la politica de aislamiento del registro.
            tenantContext.ejecutarEnContexto(tenantId, () ->
                    validarCliente.validar(new ValidarClienteCommand(id, tenantId)));
            return 1;
        } catch (Exception e) {
            log.warn("No se ha podido revalidar el cliente {}: {}", id, e.getMessage());
            return 0;
        }
    }

    private int reintentarVehiculo(RegistroPendiente registro) {
        UUID tenantId = registro.getTenantId();
        UUID id = registro.getId();
        try {
            tenantContext.ejecutarEnContexto(tenantId, () ->
                    validarVehiculo.validar(new ValidarVehiculoCommand(id, tenantId)));
            return 1;
        } catch (Exception e) {
            log.warn("No se ha podido revalidar el vehiculo {}: {}", id, e.getMessage());
            return 0;
        }
    }
}
