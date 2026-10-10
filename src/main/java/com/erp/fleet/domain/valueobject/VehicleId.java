package com.erp.fleet.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class VehicleId implements Serializable {
    private final UUID value;

    public VehicleId(UUID value) {
        if (value == null) throw new IllegalArgumentException("VehicleId no puede ser nulo");
        this.value = value;
    }

    public static VehicleId generate() { return new VehicleId(UUID.randomUUID()); }
    public static VehicleId fromString(String value) { return new VehicleId(UUID.fromString(value)); }

    public UUID getValue() { return value; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VehicleId v)) return false;
        return Objects.equals(value, v.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value.toString(); }
}
