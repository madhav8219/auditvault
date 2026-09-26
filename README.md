# AuditVault

AuditVault is a Spring Boot application for managing tamper-evident audit logs with hash chaining, retention controls, redaction, export, compliance reporting, and JWT-based authentication.

## Overview

The service records operational and security events in a way that supports:

- tamper detection through cryptographic hash chaining
- traceability of who performed which action and when
- redaction of sensitive fields without breaking chain integrity
- retention and archival policies for historical data
- export of verifiable audit bundles
- compliance-oriented reporting for access review and governance
- secure access to API endpoints using JWT authentication

## Features

- Create, query, and retrieve audit records
- Verify hash chain integrity across the full log history
- Detect tampering and identify the first inconsistent record
- Archive and restore records based on retention policy
- Redact sensitive fields while preserving hash-chain verifiability
- Export records for a specific actor or resource as a bundled payload
- Generate compliance reports for sensitive data access patterns
- Protect endpoints with JWT validation and Spring Security
- Expose OpenAPI/Swagger documentation for every controller

## Tech Stack

- Java 17
- Spring Boot 4.1.0
- Spring Security
- JWT (JJWT)
- Spring Data JPA
- MySQL
- H2 (used for test profile)
- Springdoc OpenAPI / Swagger UI
- Lombok
- JUnit 5
- Maven Wrapper

## Build Status

The project is currently building successfully with Maven.

```powershell
./mvnw.cmd clean install
```

This was verified with a fresh install run producing `BUILD SUCCESS` and exit code `0`.

## Project Structure

```text
src/
  main/
    java/
      com/auditvault/auditvault/
        config/
        controller/
        domain/
        dto/
        exception/
        repository/
        security/
        service/
    resources/
      application.properties
  test/
    java/
      com/auditvault/auditvault/
    resources/
      application-test.properties
```

## Prerequisites

Before running the project, ensure the following are installed:

- Java 17+
- Maven 3.9+ or the included Maven Wrapper
- MySQL 8+ running locally, or use the test profile for H2-based validation

## Configuration

The application is configured in `src/main/resources/application.properties`.

### Database configuration

```properties
spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/auditvault}
spring.datasource.username=${DB_USERNAME:root}
spring.datasource.password=${DB_PASSWORD:admin}
```

Environment variables with fallback defaults:

- `DB_URL` - MySQL connection URL (default: `jdbc:mysql://localhost:3306/auditvault`)
- `DB_USERNAME` - Database username (default: `root`)
- `DB_PASSWORD` - Database password (default: `admin`)

### JWT settings

```properties
app.jwt.secret=${JWT_SECRET:change-me-local-dev-32-char-secret-key}
app.jwt.expiration-ms=${JWT_EXPIRATION_MS:3600000}
```

Use a strong secret and rotate it safely in non-local environments.

## Security Model

The application uses JWT-based security with Spring Security.

### Public endpoints

These endpoints do not require a JWT token:

- `GET /`
- `GET /error`
- `POST /auth/login`
- `POST /auth/register`
- Swagger/OpenAPI endpoints: `/swagger-ui/**`, `/v3/api-docs/**`, `/swagger-ui.html`, `/webjars/**`

### Protected endpoints

All other endpoints require a valid JWT token in the `Authorization` header:

```http
Authorization: Bearer <jwt-token>
```

Examples of protected endpoints:

- `POST /audit/create`
- `GET /audit/query`
- `GET /audit/{id}`
- `GET /audit/count`
- `GET /audit/verify`
- `GET /audit/detect-tampering`
- `POST /audit/retention/archive`
- `POST /audit/retention/restore/{recordId}`
- `GET /audit/retention/status`
- `POST /audit/redact/{recordId}`
- `GET /audit/redaction/{recordId}`
- `GET /audit/export/actor/{actorId}`
- `GET /audit/export/resource/{resourceId}`
- `GET /audit/compliance/report`

## JWT Validation Flow

1. Client calls `POST /auth/login` with a valid username and password.
2. Spring Security validates the credentials.
3. The server generates a signed JWT.
4. The client sends the token in the `Authorization` header as `Bearer <token>`.
5. The JWT filter validates the token on each request.
6. If valid, the request proceeds.
7. If missing, malformed, expired, or invalid, the server returns `401 Unauthorized`.

## Database and User Storage

User information is stored in the database using JPA entities and secure password hashing.

- `users`
- `roles`
- `user_roles`

Passwords are stored using BCrypt.

## Run the Application

From the project root:

### Linux / macOS

```bash
./mvnw spring-boot:run
```

### Windows

```powershell
./mvnw.cmd spring-boot:run
```

The application starts on:

```text
http://localhost:8080
```

### Test profile (H2)

For local validation without MySQL:

```powershell
$env:SPRING_PROFILES_ACTIVE = "test"
./mvnw.cmd spring-boot:run
```

## Swagger / OpenAPI Documentation

Swagger UI is enabled through Springdoc.

Open:

```text
http://localhost:8080/swagger-ui/index.html
```

The API docs are generated from the controller annotations and DTO schemas. JSON request payloads, such as the `payload` field for `POST /audit/create`, are exposed as object-shaped payloads rather than plain strings.

## API Example: Create Audit Log

```http
POST /audit/create
Content-Type: application/json
Authorization: Bearer <jwt-token>

{
  "eventType": "CREATE",
  "actorId": "admin",
  "resourceType": "invoice",
  "resourceId": "INV-1001",
  "payload": {
    "orderId": "A-42",
    "amount": 125.5,
    "currency": "USD"
  },
  "timestamp": "2026-08-19T12:00:00"
}
```

## Auth Example

### Register

```http
POST /auth/register
Content-Type: application/json

{
  "username": "demo-user",
  "email": "demo@example.com",
  "password": "StrongPass@123"
}
```

### Login

```http
POST /auth/login
Content-Type: application/json

{
  "username": "demo-user",
  "password": "StrongPass@123"
}
```

Example response:

```json
{
  "token": "<jwt-token>",
  "type": "Bearer",
  "username": "demo-user",
  "email": "demo@example.com"
}
```

## Required JWT Claims

The JWT includes the following essential claim information:

- `sub` → username
- `iat` → issue time
- `exp` → expiration time

The token is signed with the configured secret using HMAC SHA-256.

## Notes

- The build is validated with Maven and should be run using the included wrapper for consistent environment behavior.
- Swagger/OpenAPI payload modeling was corrected so nested JSON object payloads render correctly in the UI.
- The default profile expects MySQL, while the `test` profile uses an in-memory H2 database for validation and local testing.


### Step 3: Use the token in protected requests

Add the token in the Authorization header:

```http
GET /audit/count
Authorization: Bearer <jwt-token>
```

### curl example

```bash
curl -X GET "http://localhost:8080/audit/count" \
  -H "Authorization: Bearer <jwt-token>"
```

### Postman example

1. Create a new request.
2. Select `GET`.
3. Set URL to `http://localhost:8080/audit/count`.
4. Open the `Headers` tab.
5. Add:
   - `Authorization: Bearer <jwt-token>`
6. Send the request.

## Example Protected API Calls

### Create audit log with token

```bash
curl -X POST "http://localhost:8080/audit/create" \
  -H "Authorization: Bearer <jwt-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "DATA_READ",
    "actorId": "user-123",
    "resourceType": "ACCOUNT",
    "resourceId": "ACC-1001",
    "payload": {
      "accountNumber": "123456789",
      "action": "view",
      "region": "US"
    }
  }'
```

### Query audit logs with token

```bash
curl -X GET "http://localhost:8080/audit/query?actorId=user-123&pageNumber=0&pageSize=10" \
  -H "Authorization: Bearer <jwt-token>"
```

## Validation and Testing

### Public endpoint without token

```bash
curl -i http://localhost:8080/
```

Expected result:

- HTTP `200 OK`
- Response contains `Welcome to AuditVault API`

### Protected endpoint without token

```bash
curl -i http://localhost:8080/audit/count
```

Expected result:

- HTTP `401 Unauthorized`
- Authentication error due to missing JWT token

### Protected endpoint with valid token

```bash
curl -i http://localhost:8080/audit/count \
  -H "Authorization: Bearer <jwt-token>"
```

Expected result:

- HTTP `200 OK`
- JSON response with the audit record count

## Main API Endpoints

### Audit log management

- `POST /audit/create` - create a new audit log entry
- `GET /audit/query` - query audit logs with filters and pagination
- `GET /audit/{id}` - fetch a single audit log by ID
- `GET /audit/count` - get total record count

### Integrity verification

- `GET /audit/verify` - verify the full hash chain
- `GET /audit/detect-tampering` - detect tampering in the hash chain

### Retention, redaction, and export

- `POST /audit/retention/archive` - archive old records
- `POST /audit/retention/restore/{recordId}` - restore an archived record
- `GET /audit/retention/status` - get retention statistics
- `POST /audit/redact/{recordId}` - redact sensitive fields
- `GET /audit/redaction/{recordId}` - get redaction metadata
- `GET /audit/export/actor/{actorId}` - export records by actor
- `GET /audit/export/resource/{resourceId}` - export records by resource

### Compliance reporting

- `GET /audit/compliance/report?daysBack=30` - generate a compliance report for recent access events

## Database Notes

The app uses JPA with Hibernate and automatically updates the schema with:

```properties
spring.jpa.hibernate.ddl-auto=update
```

The `audit_logs` table stores the audit record fields, hash values, retention flags, and JSON payload metadata.

## Testing

Run tests with:

```bash
./mvnw test
```

## Notes

This service is built for audit and compliance use cases and emphasizes integrity and traceability. The hash chain makes it possible to identify tampering even when records are preserved, archived, or partially redacted. JWT authentication adds a secure layer for authorizing access to sensitive API operations.
