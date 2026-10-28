package com.erp.fleet.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing identity (RUC) validation response received from the integration hub.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RucResponse(
        @JsonProperty("ruc") String ruc,
        @JsonProperty("razonSocial") @JsonAlias("legalName") String legalName,
        @JsonProperty("estado") @JsonAlias("taxStatus") String taxStatus,
        @JsonProperty("condicion") @JsonAlias("taxCondition") String taxCondition,
        @JsonProperty("direccionFiscal") @JsonAlias("fiscalAddress") String fiscalAddress,
        @JsonProperty("esValido") @JsonAlias("valid") boolean valid,
        @JsonProperty("fromCache") boolean fromCache
) {
    public boolean isValid() {
        return valid;
    }

    public boolean esValido() {
        return valid;
    }

    public String razonSocial() {
        return legalName;
    }

    public String estado() {
        return taxStatus;
    }

    public String condicion() {
        return taxCondition;
    }

    public String direccionFiscal() {
        return fiscalAddress;
    }
}
