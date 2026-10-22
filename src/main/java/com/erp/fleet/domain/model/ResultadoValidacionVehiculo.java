package com.erp.fleet.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resultado de validar una placa contra el hub.
 *
 * <p>Igual que en la identidad, el fallo del hub no es un resultado: es un
 * intento que se reintenta, y lo expresa una excepcion.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ResultadoValidacionVehiculo {

    private final boolean valido;
    private final String vin;
    private final String numeroMotor;
    private final String marca;
    private final String modelo;
    private final String color;
    private final String combustible;

    @JsonCreator
    public ResultadoValidacionVehiculo(@JsonProperty("valido") boolean valido,
                                       @JsonProperty("vin") String vin,
                                       @JsonProperty("numeroMotor") String numeroMotor,
                                       @JsonProperty("marca") String marca,
                                       @JsonProperty("modelo") String modelo,
                                       @JsonProperty("color") String color,
                                       @JsonProperty("combustible") String combustible) {
        this.valido = valido;
        this.vin = vin;
        this.numeroMotor = numeroMotor;
        this.marca = marca;
        this.modelo = modelo;
        this.color = color;
        this.combustible = combustible;
    }

    /**
     * El hub declara la placa valida.
     */
    public static ResultadoValidacionVehiculo valido(String vin, String numeroMotor, String marca,
                                                       String modelo, String color, String combustible) {
        return new ResultadoValidacionVehiculo(true, vin, numeroMotor, marca, modelo, color, combustible);
    }

    /**
     * El hub no reconoce la placa: el registro queda observado.
     */
    public static ResultadoValidacionVehiculo observado() {
        return new ResultadoValidacionVehiculo(false, null, null, null, null, null, null);
    }

    @JsonProperty("valido")
    public boolean esValido() { return valido; }

    @JsonProperty("vin")
    public String getVin() { return vin; }

    @JsonProperty("numeroMotor")
    public String getNumeroMotor() { return numeroMotor; }

    @JsonProperty("marca")
    public String getMarca() { return marca; }

    @JsonProperty("modelo")
    public String getModelo() { return modelo; }

    @JsonProperty("color")
    public String getColor() { return color; }

    @JsonProperty("combustible")
    public String getCombustible() { return combustible; }
}
