# RentWise

RentWise is a full-stack tenant review platform for sharing experiences with landlords and rental properties. It supports OTP-based sign-in out of the box, optional Google and Facebook OAuth, threaded review discussions, vote-based helpfulness signals, and property or landlord recommendation scores derived from review history.

## Stack

- Frontend: React `19.2.5`, Vite `8.0.10`, TypeScript `6.0.2`
- Backend: Spring Boot `4.0.6`, Java `25`, Spring Security, Spring Data JPA, H2

## Run locally

### Backend

```bash
cd backend
./gradlew bootRun
```

The API starts on `http://localhost:8080` and seeds sample landlords, properties, reviews, comments, and votes on first run.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

The Vite app starts on `http://localhost:5173`.

## Package for deployment

The production release artifact is a single Spring Boot jar with the React build bundled inside it.

```bash
cd frontend
npm ci

cd ../backend
./gradlew clean bootJar
```

The packaged file is written to `backend/build/libs/rent-wise.jar`.

## Auth options

- Email OTP and mobile OTP work in local development without external providers.
- Google and Facebook buttons stay disabled until OAuth credentials are configured.

For Spring Security OAuth, set standard registration variables before starting the backend:

```bash
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_ID=...
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_GOOGLE_CLIENT_SECRET=...
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_FACEBOOK_CLIENT_ID=...
export SPRING_SECURITY_OAUTH2_CLIENT_REGISTRATION_FACEBOOK_CLIENT_SECRET=...
```

Optional frontend or backend overrides:

```bash
export FRONTEND_BASE_URL=http://localhost:5173
export VITE_API_BASE_URL=http://localhost:8080/api
export VITE_BACKEND_BASE_URL=http://localhost:8080
```

## GitHub Actions

- `.github/workflows/ci.yml`: runs frontend lint or build and backend tests on every push and pull request.
- `.github/workflows/deploy.yml`: manually packages `rent-wise.jar` and deploys it to a Linux host over SSH.

Configure these repository secrets before using the deploy workflow:

- `DEPLOY_HOST`
- `DEPLOY_USER`
- `DEPLOY_SSH_KEY`
- Optional: `DEPLOY_PORT`
- Optional: `DEPLOY_DIR`

## Linux host deployment

Copy [deploy/rent-wise.env.example](/Users/zeeroiq/projects/codex/rent-wise/deploy/rent-wise.env.example:1) to `/opt/rent-wise/shared/rent-wise.env` on the target host and fill in real values. The GitHub workflow uploads the jar and then runs [scripts/deploy.sh](/Users/zeeroiq/projects/codex/rent-wise/scripts/deploy.sh:1), which places timestamped releases under `/opt/rent-wise/releases`, updates the `current.jar` symlink, restarts the app, and waits for the health check at `http://127.0.0.1:8080/api/catalog/states`.
