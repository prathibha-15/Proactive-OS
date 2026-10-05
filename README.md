# Proactive OS

Proactive OS is a personal life-tracking application with private journals, structured life events, deterministic insights and recommendations, and a development-only external-data foundation, backed by Spring Boot, React, and PostgreSQL.

## Prerequisites

- Java 21
- Maven 3.9+
- Node.js 22+
- Docker Desktop

## Start the application

1. Copy `.env.example` to `.env`, set `PROACTIVEOS_SECURITY_JWT_SECRET` to a private random value of at least 32 bytes, and adjust PostgreSQL values only if needed.
2. Start PostgreSQL:

   ```powershell
   docker compose up -d
   ```

3. Start the backend from its Maven module:

   ```powershell
   cd backend
   $env:PROACTIVEOS_SECURITY_JWT_SECRET = "your-private-random-secret-of-at-least-32-bytes"
   $env:AI_TIME_ZONE = "UTC"
   mvn spring-boot:run
   ```

4. In a second terminal, start the frontend:

   ```powershell
   cd frontend
   Copy-Item .env.example .env.local
   npm run dev
   ```

Open the frontend URL shown by Vite, normally `http://localhost:5173`. It calls the backend through the Vite development proxy.

## Verify the journal

- `docker compose ps` shows PostgreSQL as healthy.
- Open `http://localhost:8080/api/health`; the response is `{"status":"UP","database":"UP"}`.
- Open the frontend, create an account or sign in, then write a journal entry, edit it, and delete it from the detail screen.
- Existing journals and life events from before Phase 7 remain without an owner and are intentionally unavailable to accounts until explicitly backfilled.

## Authentication API

- `POST /api/auth/register`: create an account with `{ "email": "user@example.com", "password": "at-least-8-characters" }`.
- `POST /api/auth/login`: exchange credentials for a one-hour bearer access token.
- Journal, life-event, extraction, and insights endpoints require `Authorization: Bearer <accessToken>`.
- `GET /api/health` remains public. Authentication is stateless; no refresh tokens are issued.

## Event Times

- Journal clock times are interpreted in `AI_TIME_ZONE` and stored as UTC instants in the existing `eventTime` field. The default timezone is `UTC`.
- Set `VITE_APP_TIME_ZONE` in `frontend/.env.local` to the same value as `AI_TIME_ZONE`; the Events date filter and displayed event times use that timezone.
- Times without a reliable clock value remain unknown. Sleep onset can be the event time; wake-time details are retained in notes, and sleep duration is not inferred.

## External Data

- `GET /api/integrations` lists available providers and the signed-in user's last-sync time.
- `POST /api/integrations/MOCK/sync` imports deterministic synthetic Steps, Workout, and Sleep records as `DEVICE` events.
- The mock is development/test data only; there is no real health account connection, OAuth flow, or credential storage.
- External identities are deduplicated by owner, provider, and external record ID. Events keep JOURNAL, MANUAL, and DEVICE observations separate.
- Hibernate `ddl-auto: update` adds the nullable event identity columns and per-user sync-state table; this repository does not use versioned database migrations.

## External Data Foundation

- `GET /api/integrations` returns the current user's available providers and last-sync time.
- `POST /api/integrations/MOCK/sync` imports deterministic synthetic Steps, Workout, and Sleep events as `DEVICE` data.
- The mock provider is for development/testing only; there is no real health-provider connection or OAuth/token storage in this phase.
- External records retain provider identity and are deduplicated by owner, provider, and external record ID. Journal, Manual, and Device observations remain separate.
- Hibernate `ddl-auto: update` manages the added nullable event identity fields and per-user last-sync state table; this repository does not currently use a versioned migration framework.

## Journal API

- `POST /api/journals`: create an entry with `{ "content": "..." }`.
- `GET /api/journals`: list entries, newest first.
- `GET /api/journals/{id}`: retrieve one entry.
- `PUT /api/journals/{id}`: replace the content of one entry.
- `DELETE /api/journals/{id}`: delete one entry.

## Structure

- `backend`: modular Spring Boot monolith. `health` and `journal` are independent feature modules under `com.proactiveos`.
- `frontend`: Vite React TypeScript application. `src/api/journalApi.ts` owns Journal HTTP requests; routed UI screens compose and display entries.
- `docker-compose.yml`: local PostgreSQL only.