package com.erp.fleet.domain.valueobject;

import com.erp.fleet.domain.exception.InvalidPlateException;

import java.io.Serializable;
import java.util.Objects;
import java.util.regex.Pattern;

public final class Plate implements Serializable {
    // Formato placas peruanas: ABC-123 o ABC-1234
    private static final Pattern PLATE_PATTERN = Pattern.compile("^[A-Z]{3}-[0-9]{3,4}$");
    private final String value;

    public Plate(String value) {
        if (value == null || value.trim().isEmpty())
            throw new InvalidPlateException("La placa no puede estar vacía");
        String normalized = value.trim().toUpperCase();
        if (!PLATE_PATTERN.matcher(normalized).matches())
            throw new InvalidPlateException("Formato de placa inválido: " + value + ". Use formato ABC-123 o ABC-1234");
        this.value = normalized;
    }

    public static Plate of(String value) { return new Plate(value); }
    public String getValue() { return value; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Plate p)) return false;
        return Objects.equals(value, p.value);
    }
    @Override public int hashCode() { return Objects.hash(value); }
    @Override public String toString() { return value; }
}
