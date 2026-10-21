package com.erp.fleet.domain.port;

import com.erp.fleet.domain.model.ResultadoValidacionIdentidad;
import com.erp.fleet.domain.model.ResultadoValidacionVehiculo;

import java.util.Optional;

/**
 * Cache de las validaciones correctas.
 *
 * <p>Es un puerto del dominio porque la cache es una aceleracion, no una
 * parte del negocio: la aplicacion necesita saber si tiene un resultado
 * guardado, y no COMO se guarda.
 *
 * <p>La cache es de fleet y no del hub porque la del hub es LOCAL a cada
 * instancia: con varias replicas, cada una consultaria al proveedor externo
 * por su cuenta y consumiria la cuota que el hub limita por taller.
 */
public interface ValidacionCachePort {

    Optional<ResultadoValidacionIdentidad> leerRuc(String ruc);

    void guardarRuc(String ruc, ResultadoValidacionIdentidad resultado);

    Optional<ResultadoValidacionVehiculo> leerPlaca(String placa);

    void guardarPlaca(String placa, ResultadoValidacionVehiculo resultado);

    /**
     * Invalida la entrada local: el hub ha devuelto un resultado nuevo y el
     * anterior ya no vale.
     */
    void invalidarRuc(String ruc);

    void invalidarPlaca(String placa);
}
