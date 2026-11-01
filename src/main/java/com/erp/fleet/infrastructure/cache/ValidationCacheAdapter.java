package com.erp.fleet.infrastructure.cache;

import com.erp.fleet.domain.model.IdentityValidationResult;
import com.erp.fleet.domain.model.VehicleValidationResult;
import com.erp.fleet.domain.port.ValidationCachePort;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

/**
 * Redis-based adapter for validation caching with configurable TTLs.
 */
@Component
public class ValidationCacheAdapter implements ValidationCachePort {

    private static final Logger log = LoggerFactory.getLogger(ValidationCacheAdapter.class);

    private static final String RUC_PREFIX = "fleet:validacion:ruc:";
    private static final String PLATE_PREFIX = "fleet:validacion:placa:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration rucTtl;
    private final Duration plateTtl;

    public ValidationCacheAdapter(StringRedisTemplate redis,
                                  @Value("${erp.cache.ruc-ttl:30d}") Duration rucTtl,
                                  @Value("${erp.cache.placa-ttl:7d}") Duration plateTtl) {
        this.redis = redis;
        this.objectMapper = new ObjectMapper();
        this.rucTtl = rucTtl;
        this.plateTtl = plateTtl;
    }

    @Override
    public Optional<IdentityValidationResult> getRuc(String ruc) {
        return read(RUC_PREFIX + ruc, IdentityValidationResult.class);
    }

    @Override
    public void putRuc(String ruc, IdentityValidationResult result) {
        save(RUC_PREFIX + ruc, result, rucTtl);
    }

    @Override
    public Optional<VehicleValidationResult> getPlate(String plate) {
        return read(PLATE_PREFIX + plate, VehicleValidationResult.class);
    }

    @Override
    public void putPlate(String plate, VehicleValidationResult result) {
        save(PLATE_PREFIX + plate, result, plateTtl);
    }

    @Override
    public void evictRuc(String ruc) {
        evict(RUC_PREFIX + ruc);
    }

    @Override
    public void evictPlate(String plate) {
        evict(PLATE_PREFIX + plate);
    }

    private <T> Optional<T> read(String key, Class<T> targetClass) {
        try {
            String json = redis.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(json, targetClass));
        } catch (Exception e) {
            log.warn("Could not read validation cache key {}: {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    private void save(String key, Object payload, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            redis.opsForValue().set(key, json, ttl);
        } catch (Exception e) {
            log.warn("Could not write validation cache key {}: {}", key, e.getMessage());
        }
    }

    private void evict(String key) {
        try {
            redis.delete(key);
        } catch (Exception e) {
            log.warn("Could not evict validation cache key {}: {}", key, e.getMessage());
        }
    }
}
