package com.erp.fleet.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class EmployeeRegistrationId implements Serializable {
    private final UUID value;

    public EmployeeRegistrationId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("EmployeeRegistrationId cannot be null");
        }
        this.value = value;
    }

    public static EmployeeRegistrationId generate() {
        return new EmployeeRegistrationId(UUID.randomUUID());
    }

    public static EmployeeRegistrationId fromString(String value) {
        return new EmployeeRegistrationId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmployeeRegistrationId that)) return false;
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
