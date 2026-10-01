# Proactive OS

Proactive OS is a personal life-tracking application. Phase 1 provides a free-form journal backed by Spring Boot, React, and PostgreSQL. AI extraction and LifeEvents are intentionally out of scope.

## Prerequisites

- Java 17
- Maven 3.9+
- Node.js 22+
- Docker Desktop

## Start the application

1. Copy `.env.example` to `.env` and adjust the PostgreSQL values only if needed.
2. Start PostgreSQL:

   ```powershell
   docker compose up -d
   ```

3. Start the backend from its Maven module:

   ```powershell
   cd backend
   mvn spring-boot:run
   ```

4. In a second terminal, start the frontend:

   ```powershell
   cd frontend
   npm run dev
   ```

Open the frontend URL shown by Vite, normally `http://localhost:5173`. It calls the backend through the Vite development proxy.

## Verify the journal

- `docker compose ps` shows PostgreSQL as healthy.
- Open `http://localhost:8080/api/health`; the response is `{"status":"UP","database":"UP"}`.
- Open the frontend, write a journal entry, save it, edit it, and delete it from the detail screen.

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