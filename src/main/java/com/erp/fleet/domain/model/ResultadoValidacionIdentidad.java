package com.erp.fleet.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resultado de validar un documento de identidad contra el hub.
 *
 * <p>El resultado distingue tres cosas: si el documento es valido, la
 * discrepancia que el hub informa y el fallo del hub, que NO es un resultado
 * de validacion sino un intento que hay que reintentar. Por eso el
 * adaptador lanza la excepcion cuando el hub no esta disponible, y devuelve
 * un resultado solo cuando el hub ha respondido.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class ResultadoValidacionIdentidad {

    private final boolean valido;
    private final String razonSocial;
    private final String estado;
    private final String condicion;
    private final String direccionFiscal;

    @JsonCreator
    public ResultadoValidacionIdentidad(@JsonProperty("valido") boolean valido,
                                         @JsonProperty("razonSocial") String razonSocial,
                                         @JsonProperty("estado") String estado,
                                         @JsonProperty("condicion") String condicion,
                                         @JsonProperty("direccionFiscal") String direccionFiscal) {
        this.valido = valido;
        this.razonSocial = razonSocial;
        this.estado = estado;
        this.condicion = condicion;
        this.direccionFiscal = direccionFiscal;
    }

    /**
     * El hub declara el documento valido.
     */
    public static ResultadoValidacionIdentidad valido(String razonSocial, String estado,
                                                       String condicion, String direccionFiscal) {
        return new ResultadoValidacionIdentidad(true, razonSocial, estado, condicion, direccionFiscal);
    }

    /**
     * El hub informa una discrepancia: el registro queda observado.
     */
    public static ResultadoValidacionIdentidad observado() {
        return new ResultadoValidacionIdentidad(false, null, null, null, null);
    }

    @JsonProperty("valido")
    public boolean esValido() { return valido; }

    @JsonProperty("razonSocial")
    public String getRazonSocial() { return razonSocial; }

    @JsonProperty("estado")
    public String getEstado() { return estado; }

    @JsonProperty("condicion")
    public String getCondicion() { return condicion; }

    @JsonProperty("direccionFiscal")
    public String getDireccionFiscal() { return direccionFiscal; }
}
