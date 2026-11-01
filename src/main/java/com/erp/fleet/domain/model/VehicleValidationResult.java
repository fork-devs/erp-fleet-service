package com.erp.fleet.domain.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Result of validating a vehicle license plate against the integration hub.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class VehicleValidationResult {

    private final boolean valid;
    private final String vin;
    private final String engineNumber;
    private final String brand;
    private final String model;
    private final String color;
    private final String fuelType;

    @JsonCreator
    public VehicleValidationResult(
            @JsonProperty("valid") @JsonAlias("valido") boolean valid,
            @JsonProperty("vin") String vin,
            @JsonProperty("engineNumber") @JsonAlias("numeroMotor") String engineNumber,
            @JsonProperty("brand") @JsonAlias("marca") String brand,
            @JsonProperty("model") @JsonAlias("modelo") String model,
            @JsonProperty("color") String color,
            @JsonProperty("fuelType") @JsonAlias("combustible") String fuelType) {
        this.valid = valid;
        this.vin = vin;
        this.engineNumber = engineNumber;
        this.brand = brand;
        this.model = model;
        this.color = color;
        this.fuelType = fuelType;
    }

    public static VehicleValidationResult valid(String vin, String engineNumber, String brand,
                                                String model, String color, String fuelType) {
        return new VehicleValidationResult(true, vin, engineNumber, brand, model, color, fuelType);
    }

    public static VehicleValidationResult observed() {
        return new VehicleValidationResult(false, null, null, null, null, null, null);
    }

    // Compatibility aliases
    public static VehicleValidationResult valido(String vin, String engineNumber, String brand,
                                                 String model, String color, String fuelType) {
        return valid(vin, engineNumber, brand, model, color, fuelType);
    }

    public static VehicleValidationResult observado() {
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

    @JsonProperty("vin")
    public String getVin() {
        return vin;
    }

    @JsonProperty("engineNumber")
    public String getEngineNumber() {
        return engineNumber;
    }

    @JsonProperty("numeroMotor")
    public String getNumeroMotor() {
        return engineNumber;
    }

    @JsonProperty("brand")
    public String getBrand() {
        return brand;
    }

    @JsonProperty("marca")
    public String getMarca() {
        return brand;
    }

    @JsonProperty("model")
    public String getModel() {
        return model;
    }

    @JsonProperty("modelo")
    public String getModelo() {
        return model;
    }

    @JsonProperty("color")
    public String getColor() {
        return color;
    }

    @JsonProperty("fuelType")
    public String getFuelType() {
        return fuelType;
    }

    @JsonProperty("combustible")
    public String getCombustible() {
        return fuelType;
    }
}
