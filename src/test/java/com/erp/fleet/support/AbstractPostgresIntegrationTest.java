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

        // El servicio valida el token contra el JWKS del emisor de
        // credenciales. Los tests levantan su propio emisor, de modo que los
        // tokens que se usan estan realmente firmados.
        registry.add("erp.auth.jwks-url", TestJwksServer::jwksUrl);

        // El servicio llama al hub de integraciones para validar identidad y
        // vehiculo. Los tests levantan su propio hub, de modo que se comprueba
        // el cableado real y no una simulacion.
        // El barrido de revalidacion recorre los talleres con registros
        // pendientes: necesita el rol de plataforma, con su credencial
        // separada de la de la aplicacion.
        registry.add("erp.platform-role.user", EmbeddedPostgresSupport::platformRole);
        registry.add("erp.platform-role.password", EmbeddedPostgresSupport::platformPassword);

        registry.add("erp.integration-hub.url", TestHubServer::baseUrl);
        registry.add("erp.integration-hub.key", () -> "test_fleet_internal_key");
        registry.add("erp.integration-hub.service-name", () -> "erp-fleet-service");
    }
}
