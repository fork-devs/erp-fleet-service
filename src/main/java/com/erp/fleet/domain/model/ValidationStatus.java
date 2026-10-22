package com.erp.fleet.domain.model;

/**
 * Estado de la validacion de un cliente o de un vehiculo contra el
 * {@code integration-hub}.
 *
 * <p>Son tres estados y no un booleano, porque "observado" no es lo mismo que
 * "validado": observado es la discrepancia que informa el proveedor —el
 * nombre comercial no coincide con el RUC, la placa no existe—, que el taller
 * tiene que revisar.
 *
 * <p>Un fallo del hub NO es un estado de validacion: es un intento que se
 * reintenta. Mezclar ambas cosas en un mismo estado —como hacia
 * {@code FAILED}— impide tratar de forma distinta la discrepancia que hay que
 * mostrar y el fallo que hay que reintentar.
 */
public enum ValidationStatus {
    PENDING,
    VALID,
    OBSERVED
}
