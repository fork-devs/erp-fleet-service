@US10 @EP04 @erp-fleet-service
# User Story: US10 - Manage customers lifecycle, branch affiliation, and RENIEC/SUNAT validation
Feature: Customer Lifecycle, Branch Affiliation, and RENIEC/SUNAT Validation
  As a service advisor or workshop administrator
  I want to register, update, affiliate customers to branches, and validate DNI/RUC online
  So that billing and service records use verified legal customer data

  Background:
    Given the "erp-fleet-service" REST API is available at "/api/v1"

  @US10 @happy-path
  # User Story: US10 - Register a new customer and validate identity
  Scenario: Service advisor registers a customer with valid DNI and contact details
    Given an authenticated user with role "SERVICE_ADVISOR" in tenant "tenant-lima-01"
    When the client sends a "POST" request to "/api/v1/customers" with JSON payload:
      """
      {
        "documentType": "DNI",
        "documentNumber": "45678912",
        "fullName": "Lucia Fernandez Quispe",
        "email": "lucia.fernandez@example.com",
        "phone": "+51991122334",
        "address": "Calle Los Pinos 340, Miraflores"
      }
      """
    Then the response status code should be 201
    And the JSON response should contain a generated "id"
    And the JSON response field "documentNumber" should equal "45678912"

  @US10 @negative
  # User Story: US10 - Prevent duplicate customer document number within same tenant
  Scenario: Registering a customer with an existing document number in the same tenant returns HTTP 409
    Given an authenticated user with role "SERVICE_ADVISOR" in tenant "tenant-lima-01"
    And a customer with document number "45678912" already exists in tenant "tenant-lima-01"
    When the client sends a "POST" request to "/api/v1/customers" with JSON payload:
      """
      {
        "documentType": "DNI",
        "documentNumber": "45678912",
        "fullName": "Duplicate Customer",
        "email": "dup@example.com",
        "phone": "+51991122335"
      }
      """
    Then the response status code should be 409
    And the JSON response should contain an error message indicating duplicate customer document
