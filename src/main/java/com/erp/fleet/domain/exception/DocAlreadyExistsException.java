package com.erp.fleet.domain.exception;

public class DocAlreadyExistsException extends DomainException {
    public DocAlreadyExistsException(String docNumber) {
        super("FLEET_DOC_DUPLICADO", "Ya existe un cliente registrado con el documento: " + docNumber);
    }
}
