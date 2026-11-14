@US11 @EP04 @erp-fleet-service
# User Story: US11 - Manage vehicle fleet and validate license plate/VIN against SUNARP
Feature: Vehicle Fleet Management and SUNARP Plate/VIN Validation
  As a service advisor or mechanic
  I want to register vehicles, update mileage, query customer vehicles, and validate plates against SUNARP
  So that technical service dossiers remain accurate and free of identification errors

  Background:
    Given the "erp-fleet-service" REST API is available at "/api/v1"

  @US11 @happy-path
  # User Story: US11 - Register a vehicle linked to an existing customer
  Scenario: Service advisor registers a vehicle with plate, VIN, and initial mileage
    Given an authenticated user with role "SERVICE_ADVISOR" in tenant "tenant-lima-01"
    And an existing customer "customer-uuid-01" in tenant "tenant-lima-01"
    When the client sends a "POST" request to "/api/v1/vehicles" with JSON payload:
      """
      {
        "customerId": "customer-uuid-01",
        "licensePlate": "ABC-123",
        "vin": "1HGBH41JXMN109186",
        "brand": "Toyota",
        "model": "Corolla",
        "year": 2022,
        "engineNumber": "2ZR-FE-987654",
        "mileage": 32500
      }
      """
    Then the response status code should be 201
    And the JSON response should contain a generated "id"
    And the JSON response field "licensePlate" should equal "ABC-123"

  @US11 @negative
  # User Story: US11 - Prevent duplicate license plate registration in same tenant
  Scenario: Registering a vehicle with a duplicate license plate in the same tenant returns HTTP 409
    Given an authenticated user with role "SERVICE_ADVISOR" in tenant "tenant-lima-01"
    And a vehicle with license plate "ABC-123" is already registered in tenant "tenant-lima-01"
    When the client sends a "POST" request to "/api/v1/vehicles" with JSON payload:
      """
      {
        "customerId": "customer-uuid-01",
        "licensePlate": "ABC-123",
        "vin": "1HGBH41JXMN109187",
        "brand": "Toyota",
        "model": "Corolla",
        "year": 2022,
        "mileage": 33000
      }
      """
    Then the response status code should be 409
    And the JSON response should contain an error message indicating license plate already exists
