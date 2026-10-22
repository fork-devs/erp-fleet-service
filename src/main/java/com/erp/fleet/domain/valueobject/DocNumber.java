package com.erp.fleet.domain.valueobject;

import java.io.Serializable;
import java.util.Objects;

public final class DocNumber implements Serializable {
    private final String value;
    private final String docType;

    public DocNumber(String value, String docType) {
        if (value == null || value.trim().isEmpty())
            throw new IllegalArgumentException("El número de documento no puede estar vacío");
        this.value = value.trim();
        this.docType = docType;
    }

    public static DocNumber of(String value, String docType) { return new DocNumber(value, docType); }
    public String getValue() { return value; }
    public String getDocType() { return docType; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DocNumber d)) return false;
        return Objects.equals(value, d.value) && Objects.equals(docType, d.docType);
    }
    @Override public int hashCode() { return Objects.hash(value, docType); }
    @Override public String toString() { return docType + ":" + value; }
}
