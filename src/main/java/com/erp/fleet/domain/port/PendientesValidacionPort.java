package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.RegistroPendiente;

import java.util.List;

/**
 * Registros de clientes y vehiculos pendientes de revalidar.
 *
 * <p>El barrido recorre los talleres con registros pendientes: todavia no
 * conoce el taller de cada registro, de modo que la consulta es de alcance de
 * plataforma. La revalidacion, en cambio, se hace SIEMPRE en el contexto del
 * taller del registro.
 *
 * <p>El limite por taller es lo que evita que un taller con muchos registros
 * pendientes agote la cuota de validacion del hub, que se comparte entre
 * talleres.
 */
public interface PendientesValidacionPort {

    List<RegistroPendiente> clientesPendientes(int maxPorTaller);

    List<RegistroPendiente> vehiculosPendientes(int maxPorTaller);
}
