@US12 @EP05 @erp-fleet-service
# User Story: US12 - Manage full lifecycle of service appointments, bay confirmation, and check-in
Feature: Service Appointments, Bay Confirmation, and Physical Check-in
  As a service advisor or workshop administrator
  I want to schedule, confirm, reschedule, check in, complete, or cancel service appointments
  So that vehicle intake is orderly without bay overbooking or time conflicts

  Background:
    Given the "erp-fleet-service" REST API is available at "/api/v1"

  @US12 @happy-path
  # User Story: US12 - Schedule, confirm, and check in a service appointment
  Scenario: Service advisor schedules an appointment, confirms bay availability, and performs check-in
    Given an authenticated user with role "SERVICE_ADVISOR" in tenant "tenant-lima-01"
    And an existing customer "customer-uuid-01", vehicle "vehicle-uuid-01", and branch "branch-uuid-01" with available bay capacity
    When the client sends a "POST" request to "/api/v1/appointments" with JSON payload:
      """
      {
        "branchId": "branch-uuid-01",
        "customerId": "customer-uuid-01",
        "vehicleId": "vehicle-uuid-01",
        "scheduledAt": "2026-11-20T10:00:00Z",
        "serviceType": "40,000 km Preventive Maintenance"
      }
      """
    Then the response status code should be 201
    And the JSON response field "status" should equal "SCHEDULED"
    When the client sends a "POST" request to "/api/v1/appointments/{appointmentId}/confirm"
    Then the response status code should be 200
    And the JSON response field "status" should equal "CONFIRMED"
    When the client sends a "POST" request to "/api/v1/appointments/{appointmentId}/check-in"
    Then the response status code should be 200
    And the JSON response field "status" should equal "CHECKED_IN"

  @US12 @negative
  # User Story: US12 - Reject overlapping appointment when bay capacity is full
  Scenario: Scheduling an appointment that collides with full bay capacity returns HTTP 409
    Given an authenticated user with role "SERVICE_ADVISOR"
    And branch "branch-uuid-01" has zero available bay slots at "2026-11-20T10:00:00Z"
    When the client sends a "POST" request to "/api/v1/appointments" with JSON payload:
      """
      {
        "branchId": "branch-uuid-01",
        "customerId": "customer-uuid-02",
        "vehicleId": "vehicle-uuid-02",
        "scheduledAt": "2026-11-20T10:00:00Z",
        "serviceType": "Oil Change"
      }
      """
    Then the response status code should be 409
    And the JSON response should contain an error message indicating time slot or bay capacity conflict
