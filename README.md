A backend project developed to explore core banking operations using Java and Spring Boot. The project focuses on basic banking processes such as customer management, account creation, balance management, and money transfers.

## Features

- **Customer Management:** Create, search, list, update, and delete customer records. Customers are identified by their TCKN.
- **Account Management:** Create bank accounts linked to customers, generate unique IBANs, and manage account balances in TRY, USD, and EUR.
- **Money Transfers:** Transfer money between accounts using IBANs. The system checks account existence, currency compatibility, and available balance before processing a transfer. HTTP transfer idempotency is scoped to the authenticated identity, operation, and key.
- **Transaction History:** Store transfer details, including sender, receiver, amount, currency, and transaction timestamp.
- **REST API:** Expose banking operations through RESTful endpoints.
- **Authentication and Authorization:** Provisioned identities, BCrypt passwords, 15-minute stateless JWT access tokens, CUSTOMER/BANK_OPERATOR/AUDITOR/ADMIN roles, and customer/account ownership checks.

## Technologies

- Java 21
- Spring Boot
- Spring Web MVC
- Spring Data JPA
- Spring Security
- Hibernate
- PostgreSQL
- Maven
- Jakarta Bean Validation
- Springdoc OpenAPI

## Architecture

The project follows a layered structure with separate controllers, services, repositories, DTOs, and entities.

It uses Spring Data JPA for database operations and transaction management. Flyway migrations manage the database schema. Spring Security validates bearer JWTs on protected requests and reloads current identity status and roles from the database.

## Authentication configuration

Set `JWT_SECRET` to a unique random secret of at least 32 UTF-8 bytes and set `DB_PASSWORD` before starting the application. The application intentionally fails during startup when `JWT_SECRET` is missing or too short; the database password has no insecure YAML fallback. Define variables in the shell, IDE run configuration, or container environment (`.env.example` is only a template and is not auto-loaded by Spring Boot). Do not commit real signing secrets or database passwords. `JWT_ISSUER` defaults to `corebanking-prod`; `JWT_EXPIRATION_MS` defaults to `900000` (15 minutes).

Only `POST /api/auth/login` is public by default. An `ADMIN` identity provisions users through `POST /api/auth/users`; public self-registration is not supported. A `CUSTOMER` identity must be linked to one existing customer record, while staff identities must not have a customer link.

For a new database, no default administrator is seeded. To create the first `ADMIN`, temporarily set `ADMIN_BOOTSTRAP_ENABLED=true`, `ADMIN_BOOTSTRAP_USERNAME`, and `ADMIN_BOOTSTRAP_PASSWORD` in one application instance. The startup runner creates the administrator only when no `ADMIN` identity exists; it uses the same BCrypt provisioning service. Use a password of at least 12 characters and no more than 72 UTF-8 bytes. After the first administrator is created, remove all three bootstrap variables and restart the application. Do not leave bootstrap enabled in a multi-instance deployment; use a single instance for this one-time operation. The feature never creates a default username or password.

Send the returned token as `Authorization: Bearer <accessToken>` to protected APIs. `OPENAPI_ENABLED` controls whether the Swagger/OpenAPI endpoints are enabled; `OPENAPI_PUBLIC` separately controls anonymous access (both default to `false`). Login throttling defaults to 10 failed attempts per client IP in 15 minutes. The in-process limiter is a fallback; clustered deployments also require a gateway/shared rate limiter.

Payment idempotency keys are isolated per authenticated identity; historical pre-authentication records use the reserved internal `principal_id=0` scope and cannot satisfy an authenticated user request. This release does not implement refresh tokens, logout, token blacklisting, public sign-up, or persistent authentication audit storage. Authentication events are published for integration with the SPEC-015 audit implementation.
