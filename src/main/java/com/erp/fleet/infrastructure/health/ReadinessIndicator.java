package com.erp.fleet.infrastructure.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.stereotype.Component;

/**
 * Disponibilidad del servicio.
 *
 * <p>Comprueba de verdad las dos dependencias: la base de datos y la cache de
 * validaciones. No se limita a decir que estan configuradas: las dos se
 * consultan de verdad, de modo que un pool roto o una cache inaccesible se
 * distinguen de un servicio sano.
 *
 * <p>El servicio informa de NO disponible cuando una de las dos falla: es lo
 * que un orquestador necesita para dejar de mandarle trafico.
 */
@Component("fleetReadiness")
public class ReadinessIndicator implements HealthIndicator {

    private final DatabaseHealthIndicator database;
    private final CacheHealthIndicator cache;

    public ReadinessIndicator(DatabaseHealthIndicator database, CacheHealthIndicator cache) {
        this.database = database;
        this.cache = cache;
    }

    @Override
    public Health health() {
        Health saludBaseDeDatos = database.health();
        Health saludCache = cache.health();

        Health.Builder builder = (saludBaseDeDatos.getStatus() == Status.DOWN
                || saludCache.getStatus() == Status.DOWN)
                ? Health.down()
                : Health.up();

        return builder
                .withDetail("base_de_datos", saludBaseDeDatos.getStatus().getCode())
                .withDetail("cache", saludCache.getStatus().getCode())
                .build();
    }
}
