package com.erp.fleet.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base de los tests de integracion del servicio de fleet.
 *
 * <p>Arranca PostgreSQL real en proceso y conecta la aplicacion con el rol de
 * ejecucion {@code erp_fleet_app}, que no es propietario de las tablas. Las
 * migraciones de Flyway las ejecuta el propietario.
 */
@SpringBootTest
public abstract class AbstractPostgresIntegrationTest {

    @DynamicPropertySource
    static void registerPostgresProperties(DynamicPropertyRegistry registry) {
        EmbeddedPostgresSupport.start();

        registry.add("spring.datasource.url", EmbeddedPostgresSupport::jdbcUrl);
        registry.add("spring.datasource.username", EmbeddedPostgresSupport::appRole);
        registry.add("spring.datasource.password", EmbeddedPostgresSupport::appPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");

        registry.add("spring.flyway.url", EmbeddedPostgresSupport::jdbcUrl);
        registry.add("spring.flyway.user", EmbeddedPostgresSupport::ownerUser);
        registry.add("spring.flyway.password", EmbeddedPostgresSupport::ownerPassword);
        registry.add("spring.flyway.placeholders.appRolePassword", EmbeddedPostgresSupport::appPassword);
        registry.add("spring.flyway.placeholders.platformRolePassword",
                EmbeddedPostgresSupport::platformPassword);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }
}
