package com.erp.fleet.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public final class StaffCareerHistoryId implements Serializable {
    private final UUID value;

    public StaffCareerHistoryId(UUID value) {
        if (value == null) {
            throw new IllegalArgumentException("StaffCareerHistoryId cannot be null");
        }
        this.value = value;
    }

    public static StaffCareerHistoryId generate() {
        return new StaffCareerHistoryId(UUID.randomUUID());
    }

    public static StaffCareerHistoryId fromString(String value) {
        return new StaffCareerHistoryId(UUID.fromString(value));
    }

    public UUID getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof StaffCareerHistoryId that)) return false;
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
