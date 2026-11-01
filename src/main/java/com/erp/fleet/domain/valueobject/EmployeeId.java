package com.erp.fleet.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class EmployeeId implements Serializable {
    private final UUID value;

    public EmployeeId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("EmployeeId cannot be null");
        }
        this.value = value;
    }

    public static EmployeeId generate() {
        return new EmployeeId(UUID.randomUUID());
    }

    public static EmployeeId fromString(String value) {
        return new EmployeeId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeId that)) return false;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
