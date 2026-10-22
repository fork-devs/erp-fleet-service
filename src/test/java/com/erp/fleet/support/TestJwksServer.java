package com.erp.fleet.support;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Emisor de credenciales de prueba: sirve un conjunto de claves publicas por
 * HTTP y firma tokens con la clave privada correspondiente.
 *
 * <p>El servicio de fleet valida el token contra el JWKS del servicio de
 * autenticacion. Para probar el aislamiento entre talleres hace falta un
 * token verificado de verdad, de modo que los tests levantan su propio emisor
 * en lugar de mockear la validacion.
 */
public final class TestJwksServer {

    private static final String KEY_ID = "test-auth-key-v1";

    private static HttpServer server;
    private static RSAKey rsaKey;
    private static String jwksUrl;

    private TestJwksServer() {
    }

    public static synchronized void start() {
        if (server != null) {
            return;
        }
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            rsaKey = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(KEY_ID)
                    .algorithm(JWSAlgorithm.RS256)
                    .build();

            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/.well-known/jwks.json", exchange -> {
                byte[] body = new JWKSet(rsaKey.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();

            jwksUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/.well-known/jwks.json";
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo arrancar el emisor de credenciales de prueba", e);
        }
    }

    public static synchronized String jwksUrl() {
        start();
        return jwksUrl;
    }

    /**
     * Firma un token de acceso para el taller indicado, con los roles dados.
     */
    public static String signToken(UUID tenantId, List<String> roles) {
        start();
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(UUID.randomUUID().toString())
                    .claim("tenant_id", tenantId.toString())
                    .claim("roles", roles)
                    .issuer("https://auth.test.erp-automotriz.com")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(900)))
                    .build();

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(KEY_ID).build(),
                    claims);
            jwt.sign(new RSASSASigner(rsaKey));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo firmar el token de prueba", e);
        }
    }
}
