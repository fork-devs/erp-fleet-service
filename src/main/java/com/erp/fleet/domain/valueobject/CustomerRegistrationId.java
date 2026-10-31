package com.erp.fleet.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class CustomerRegistrationId implements Serializable {
    private final UUID value;

    public CustomerRegistrationId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("CustomerRegistrationId cannot be null");
        }
        this.value = value;
    }

    public static CustomerRegistrationId generate() {
        return new CustomerRegistrationId(UUID.randomUUID());
    }

    public static CustomerRegistrationId fromString(String value) {
        return new CustomerRegistrationId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CustomerRegistrationId that)) return false;
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
