package com.erp.fleet.infrastructure.health;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Comprobacion real de la base de datos.
 *
 * <p>No basta con decir que hay una conexion configurada: se abre una, se
 * ejecuta una consulta y se cierra. Un pool roto se distingue de un servicio
 * sano, y es justo lo que un orquestador necesita para decidir si le manda
 * trafico.
 */
@Component("fleetDatabase")
public class DatabaseHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseHealthIndicator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Health health() {
        try {
            Integer resultado = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (resultado != null && resultado == 1) {
                return Health.up().withDetail("comprobacion", "consulta ejecutada").build();
            }
            return Health.down().withDetail("comprobacion", "la consulta no devolvio lo esperado").build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
