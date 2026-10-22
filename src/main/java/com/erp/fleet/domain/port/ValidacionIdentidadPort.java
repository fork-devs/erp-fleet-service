package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.ResultadoValidacionIdentidad;

/**
 * Consulta al hub la validacion de un documento de identidad.
 *
 * <p>El puerto expresa UN resultado, no un booleano: el hub puede declarar el
 * documento valido o informar de una discrepancia, y son dos situaciones que
 * el taller tiene que ver de forma distinta. El fallo del hub NO es un
 * resultado: el adaptador lanza la excepcion, y el caso de uso deja el
 * registro pendiente y programa el reintento.
 */
public interface ValidacionIdentidadPort {

    ResultadoValidacionIdentidad validarDocumento(String ruc);
}
