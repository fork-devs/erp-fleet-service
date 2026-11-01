package com.erp.fleet.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class BranchId implements Serializable {
    private final UUID value;

    public BranchId(UUID value) {
        if (value == null) throw new IllegalArgumentException("BranchId no puede ser nulo");
        this.value = value;
    }

    public static BranchId generate() {
        return new BranchId(UUID.randomUUID());
    }

    public static BranchId fromString(String value) {
        return new BranchId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BranchId branchId)) return false;
        return Objects.equals(value, branchId.value);
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
