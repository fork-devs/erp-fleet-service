package com.erp.fleet.domain.exception;

public class StaffCareerHistoryNotFoundException extends DomainException {
    public StaffCareerHistoryNotFoundException(String id) {
        super("FLEET_CAREER_HISTORY_NOT_FOUND", "Staff career history not found with ID: " + id);
    }
}
