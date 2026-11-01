package com.erp.fleet.domain.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of validating a customer identity document (RUC/DNI) against the integration hub.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class IdentityValidationResult {

    private final boolean valid;
    private final String legalName;
    private final String taxStatus;
    private final String taxCondition;
    private final String fiscalAddress;

    @JsonCreator
    public IdentityValidationResult(
            @JsonProperty("valid") @JsonAlias("valido") boolean valid,
            @JsonProperty("legalName") @JsonAlias("razonSocial") String legalName,
            @JsonProperty("taxStatus") @JsonAlias("estado") String taxStatus,
            @JsonProperty("taxCondition") @JsonAlias("condicion") String taxCondition,
            @JsonProperty("fiscalAddress") @JsonAlias("direccionFiscal") String fiscalAddress) {
        this.valid = valid;
        this.legalName = legalName;
        this.taxStatus = taxStatus;
        this.taxCondition = taxCondition;
        this.fiscalAddress = fiscalAddress;
    }

    public static IdentityValidationResult valid(String legalName, String taxStatus,
                                                  String taxCondition, String fiscalAddress) {
        return new IdentityValidationResult(true, legalName, taxStatus, taxCondition, fiscalAddress);
    }

    public static IdentityValidationResult observed() {
        return new IdentityValidationResult(false, null, null, null, null);
    }

    // Compatibility alias
    public static IdentityValidationResult valido(String legalName, String taxStatus,
                                                  String taxCondition, String fiscalAddress) {
        return valid(legalName, taxStatus, taxCondition, fiscalAddress);
    }

    public static IdentityValidationResult observado() {
        return observed();
    }

    @JsonProperty("valid")
    public boolean isValid() {
        return valid;
    }

    @JsonProperty("valido")
    public boolean esValido() {
        return valid;
    }

    @JsonProperty("legalName")
    public String getLegalName() {
        return legalName;
    }

    @JsonProperty("razonSocial")
    public String getRazonSocial() {
        return legalName;
    }

    @JsonProperty("taxStatus")
    public String getTaxStatus() {
        return taxStatus;
    }

    @JsonProperty("estado")
    public String getEstado() {
        return taxStatus;
    }

    @JsonProperty("taxCondition")
    public String getTaxCondition() {
        return taxCondition;
    }

    @JsonProperty("condicion")
    public String getCondicion() {
        return taxCondition;
    }

    @JsonProperty("fiscalAddress")
    public String getFiscalAddress() {
        return fiscalAddress;
    }

    @JsonProperty("direccionFiscal")
    public String getDireccionFiscal() {
        return fiscalAddress;
    }
}
