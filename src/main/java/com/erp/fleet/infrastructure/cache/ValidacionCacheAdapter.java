package com.erp.fleet.infrastructure.cache;

import com.erp.fleet.domain.model.ResultadoValidacionIdentidad;
import com.erp.fleet.domain.model.ResultadoValidacionVehiculo;
import com.erp.fleet.domain.port.ValidacionCachePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Cache de las validaciones correctas.
 *
 * <p>La cache es de fleet y no del hub porque la del hub es LOCAL a cada
 * instancia: con varias replicas, cada una consultaria a SUNAT y RENIEC por
 * su cuenta y consumiria la cuota que el hub limita por taller. Con la cache
 * compartida, el consumo del proveedor externo y de la cuota del hub cae a
 * una consulta por taller y por clave.
 *
 * <p>La vigencia es la del diseno: un RUC treinta dias y una placa siete. Un
 * resultado nuevo del hub SIEMPRE invalida la entrada local.
 */
@Component
public class ValidacionCacheAdapter implements ValidacionCachePort {

    private static final Logger log = LoggerFactory.getLogger(ValidacionCacheAdapter.class);

    private static final String PREFIJO_RUC = "fleet:validacion:ruc:";
    private static final String PREFIJO_PLACA = "fleet:validacion:placa:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration rucTtl;
    private final Duration placaTtl;

    public ValidacionCacheAdapter(StringRedisTemplate redis,
                                  @Value("${erp.cache.ruc-ttl:30d}") Duration rucTtl,
                                  @Value("${erp.cache.placa-ttl:7d}") Duration placaTtl) {
        this.redis = redis;
        // El serializador es asunto del adapter: los resultados que guarda no
        // tienen fechas, y la cache no depende de la configuracion de
        // Jackson de la aplicacion.
        this.objectMapper = new ObjectMapper();
        this.rucTtl = rucTtl;
        this.placaTtl = placaTtl;
    }

    @Override
    public Optional<ResultadoValidacionIdentidad> leerRuc(String ruc) {
        return leer(PREFIJO_RUC + ruc, ResultadoValidacionIdentidad.class);
    }

    @Override
    public void guardarRuc(String ruc, ResultadoValidacionIdentidad resultado) {
        guardar(PREFIJO_RUC + ruc, resultado, rucTtl);
    }

    @Override
    public Optional<ResultadoValidacionVehiculo> leerPlaca(String placa) {
        return leer(PREFIJO_PLACA + placa, ResultadoValidacionVehiculo.class);
    }

    @Override
    public void guardarPlaca(String placa, ResultadoValidacionVehiculo resultado) {
        guardar(PREFIJO_PLACA + placa, resultado, placaTtl);
    }

    /**
     * Invalida la entrada local: el hub ha devuelto un resultado nuevo, y el
     * anterior ya no vale.
     */
    @Override
    public void invalidarRuc(String ruc) {
        redis.delete(PREFIJO_RUC + ruc);
    }

    @Override
    public void invalidarPlaca(String placa) {
        redis.delete(PREFIJO_PLACA + placa);
    }

    private <T> Optional<T> leer(String clave, Class<T> tipo) {
        try {
            String valor = redis.opsForValue().get(clave);
            if (valor == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(valor, tipo));
        } catch (Exception e) {
            // La cache es una aceleracion, no una fuente de verdad: si no se
            // puede leer, se consulta al hub.
            log.warn("No se ha podido leer la cache de validacion {}: {}", clave, e.getMessage());
            return Optional.empty();
        }
    }

    private void guardar(String clave, Object valor, Duration ttl) {
        try {
            redis.opsForValue().set(clave, objectMapper.writeValueAsString(valor), ttl);
        } catch (Exception e) {
            log.warn("No se ha podido guardar la cache de validacion {}: {}", clave, e.getMessage());
        }
    }
}
