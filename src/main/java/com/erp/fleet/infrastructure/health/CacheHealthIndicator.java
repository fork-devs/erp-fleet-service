package com.erp.fleet.infrastructure.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Comprobacion real de la cache de validaciones.
 *
 * <p>La cache no es imprescindible para operar: sin ella, la validacion se
 * consulta al hub de cada vez. Por eso su fallo degrada pero no tumba, y se
 * informa aparte para que se pueda ver.
 */
@Component("fleetCache")
public class CacheHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate redis;

    public CacheHealthIndicator(StringRedisTemplate redis) {
        this.redis = redis;
    }

    @Override
    public Health health() {
        try {
            String valor = redis.opsForValue().get("fleet:salud");
            return Health.up().withDetail("comprobacion", "cache accesible").build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
