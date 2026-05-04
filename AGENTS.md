# Repository Guidelines

## Project Structure & Module Organization
RentWise is a full-stack monorepo with a clear separation: `backend/` contains the Spring Boot API (Java 25), and `frontend/` contains the React application (TypeScript 6, Vite 8). Deployment merges both into a single `rent-wise.jar`.

- `backend/src/main/java/com/rentwise/` - Spring Boot application code (services, controllers, entities, repositories)
  - `backend/src/main/java/com/rentwise/backend/auth/` - Authentication and authorization (`AuthProvider`, `RentwisePrincipal`)
  - `backend/src/main/java/com/rentwise/backend/config/` - Spring configuration (`SecurityConfig`, `DataInitializer`)
  - `backend/src/main/java/com/rentwise/backend/landlord/` - Landlord domain (entity, repository)
  - `backend/src/main/java/com/rentwise/backend/property/` - Property domain (entity, repository)
  - `backend/src/main/java/com/rentwise/backend/review/` - Review domain (entities, repositories)
  - `backend/src/main/java/com/rentwise/backend/location/` - Location management (Country, State, City entities, repositories, services, DTOs)
  - `backend/src/main/java/com/rentwise/backend/user/` - User/AppUser domain (entity, repository)
  - `backend/src/main/java/com/rentwise/backend/web/` - Web layer (controllers, services, DTOs)
- `backend/src/test/java/com/rentwise/` - JUnit and Spring Security tests with Gradle
- `frontend/src/` - React components (`App.tsx`, `main.tsx`) and API integration (`api.ts`, `types.ts`)
- `deploy/` - Configuration templates like `rent-wise.env.example`
- `scripts/` - Deployment automation (`deploy.sh`) for Linux hosts with nginx and SSL
- `docs/` - Design notes and ADRs

Backend follows standard Spring Boot conventions (`PascalCase` for entities and services, `*Dto` naming for DTOs, `*Command` for command objects). Frontend uses `PascalCase` for React components and `kebab-case` for static assets.

## Build, Test, and Development Commands
Development and testing are configured per-module:

**Frontend** (React + Vite):
- `cd frontend && npm run dev` - Start Vite dev server on `http://localhost:5173` with hot module reload
- `cd frontend && npm run lint` - Run ESLint for TypeScript and React
- `cd frontend && npm run build` - Build optimized React bundle to `frontend/dist/`

**Backend** (Spring Boot + Gradle):
- `cd backend && ./gradlew bootRun` - Start Spring Boot on `http://localhost:8080`; seeds sample data on first run and mounts H2 console at `/h2-console`
- `cd backend && ./gradlew test` - Run full JUnit test suite with Spring Security tests
- `cd backend && ./gradlew clean bootJar` - Build production jar to `backend/build/libs/rent-wise.jar` (including bundled React build)

**Production Packaging**:
Frontend build is integrated into the backend bootJar process automatically via a Gradle `bundleFrontend` task. To package for deployment, `cd frontend && npm ci && cd ../backend && ./gradlew clean bootJar` creates a single self-contained jar that serves the React app and API from one process.

**CI/CD**:
GitHub Actions workflows run on every push and pull request. See `.github/workflows/ci.yml` for frontend lint/build and backend tests. The deploy workflow runs manually and packages the jar to a Linux host via SSH.

## Coding Style & Naming Conventions
**Backend (Java)**:
- Use 4-space indentation
- Entity and service classes: `PascalCase` (e.g., `Landlord`, `Property`, `ReviewService`)
- DTOs: `PascalCase` with `Dto` suffix (e.g., `LandlordDto`, `PropertyCardDto`)
- Package structure follows domain: `com.rentwise.{controller,service,entity,repository,dto}`
- Use Spring Bean naming conventions (`@Service`, `@Repository`, `@Controller`)

**Frontend (TypeScript + React)**:
- Use 2-space indentation in config files (JSON, YAML, Markdown)
- React components: `PascalCase.tsx` (e.g., `App.tsx`, `main.tsx`)
- TypeScript types: `PascalCase` (defined in `types.ts` for shared types)
- API integration: centralized in `api.ts` with typed request/response classes
- Use ESLint configuration in `eslint.config.js` and run via `npm run lint`

**Environment Variables**:
- Spring Boot: `SPRING_*` prefix (e.g., `SPRING_DATASOURCE_URL`, `SPRING_PROFILES_ACTIVE`)
- Frontend URLs: `FRONTEND_BASE_URL`, `VITE_API_BASE_URL`, `VITE_BACKEND_BASE_URL`
- OAuth: `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_{GOOGLE,FACEBOOK}_*`
- See `deploy/rent-wise.env.example` for production template

## Testing Guidelines
**Backend**: JUnit tests live in `backend/src/test/java/` and mirror the source structure. Run via `cd backend && ./gradlew test`. Include Spring Security tests and integration tests for cross-service behavior. Use Spring Boot Test annotations (`@SpringBootTest`, `@WebMvcTest`) for context.

**Frontend**: ESLint configuration enforces type safety and React patterns. Run linting via `cd frontend && npm run lint`. Full build verification occurs in CI. Keep lightweight unit tests for utility functions; integration testing happens in the browser during development with Vite.

**CI/CD**: The `test` task runs automatically on every push and pull request via GitHub Actions (`.github/workflows/ci.yml`). Frontend runs lint and build; backend runs the full Gradle test suite. Ensure the full suite runs from the documented commands before pushing.

## Commit & Pull Request Guidelines
Git history is not available in this workspace, so use short, imperative commit subjects and keep them scoped, for example `feat: add CLI bootstrap` or `fix: reject empty config`. Pull requests should include a brief summary, the reason for the change, test evidence, linked issues, and screenshots when UI output changes.

## Security & Configuration Tips
Do not commit secrets, local credentials, or machine-specific files. Commit a template such as `deploy/rent-wise.env.example` when configuration is needed, and keep generated artifacts (build outputs, `node_modules/`, `.gradle/`) out of version control.

**Admin-Level Access**:
- The `AppUser` entity includes an `isAdmin` boolean flag; default is `false`
- Admin status is exposed via `RentwisePrincipal.isAdmin()` in the authentication principal
- Location management endpoints (`/api/admin/locations/**`) are protected at both the filter level (authentication required for POST/DELETE) and method level (admin check via `LocationController.requireAdmin()`)
- Admin users are seeded during startup via `DataInitializer`; promotes users to admin by passing `true` to the `AppUser` constructor

**Location Management Endpoints** (all under `/api/admin/locations`):
- `GET /countries` - List all countries (public read)
- `GET /countries/{countryId}` - Get country details (public read)
- `POST /countries` - Create country (admin-only)
- `DELETE /countries/{countryId}` - Delete country (admin-only, fails if states exist)
- `GET /countries/{countryId}/states` - List states by country (public read)
- `POST /states` - Create state (admin-only)
- `GET /states/{stateId}/cities` - List cities by state (public read)
- `POST /cities` - Create city (admin-only)
- `DELETE /states/{stateId}` and `DELETE /cities/{cityId}` - Delete state/city (admin-only, fails if children exist)

**Local Development**:
- OAuth buttons in the frontend remain disabled until real credentials are provided via environment variables
- OTP codes are exposed in dev mode (see `app.otp.expose-dev-code: true` in `backend/src/main/resources/application.yml`)
- H2 database runs in-memory with file persistence at `data/rentwise.mv.db`
- Admin user (email: `admin@example.com`) is created on first run for testing location endpoints

**Production Deployment**:
- Copy `deploy/rent-wise.env.example` to `/opt/rent-wise/shared/rent-wise.env` on the target host and populate real values
- The deployment script (`scripts/deploy.sh`) handles nginx reverse proxy, SSL via certbot, and process management
- Switch from H2 to a managed database (PostgreSQL recommended) by setting `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- Set `SPRING_PROFILES_ACTIVE=prod` to disable dev features like OTP exposure and H2 console
- Configure OAuth credentials for Google and Facebook via `SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_*` environment variables
- Promote users to admin status by updating the `is_admin` column in the `app_users` table or through an admin API (implementation pending)
