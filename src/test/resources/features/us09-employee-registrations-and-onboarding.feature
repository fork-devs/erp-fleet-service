@US09 @EP03 @erp-fleet-service
# User Story: US09 - Manage staff applications and onboarding to workshop branches
Feature: Employee Registrations and Branch Onboarding
  As a workshop administrator or applicant technician
  I want to submit branch join requests and approve or reject employee registrations
  So that qualified specialists are formally onboarded to each workshop branch

  Background:
    Given the "erp-fleet-service" REST API is available at "/api/v1"

  @US09 @happy-path
  # User Story: US09 - Submit and approve a branch join request
  Scenario: Technician submits a join request and administrator approves onboarding
    Given an authenticated technician user "tech-applicant-01"
    When the client sends a "POST" request to "/api/v1/employee-registrations/request-join" with JSON payload:
      """
      {
        "branchId": "branch-uuid-01",
        "specialty": "Engine Diagnostics",
        "notes": "5 years experience in European vehicles"
      }
      """
    Then the response status code should be 201
    And the JSON response field "status" should equal "PENDING"
    When an authenticated "WORKSHOP_ADMIN" sends a "POST" request to "/api/v1/employee-registrations/{registrationId}/approve" with role "TECHNICIAN"
    Then the response status code should be 200
    And the JSON response field "status" should equal "APPROVED"

  @US09 @negative
  # User Story: US09 - Reject approval of an already rejected registration
  Scenario: Approving an already rejected employee registration returns HTTP 409 Conflict
    Given an employee registration "reg-rejected-01" with status "REJECTED"
    And an authenticated user with role "WORKSHOP_ADMIN"
    When the client sends a "POST" request to "/api/v1/employee-registrations/reg-rejected-01/approve"
    Then the response status code should be 409
    And the JSON response should contain an error message indicating invalid registration state transition
