package com.erp.fleet.support;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Hub de integraciones de prueba.
 *
 * <p>El servicio de fleet llama al hub por HTTPS para validar la identidad y
 * el vehiculo. Los tests levantan su propio hub, de modo que se comprueba el
 * cableado real —cabeceras, rutas y codigos— y no una simulacion.
 */
public final class TestHubServer {

    private static HttpServer server;
    private static String baseUrl;

    /** Numero de llamadas recibidas, para comprobar que la cache evita la segunda. */
    private static final AtomicInteger llamadas = new AtomicInteger();

    /** Cuando es true, el hub responde como caido. */
    private static volatile boolean caido = false;

    /** Cuando es true, el hub responde que el documento no cuadra. */
    private static volatile boolean observado = false;

    private TestHubServer() {
    }

    public static synchronized void start() {
        if (server != null) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/v1/integrations/validate/ruc", exchange -> {
                llamadas.incrementAndGet();
                if (caido) {
                    exchange.sendResponseHeaders(500, -1);
                    exchange.close();
                    return;
                }
                String json = observado
                        ? "{\"ruc\":\"20601030013\",\"razonSocial\":\"\",\"estado\":\"\",\"condicion\":\"\","
                        + "\"direccionFiscal\":\"\",\"esValido\":false,\"fromCache\":false}"
                        : "{\"ruc\":\"20601030013\",\"razonSocial\":\"Talleres SAC\",\"estado\":\"ACTIVO\","
                        + "\"condicion\":\"HABIDO\",\"direccionFiscal\":\"Av. Siempre Viva 742\","
                        + "\"esValido\":true,\"fromCache\":false}";
                responder(exchange, json);
            });
            server.createContext("/api/v1/integrations/validate/placa", exchange -> {
                llamadas.incrementAndGet();
                if (caido) {
                    exchange.sendResponseHeaders(500, -1);
                    exchange.close();
                    return;
                }
                String json = observado
                        ? "{\"placa\":\"ABC-123\",\"esValido\":false,\"fromCache\":false}"
                        : "{\"placa\":\"ABC-123\",\"marca\":\"Toyota\",\"modelo\":\"Corolla\","
                        + "\"vin\":\"8AJ12345678901234\",\"numeroMotor\":\"2ZRFE\","
                        + "\"anioFabricacion\":\"2021\",\"color\":\"Blanco\",\"combustible\":\"Gasolina\","
                        + "\"esValido\":true,\"fromCache\":false}";
                responder(exchange, json);
            });
            server.start();
            baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo arrancar el hub de prueba", e);
        }
    }

    private static void responder(com.sun.net.httpserver.HttpExchange exchange, String json)
            throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    public static synchronized String baseUrl() {
        start();
        return baseUrl;
    }

    public static int llamadas() {
        return llamadas.get();
    }

    public static void reiniciarContador() {
        llamadas.set(0);
    }

    public static void setCaido(boolean valor) {
        caido = valor;
    }

    public static void setObservado(boolean valor) {
        observado = valor;
    }
}
