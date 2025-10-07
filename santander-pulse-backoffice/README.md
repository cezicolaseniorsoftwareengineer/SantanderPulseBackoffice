# Sant## Technology Stacknder Pulse – Backoffice Frontend

Angular 18 single-page application that powers the Santander Pulse bac- Automated accessibility testing (axe-core).

---

Built with care using Santander's internal design language and CPF validators compliant with ABNT NBR 9892:1987.
Built with care using Santander's internal design language and CPF validators compliant with ABNT NBR 9892:1987.uilt with care using Santander's internal design language and CPF validators compliant with ABNT NBR 9892:1987.uilt with care using Santander's internal design language and CPF validators compliant with ABNT NBR 9892:1987.fice. The app now integrates with the Spring Boot backend (`santander-pulse-backend`) using JWT authentication, CPF-based login/registration and optional Google OAuth 2.0 sign-in.

---

## Technology Stack

- **Angular 18.2** with standalone components and RxJS 7
- **TypeScript 5** in strict mode
- **SCSS** with Santander design tokens (`src/assets/styles/_variables.scss`)
- **Pulse UI Library** (`projects/ui-lib`) for buttons, inputs, table and validators
- **Spring Boot 3 backend** for authentication, refresh tokens and customer data

---

## Authentication Flows

1. **CPF + Password**
  - Existing users sign in with CPF (numbers only) and password.
  - New users start in the "Criar senha" tab: inform CPF, name, email and define a password. A JWT pair is issued on success.

2. **Google OAuth 2.0**
  - Clicking “Entrar com Google” redirects to Google.
  - After consent, the backend provisions/updates the user and redirects to `/oauth2/callback` with access & refresh tokens.

Tokens are stored in `localStorage` via `AuthService`, and protected API calls automatically add the bearer token through `SecurityInterceptor`.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Node.js | 22.20.0+
| npm | 10.9.3+
| Angular CLI | 18.2.21 (optional, for `ng` commands)
| Java | 21 (to run the backend)
| Maven | 3.9+

---

## Environment Variables

The frontend reads static configuration from `src/environments/environment.ts`. Make sure the following backend variables are configured before starting Spring Boot:

| Backend property | Description |
|------------------|-------------|
| `app.jwt.secret` | Secret used to sign JWTs (already set in `application.yml` for local use). |
| `spring.security.oauth2.client.registration.google.client-id` | Google client ID (export via `set-google-oauth.ps1`). |
| `spring.security.oauth2.client.registration.google.client-secret` | Google client secret (export via `set-google-oauth.ps1`). |
| `app.frontend-url` | SPA base URL (defaults to `http://localhost:4200`). |

> Create OAuth credentials in the Google Cloud console (Web application) and whitelist `http://localhost:8080/api/login/oauth2/code/google` as the Authorized redirect URI. You can set the environment variables by running `../scripts/set-google-oauth.ps1 -ClientId "..." -ClientSecret "..."` before starting the backend.

---

## Running the Full Stack Locally

1. **Backend**
  ```powershell
  cd santander-pulse-backend
  mvn spring-boot:run
  ```

2. **Frontend** (new terminal)
  ```powershell
  cd santander-pulse-backoffice
  npm install
  npm start
  ```

3. Navigate to `http://localhost:4200/` and use one of the seeded users (see backend README) or create a new one via the “Criar senha” tab.

---

## Project Highlights

- `src/app/features/auth/pages/login.page.ts` – CPF login, password creation and Google button.
- `src/app/features/auth/pages/oauth-callback.page.ts` – Handles the OAuth redirect, persists tokens and routes to `/`.
- `src/app/core/interceptors/security.interceptor.ts` – Automatically appends JWT tokens to API requests.
- `src/app/core/services/notification.service.ts` – Toast notifications for success/error states.
- `projects/ui-lib/src/lib` – Pulse component library used across the app.

---

## Quality Checks

```powershell
npm run lint     # Angular ESLint
npm run test     # Karma/Jasmine unit tests
npm run build    # Production build (outputs to dist/)
```

For backend validation:

```powershell
cd santander-pulse-backend
mvn test
```

---

## Production Build

```powershell
cd santander-pulse-backoffice
npm run build
```

Artifacts will be generated in `dist/santander-pulse-backoffice/`. Deploy alongside the Spring Boot API or configure a reverse proxy pointing `/api` to the backend base URL.

---

## Refresh Tokens

The backend issues both access and refresh tokens. `AuthService` automatically:

- Stores `accessToken`, `refreshToken` and `user` metadata in `localStorage`.
- Refreshes the access token on login/register/OAuth.
- Clears tokens on logout and redirects to the login page.

---

## Roadmap & Ideas

- Customer CRUD backed by Spring Boot endpoints (in progress).
- Persisted sessions across browser tabs using BroadcastChannel.
- E2E coverage with Cypress for auth flows.
- Automated accessibility testing (axe-core).

---

Built with ❤️ using Santander’s internal design language and CPF validators compliant with ABNT NBR 9892:1987.
