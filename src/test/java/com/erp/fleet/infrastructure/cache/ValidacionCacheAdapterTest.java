package com.erp.fleet.infrastructure.cache;

import com.erp.fleet.domain.model.ResultadoValidacionIdentidad;
import com.erp.fleet.domain.model.ResultadoValidacionVehiculo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * La cache de validaciones, con su vigencia y su invalidacion.
 *
 * <p>El TTL es el del diseno: un RUC treinta dias y una placa siete. Con una
 * cache sin vigencia, una placa podria seguir valida para siempre, y un RUC
 * con una razon social que ya no es la del titular.
 */
class ValidacionCacheAdapterTest {

    private static final String CLAVE_RUC = "fleet:validacion:ruc:20601030013";
    private static final String CLAVE_PLACA = "fleet:validacion:placa:ABC-123";

    private final Map<String, String> almacen = new HashMap<>();
    private StringRedisTemplate redis;
    private ValidacionCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operaciones = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operaciones);
        when(operaciones.get(anyString())).thenAnswer(invocacion -> almacen.get(invocacion.getArgument(0)));
        org.mockito.Mockito.doAnswer(invocacion -> {
            almacen.put(invocacion.getArgument(0), invocacion.getArgument(1));
            return null;
        }).when(operaciones).set(anyString(), anyString(), any(Duration.class));
        when(redis.delete(anyString())).thenAnswer(invocacion -> {
            almacen.remove(invocacion.getArgument(0));
            return Boolean.TRUE;
        });

        adapter = new ValidacionCacheAdapter(redis, Duration.ofDays(30), Duration.ofDays(7));
    }

    @Test
    @DisplayName("Un RUC guardado se vuelve a leer")
    void unRucGuardadoSeVuelveALeer() {
        adapter.guardarRuc("20601030013",
                ResultadoValidacionIdentidad.valido("Talleres SAC", "ACTIVO", "HABIDO", "Av. Siempre Viva 742"));

        Optional<ResultadoValidacionIdentidad> resultado = adapter.leerRuc("20601030013");

        assertTrue(resultado.isPresent());
        assertTrue(resultado.get().esValido());
        assertEquals("Talleres SAC", resultado.get().getRazonSocial());
    }

    @Test
    @DisplayName("Un RUC ausente no inventa un resultado")
    void unRucAusenteNoInventaUnResultado() {
        assertTrue(adapter.leerRuc("99999999").isEmpty());
    }

    @Test
    @DisplayName("La vigencia del RUC es de treinta dias")
    void laVigenciaDelRucEsDeTreintaDias() {
        adapter.guardarRuc("20601030013", ResultadoValidacionIdentidad.valido("Talleres SAC", "ACTIVO", "HABIDO", null));

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operaciones = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operaciones);
        adapter.guardarRuc("20601030013", ResultadoValidacionIdentidad.valido("Talleres SAC", "ACTIVO", "HABIDO", null));

        verify(operaciones).set(eq(CLAVE_RUC), anyString(), eq(Duration.ofDays(30)));
    }

    @Test
    @DisplayName("La vigencia de la placa es de siete dias")
    void laVigenciaDeLaPlacaEsDeSieteDias() {
        adapter.guardarPlaca("ABC-123", ResultadoValidacionVehiculo.valido("8AJ123", "2ZRFE", "Toyota", "Corolla", "Blanco", "Gasolina"));

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operaciones = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operaciones);
        adapter.guardarPlaca("ABC-123", ResultadoValidacionVehiculo.valido("8AJ123", "2ZRFE", "Toyota", "Corolla", "Blanco", "Gasolina"));

        verify(operaciones).set(eq(CLAVE_PLACA), anyString(), eq(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("Un resultado nuevo del hub invalida la entrada local")
    void unResultadoNuevoDelHubInvalidaLaEntradaLocal() {
        adapter.guardarRuc("20601030013", ResultadoValidacionIdentidad.valido("Talleres SAC", "ACTIVO", "HABIDO", null));

        adapter.invalidarRuc("20601030013");

        assertTrue(adapter.leerRuc("20601030013").isEmpty());
        verify(redis).delete(CLAVE_RUC);
    }

    @Test
    @DisplayName("Una placa ausente no inventa un resultado")
    void unaPlacaAusenteNoInventaUnResultado() {
        assertTrue(adapter.leerPlaca("ZZZ-999").isEmpty());
        verify(redis, never()).delete(anyString());
    }
}
