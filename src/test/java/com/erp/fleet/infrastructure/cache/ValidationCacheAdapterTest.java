package com.erp.fleet.infrastructure.cache;

import com.erp.fleet.domain.model.IdentityValidationResult;
import com.erp.fleet.domain.model.VehicleValidationResult;
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
 * Unit test for validation cache adapter verifying TTL and cache eviction semantics.
 */
class ValidationCacheAdapterTest {

    private static final String RUC_KEY = "fleet:validacion:ruc:20601030013";
    private static final String PLATE_KEY = "fleet:validacion:placa:ABC-123";

    private final Map<String, String> storage = new HashMap<>();
    private StringRedisTemplate redis;
    private ValidationCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        when(operations.get(anyString())).thenAnswer(invocation -> storage.get(invocation.getArgument(0)));
        org.mockito.Mockito.doAnswer(invocation -> {
            storage.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(operations).set(anyString(), anyString(), any(Duration.class));
        when(redis.delete(anyString())).thenAnswer(invocation -> {
            storage.remove(invocation.getArgument(0));
            return Boolean.TRUE;
        });

        adapter = new ValidationCacheAdapter(redis, Duration.ofDays(30), Duration.ofDays(7));
    }

    @Test
    @DisplayName("A saved RUC is read back from cache")
    void savedRucIsReadBack() {
        adapter.putRuc("20601030013",
                IdentityValidationResult.valid("Talleres SAC", "ACTIVO", "HABIDO", "Av. Siempre Viva 742"));

        Optional<IdentityValidationResult> result = adapter.getRuc("20601030013");

        assertTrue(result.isPresent());
        assertTrue(result.get().isValid());
        assertEquals("Talleres SAC", result.get().getLegalName());
    }

    @Test
    @DisplayName("An absent RUC returns empty optional")
    void absentRucReturnsEmpty() {
        assertTrue(adapter.getRuc("99999999").isEmpty());
    }

    @Test
    @DisplayName("RUC validity TTL is 30 days")
    void rucTtlIsThirtyDays() {
        adapter.putRuc("20601030013", IdentityValidationResult.valid("Talleres SAC", "ACTIVO", "HABIDO", null));

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        adapter.putRuc("20601030013", IdentityValidationResult.valid("Talleres SAC", "ACTIVO", "HABIDO", null));

        verify(operations).set(eq(RUC_KEY), anyString(), eq(Duration.ofDays(30)));
    }

    @Test
    @DisplayName("Vehicle plate validity TTL is 7 days")
    void plateTtlIsSevenDays() {
        adapter.putPlate("ABC-123", VehicleValidationResult.valid("8AJ123", "2ZRFE", "Toyota", "Corolla", "Blanco", "Gasolina"));

        @SuppressWarnings("unchecked")
        ValueOperations<String, String> operations = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(operations);
        adapter.putPlate("ABC-123", VehicleValidationResult.valid("8AJ123", "2ZRFE", "Toyota", "Corolla", "Blanco", "Gasolina"));

        verify(operations).set(eq(PLATE_KEY), anyString(), eq(Duration.ofDays(7)));
    }

    @Test
    @DisplayName("Invalidating a RUC deletes the cached entry")
    void invalidatingRucDeletesKey() {
        adapter.putRuc("20601030013", IdentityValidationResult.valid("Talleres SAC", "ACTIVO", "HABIDO", null));

        adapter.evictRuc("20601030013");

        assertTrue(adapter.getRuc("20601030013").isEmpty());
        verify(redis).delete(RUC_KEY);
    }

    @Test
    @DisplayName("An absent plate returns empty optional")
    void absentPlateReturnsEmpty() {
        assertTrue(adapter.getPlate("ZZZ-999").isEmpty());
        verify(redis, never()).delete(anyString());
    }
}
