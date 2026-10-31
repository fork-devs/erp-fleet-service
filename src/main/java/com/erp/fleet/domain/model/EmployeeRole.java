package com.erp.fleet.domain.model;

import java.util.Arrays;

public enum EmployeeRole {
    ROLE_BRANCH_MANAGER,
    ROLE_ASSISTANT,
    ROLE_EMPLOYEE;

    public static EmployeeRole fromString(String roleStr) {
        if (roleStr == null || roleStr.trim().isEmpty()) {
            return ROLE_EMPLOYEE;
        }
        String normalized = roleStr.trim().toUpperCase();
        if (!normalized.startsWith("ROLE_")) {
            normalized = "ROLE_" + normalized;
        }
        for (EmployeeRole role : values()) {
            if (role.name().equalsIgnoreCase(normalized)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Invalid employee role: " + roleStr + ". Valid roles are: " + Arrays.toString(values()));
    }
}
