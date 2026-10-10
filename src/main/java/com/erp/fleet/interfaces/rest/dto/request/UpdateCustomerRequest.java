package com.erp.fleet.interfaces.rest.dto.request;

public record UpdateCustomerRequest(
        String fullName,
        String email,
        String phone
) {}
