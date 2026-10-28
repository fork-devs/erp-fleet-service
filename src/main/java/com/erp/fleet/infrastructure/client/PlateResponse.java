package com.erp.fleet.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO representing vehicle validation response received from the integration hub.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PlateResponse(
        @JsonProperty("placa") @JsonAlias("plate") String plate,
        @JsonProperty("marca") @JsonAlias("brand") String brand,
        @JsonProperty("modelo") @JsonAlias("model") String model,
        @JsonProperty("vin") String vin,
        @JsonProperty("numeroMotor") @JsonAlias("engineNumber") String engineNumber,
        @JsonProperty("anioFabricacion") @JsonAlias("manufacturingYear") String manufacturingYear,
        @JsonProperty("color") String color,
        @JsonProperty("combustible") @JsonAlias("fuelType") String fuelType,
        @JsonProperty("esValido") @JsonAlias("valid") boolean valid,
        @JsonProperty("fromCache") boolean fromCache
) {
    public boolean isValid() {
        return valid;
    }

    public boolean esValido() {
        return valid;
    }
}
