package com.erp.fleet.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class CustomerId implements Serializable {
    private final UUID value;

    public CustomerId(UUID value) {
        if (value == null) throw new IllegalArgumentException("CustomerId no puede ser nulo");
        this.value = value;
    }

    public static CustomerId generate() { return new CustomerId(UUID.randomUUID()); }
    public static CustomerId fromString(String value) { return new CustomerId(UUID.fromString(value)); }

    public UUID getValue() { return value; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerId c)) return false;
        return Objects.equals(value, c.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value.toString(); }
}
