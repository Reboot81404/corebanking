# Core Banking System

A backend project developed with Java 21 and Spring Boot to simulate core banking operations through RESTful APIs.

## Features

- **Customer Management:** Create, update, search, and manage customer records.
- **Account Management:** Create customer accounts, generate unique IBANs, and manage balances in TRY, USD, and EUR.
- **Money Transfers:** Transfer funds between accounts with balance and currency validation.
- **Transaction History:** Track completed transfers and transaction details.
- **Authentication & Authorization:** JWT-based authentication with role-based access control.
- **Ownership Validation:** Restrict customers to their own accounts and operations.
- **Idempotent Transfers:** Prevent duplicate transfer processing through idempotency keys.

## Technologies

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA & Hibernate
- PostgreSQL
- Flyway
- Maven
- Jakarta Bean Validation
- Springdoc OpenAPI

## Architecture

The project follows a layered architecture:

- **Controller:** Handles HTTP requests and responses.
- **Service:** Contains business logic.
- **Repository:** Manages database operations.
- **Entity:** Represents database models.
- **DTO:** Handles API request and response objects.

Flyway is used to manage database schema migrations, while Spring Security handles authentication and authorization.

## Configuration

The application requires the following environment variables:

| Variable | Description |
|---|---|
| `DB_URL` | PostgreSQL connection URL |
| `DB_USERNAME` | Database username |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | JWT signing secret (minimum 32 UTF-8 bytes) |

Optional variables:

| Variable | Default |
|---|---|
| `JWT_ISSUER` | `corebanking-prod` |
| `JWT_EXPIRATION_MS` | `900000` |
| `BANK_CODE` | `00000` |
| `OPENAPI_ENABLED` | `false` |
| `OPENAPI_PUBLIC` | `false` |
| `LOGIN_MAX_ATTEMPTS` | `10` |
| `LOGIN_WINDOW_SECONDS` | `900` |

Set these variables in your environment or IDE run configuration. The application does not automatically load `.env` files.

## Authentication

- JWT access tokens are valid for 15 minutes by default.
- Supported roles: `CUSTOMER`, `BANK_OPERATOR`, `AUDITOR`, and `ADMIN`.
- Only `POST /api/auth/login` is publicly accessible by default.
- User provisioning is restricted to administrators.
- Public self-registration is not supported.
- Passwords are stored using BCrypt.

Include the access token in protected requests:

```http
Authorization: Bearer <accessToken>
```

### Initial Administrator

No default administrator account is created.

To provision the first administrator, temporarily configure:

```properties
ADMIN_BOOTSTRAP_ENABLED=true
ADMIN_BOOTSTRAP_USERNAME=admin
ADMIN_BOOTSTRAP_PASSWORD=your-secure-password
```

After the administrator is created, remove these variables and restart the application.

Use this process on a single application instance only.

## Running the Application

Build the project:

```bash
./mvnw clean package
```

Run the application:

```bash
./mvnw spring-boot:run
```

The application runs on port `8081` by default.

## API Documentation

OpenAPI and Swagger UI can be enabled using:

```properties
OPENAPI_ENABLED=true
OPENAPI_PUBLIC=true
```

`OPENAPI_ENABLED` controls endpoint availability, while `OPENAPI_PUBLIC` controls anonymous access.

## Security Notes

- Never commit database passwords or JWT signing secrets.
- Use a strong JWT secret of at least 32 UTF-8 bytes.
- Login attempts are rate-limited.
- Production deployments with multiple instances require a shared rate-limiting solution.
- Refresh tokens, token blacklisting, public registration, and persistent authentication audit storage are not implemented in this release.

## License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.
