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
