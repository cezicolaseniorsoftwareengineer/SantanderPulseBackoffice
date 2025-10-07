# Santander Pulse Backend

Banking backend application with H2 database and JWT authentication.  
**Version**: 1.0.0 | **Updated**: 2025-10-03

## Features

- JWT Authentication with refresh tokens
- H2 in-memory database with JPA/Hibernate
- RESTful API with OpenAPI documentation
- Banking validations (CPF/CNPJ Module 11)
- Role-based access control (RBAC)
- Audit logging and security
- Optional test data initialization (disabled by default)

## Technology Stack

- **Java 17** (Microsoft JDK 17.0.16.8-hotspot)
- **Spring Boot 3.5.6**
- **Spring Security 6.x**
- **Spring Data JPA**
- **H2 Database**
- **JWT (JSON Web Tokens) 0.12.3**
- **OpenAPI/Swagger 2.2.0**
- **Maven 3.9.x**

## Quick Start

### Prerequisites

- Java 17 or higher (Microsoft JDK recommended)
- Maven 3.8 or higher

### Running the Application

```bash
# Clone and navigate to backend directory
cd santander-pulse-backend

# Run with Maven
mvn spring-boot:run

# Or build and run JAR
mvn clean package
java -jar target/pulse-backend-1.0.0.jar
```

### Application URLs

- **API Base**: http://localhost:8080/api
- **H2 Console**: http://localhost:8080/api/h2-console
- **Swagger UI**: http://localhost:8080/api/swagger-ui.html
- **Health Check**: http://localhost:8080/api/actuator/health
- **Frontend**: http://localhost:4200

### Quick Access

**To access the system immediately:**
1. Backend will be running at: http://localhost:8080/api
2. Frontend will be running at: http://localhost:4200
3. Use admin credentials: **CPF: 11122233344** | **Password: admin123**

Navigate to http://localhost:4200, you'll be redirected to login, use the credentials above.

## Authentication

### Authentication Methods

The application supports two authentication methods:

1. **CPF/Password Authentication** - Traditional login with CPF and password
2. **Google OAuth 2.0** - Login with Google account

### Default Users (CPF/Password)

By default the application now bootstraps a technical **ADMIN** account on startup so you can access the backoffice without extra steps:

| CPF | Password | Role | Description |
|----------|----------|------|--------------|
| 11122233344 | admin123 | ADMIN | System administrator |

The credentials can be overridden through environment variables (`APP_BOOTSTRAP_DEFAULT_ADMIN_*`) or disabling the bootstrap entirely with `APP_BOOTSTRAP_DEFAULT_ADMIN_ENABLED=false`.

> **Seeding is still optional:** the complete seeding of demonstration users/customers remains controlled by the `pulse.seed-data.enabled` flag. See [Sample Data](#sample-data) to enable or disable additional data.

When seeding is active, extra `MANAGER` and `USER` accounts are also automatically created for RBAC scenarios.

### Google OAuth 2.0 Setup

To enable Google authentication, you must configure OAuth credentials:

#### Step 1: Create Google OAuth Credentials

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Navigate to **APIs & Services** > **Credentials**
4. Click **Create Credentials** > **OAuth 2.0 Client ID**
5. Configure consent screen if prompted
6. Select **Web application** as application type
7. Add authorized redirect URIs:
  - `http://localhost:8080/api/login/oauth2/code/google` (development)
  - Your production URL + `/api/login/oauth2/code/google` (production)
8. Copy the **Client ID** and **Client Secret**

#### Step 2: Configure Environment Variables

**Option A: Using PowerShell Script (Recommended)**

```powershell
cd scripts
.\set-google-oauth.ps1 -ClientId "YOUR_CLIENT_ID" -ClientSecret "YOUR_CLIENT_SECRET"
```

Add `-Persist` flag to save variables permanently:

```powershell
.\set-google-oauth.ps1 -ClientId "YOUR_CLIENT_ID" -ClientSecret "YOUR_CLIENT_SECRET" -Persist
```

**Option B: Manual Environment Variables**

Windows PowerShell:
```powershell
$env:GOOGLE_CLIENT_ID = "your-client-id.apps.googleusercontent.com"
$env:GOOGLE_CLIENT_SECRET = "your-client-secret"
```

Linux/Mac:
```bash
export GOOGLE_CLIENT_ID="your-client-id.apps.googleusercontent.com"
export GOOGLE_CLIENT_SECRET="your-client-secret"
```

**Option C: Using .env file**

Copy `.env.example` to `.env` and fill in your credentials:
```bash
cp .env.example .env
# Edit .env with your actual credentials
```

**Option D: Using a local properties file**

Copy `src/main/resources/application-oauth.properties.example` to `application-oauth.properties`
and replace the placeholders with your values. The file is ignored by Git and is loaded automatically.

#### Step 3: Verify Configuration

Start the backend and check the provider endpoint:

```bash
curl http://localhost:8080/api/auth/providers
```

Expected response when configured correctly:
```json
{
  "providers": {
   "google": {
    "enabled": true,
    "authorizationUrl": "http://localhost:8080/api/oauth2/authorization/google",
    "redirectUri": "http://localhost:8080/api/login/oauth2/code/google",
    "scopes": ["profile", "email"]
   }
  }
}
```

If `enabled: false`, verify your environment variables are set correctly.

#### Security Notes

- Never commit real OAuth credentials to version control
- Use different credentials for development and production
- Rotate credentials periodically
- Restrict authorized domains in Google Console
- The application validates credentials at startup and rejects placeholder values

### JWT Authentication Flow

1. **Login**: POST `/auth/login` with CPF/password
2. **Response**: Receive access token and refresh token
3. **API Calls**: Include `Authorization: Bearer <token>` header
4. **Refresh**: POST `/auth/refresh` with refresh token

### Google OAuth Flow

1. Frontend redirects user to `/api/oauth2/authorization/google`
2. User authenticates with Google
3. Google redirects to `/api/login/oauth2/code/google`
4. Backend creates/updates user and generates JWT tokens
5. Backend redirects to frontend callback with tokens in URL params
6. Frontend stores tokens and completes login

1. Create an **OAuth 2.0 Web Client** and add the following redirect URIs:
  - `http://localhost:8080/api/login/oauth2/code/google`
  - (optional) The deployed backend URL, e.g. `https://<your-domain>/api/login/oauth2/code/google`
2. Copy the generated **Client ID** and **Client Secret**.
3. Export them before starting the backend. On Windows PowerShell run:

  ```powershell
  cd ..\scripts
  ./set-google-oauth.ps1 -ClientId "<your-client-id>" -ClientSecret "<your-client-secret>"
  ```

  Use `-Persist` if you want to store them permanently via `setx`.
4. (Optional) Create a `.env` file in `santander-pulse-backend/` based on `.env.example` if you prefer to manage the variables manually.
5. Start the backend (`mvn spring-boot:run`) and verify that `GET /api/auth/providers` returns `"google": { "enabled": true, ... }`.

Once configured, successful Google sign-ins will create or update the user record and redirect to
`http://localhost:4200/oauth2/callback` carrying the JWT pair for the SPA.

### JWT Authentication Flow

1. **Login**: POST `/auth/login` with username/password
2. **Response**: Receive access token and refresh token
3. **API Calls**: Include `Authorization: Bearer <token>` header
4. **Refresh**: POST `/auth/refresh` with refresh token

### Example Login Request (CPF/Password)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "cpf": "12345678901",
    "password": "password123"
  }'
```

### Google OAuth Flow

1. Frontend redirects user to `/api/oauth2/authorization/google`
2. User authenticates with Google
3. Google redirects to `/api/login/oauth2/code/google`
4. Backend creates/updates user and generates JWT tokens
5. Backend redirects to frontend callback with tokens in URL params
6. Frontend stores tokens and completes login

## API Endpoints

### Authentication
- `POST /auth/login` - User login
- `POST /auth/register` - User registration
- `POST /auth/refresh` - Refresh access token

### Customers
- `GET /customers` - List customers (paginated)
- `GET /customers/{id}` - Get customer by ID
- `POST /customers` - Create new customer
- `PUT /customers/{id}` - Update customer
- `DELETE /customers/{id}` - Deactivate customer
- `GET /customers/stats` - Customer statistics

## Database Configuration

### H2 Console Access

- **URL**: `jdbc:h2:mem:pulsedb`
- **Username**: `pulse_user`
- **Password**: `pulse_secure_password_2025`
- **Driver**: `org.h2.Driver`

### Sample Data

Database seeding is **disabled by default** so the application boots with an empty schema ("zerada").
Enable the original demo fixtures only when needed for manual testing:

```powershell
# Windows PowerShell example
$env:PULSE_SEED_DATA_ENABLED = "true"
mvn spring-boot:run
```

```bash
# macOS/Linux example
export PULSE_SEED_DATA_ENABLED=true
mvn spring-boot:run
```

Alternatively, pass the flag directly to the JVM:

```bash
java -jar target/pulse-backend-1.0.0.jar --pulse.seed-data.enabled=true
```

When seeding is enabled the initializer creates:
- 3 default users (admin, manager, user roles)
- 4 sample customers for testing

## Security Features

### JWT Configuration
- **Algorithm**: HS512
- **Access Token**: 24 hours expiration
- **Refresh Token**: 7 days expiration
- **Secret**: Base64 encoded 512-bit key

### Banking Validations
- **CPF**: Módulo 11 algorithm validation
- **CNPJ**: Módulo 11 algorithm validation
- **Phone**: Brazilian format validation
- **Email**: RFC 5322 compliant

### Role-Based Access
- **USER**: Read customers, create/update own data
- **MANAGER**: All USER permissions + customer management
- **ADMIN**: All permissions including user management

## Development

### Project Structure

```
src/main/java/com/santander/pulse/
├── application/          # Controllers and DTOs
│   ├── controller/       # REST controllers
│   └── dto/              # Data Transfer Objects
├── domain/               # Domain entities
│   ├── User.java         # User entity
│   └── Customer.java     # Customer entity
├── infrastructure/       # Infrastructure layer
│   ├── repositories/     # Data repositories
│   ├── security/         # Security configuration
│   └── config/           # Application configuration
└── SantanderPulseApplication.java
```

### Building for Production

```bash
# Create production build
mvn clean package -Pprod

# Run with production profile
java -jar target/pulse-backend-1.0.0.jar --spring.profiles.active=prod
```

### Testing

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report

# View coverage report
open target/site/jacoco/index.html
```

## Monitoring

### Actuator Endpoints

- `/actuator/health` - Application health
- `/actuator/metrics` - Application metrics
- `/actuator/prometheus` - Prometheus metrics

### Logging

- **Format**: Structured JSON logging
- **Level**: INFO (configurable)
- **Security**: Sensitive data masked
- **Audit**: All banking operations logged

## Compliance

### Banking Standards
- CPF/CNPJ validation (Brazilian standards)
- Audit trail for all operations
- Data masking for sensitive information
- Secure password handling (BCrypt)

### Security Standards
- JWT tokens with proper expiration
- CORS configuration for frontend
- SQL injection protection (JPA)
- Input validation and sanitization

## License

Proprietary - Cezi Cola Software Engineer

---

**Santander Pulse Engineering Guild**  
*Desenvolvido por Cezi Cola Senior Software Engineer Todos os Direitos Reservados 2025*