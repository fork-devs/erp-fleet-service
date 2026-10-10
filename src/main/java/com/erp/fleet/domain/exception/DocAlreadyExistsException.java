package com.erp.fleet.domain.exception;

public class DocAlreadyExistsException extends DomainException {
    public DocAlreadyExistsException(String docNumber) {
        super("Ya existe un cliente registrado con el documento: " + docNumber);
    }
}
