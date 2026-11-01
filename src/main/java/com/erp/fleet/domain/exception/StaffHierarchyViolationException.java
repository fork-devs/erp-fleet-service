package com.erp.fleet.domain.exception;

public class StaffHierarchyViolationException extends DomainException {
    public StaffHierarchyViolationException(String message) {
        super("FLEET_STAFF_HIERARCHY_VIOLATION", message);
    }
}
