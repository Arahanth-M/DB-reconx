# Day 5 — Security & REST Controllers Implementation

This document provides a summary of all tickets and tasks implemented for **Day 5** in the ReconX platform repository.

---

## 📋 Implemented Tickets Summary

| Ticket ID | Component / Class | Endpoint / Description | Status |
|---|---|---|---|
| **TICKET-ADV072** | `JwtTokenProvider.java` | Generates & parses HS256-signed JWTs using JJWT 0.12.x. Standardized payload with subject = user email, issuer, expiration, and role claim. | ✅ Implemented |
| **TICKET-ADV072** | `AuthController.java` | `POST /auth/login` — Verifies user credentials against BCrypt password hashes in `AppUserRepository` and issues JWT tokens wrapped in `LoginResponse`. | ✅ Implemented |
| **TICKET-ADV073** | `JwtAuthenticationFilter.java` | Extends `OncePerRequestFilter` to extract `Authorization: Bearer <token>`, validate claims, and populate `SecurityContextHolder` with `ROLE_<role>`. | ✅ Implemented |
| **TICKET-ADV074** | `SecurityConfig.java` | Configures Spring Security with stateless session management (`STATELESS`), CSRF disabled, `@EnableMethodSecurity`, path matchers for permitAll vs role-based rules, and registers `JwtAuthenticationFilter` before `UsernamePasswordAuthenticationFilter`. | ✅ Implemented |
| **TICKET-ADV063** | `TradeController.java` | `GET /v1/trades` — List trades with dynamic filter parameters (`from`, `to`, `status`, `counterpartyId`) and pagination via `PagedResponse`. | ✅ Implemented |
| **TICKET-ADV064** | `TradeController.java` | `POST /v1/trades` — Create trade endpoint delegating to `TradeService.create(req, actor)`, returning `201 Created` with a `Location` header. | ✅ Implemented |
| **TICKET-ADV065** | `TradeController.java` | `PUT /v1/trades/{id}` — Full trade update endpoint delegating to `TradeService.update(...)`. | ✅ Implemented |
| **TICKET-ADV066** | `TradeController.java` | `PATCH /v1/trades/{id}/status` — Status-only update endpoint delegating to `TradeService.updateStatus(...)`. | ✅ Implemented |
| **TICKET-ADV067** | `TradeController.java` | `DELETE /v1/trades/{id}` — Soft delete endpoint delegating to `TradeService.softDelete(...)`, returning `204 No Content`. | ✅ Implemented |
| **TICKET-ADV068** | `ReconController.java` | `POST /v1/recon/run` — Async recon run trigger returning `202 Accepted` with a generated `jobId` and `QUEUED` status. | ✅ Implemented |
| **TICKET-ADV069** | `ReconController.java` | `GET /v1/recon/jobs/{jobId}/results` — Returns reconciliation break results for a given job. | ✅ Implemented |
| **TICKET-ADV070** | `ReconController.java` | `PUT /v1/recon/results/{id}/resolve` — Marks a `ReconBreak` as `RESOLVED` with optional resolution notes and timestamp. | ✅ Implemented |
| **TICKET-ADV071** | `AuditController.java` | `GET /v1/audit/trades/{tradeRef}` — Fetches complete audit history log entries ordered by timestamp for a given trade reference. | ✅ Implemented |
| **TICKET-ADV138** | `AuditController.java` | `GET /v1/audit/trades/{tradeRef}/events` — Stream/list of Kafka-sourced event log entries for a trade. | ✅ Implemented |

---

## 🔒 Security Architecture Overview

### JWT Token Flow
1. Client sends `POST /auth/login` with `{"email": "...", "password": "..."}`.
2. Server verifies BCrypt password hash against `users` table via `AppUserRepository`.
3. On success, `JwtTokenProvider.generate(email, role)` creates an HS256-signed JWT containing:
   - `sub` = user email
   - `iss` = configured issuer
   - `iat` / `exp` = issued-at and expiration timestamps
   - `role` = custom claim (e.g., `ADMIN`, `TRADER`, `VIEWER`, `RECON_ANALYST`)
4. Client receives `LoginResponse` with `token`, `tokenType: "Bearer"`, `expiresInSeconds`, and `role`.

### Security Filter Chain (TICKET-ADV073 + ADV074)
- **Session**: `STATELESS` — no HTTP sessions; every request carries its own credential.
- **CSRF**: Disabled (safe for a stateless JWT API).
- **JwtAuthenticationFilter**: Runs before `UsernamePasswordAuthenticationFilter`, populates `SecurityContextHolder`.
- **@EnableMethodSecurity**: Enables `@PreAuthorize` on service methods.

### RBAC Endpoint Mapping

| Path | Allowed Roles |
|---|---|
| `/auth/login`, `/actuator/health/**`, `/actuator/info`, `/actuator/prometheus`, `/swagger-ui/**`, `/v3/api-docs/**`, `/h2/**` | `permitAll` |
| `GET /v1/trades/**` | `VIEWER`, `TRADER`, `RECON_ANALYST`, `ADMIN` |
| `POST /v1/trades` | `TRADER`, `ADMIN` |
| `PUT /v1/trades/**`, `PATCH /v1/trades/**` | `TRADER`, `ADMIN` |
| `DELETE /v1/trades/**` | `ADMIN` only |
| `/v1/recon/**` | `RECON_ANALYST`, `ADMIN` |
| `/v1/audit/**` | `RECON_ANALYST`, `ADMIN` |
| Everything else | `authenticated` |

---

## 🗂️ Modified Files

### Security Layer
| File | Ticket(s) |
|---|---|
| `backend/src/main/java/com/dbtraining/reconx/security/JwtTokenProvider.java` | ADV072 |
| `backend/src/main/java/com/dbtraining/reconx/security/JwtAuthenticationFilter.java` | ADV073 |
| `backend/src/main/java/com/dbtraining/reconx/security/SecurityConfig.java` | ADV073, ADV074 |

### REST Controllers
| File | Ticket(s) |
|---|---|
| `backend/src/main/java/com/dbtraining/reconx/controller/AuthController.java` | ADV072 |
| `backend/src/main/java/com/dbtraining/reconx/controller/TradeController.java` | ADV063, ADV064, ADV065, ADV066, ADV067 |
| `backend/src/main/java/com/dbtraining/reconx/controller/ReconController.java` | ADV068, ADV069, ADV070 |
| `backend/src/main/java/com/dbtraining/reconx/controller/AuditController.java` | ADV071, ADV138 |

---

## 🚀 How to Run & Verify

### Build
```bash
cd backend
./mvnw clean compile
```

### Run
```bash
./mvnw spring-boot:run
```
Wait for `Started ReconxApplication in ~4 seconds`.

### Verify Security Enforcement
```bash
# Public endpoint — should return 200
curl -i http://localhost:8081/api/actuator/health

# Protected endpoint — anonymous should get 403
curl -i http://localhost:8081/api/v1/trades
```

### Obtain JWT & Access Protected Resources
```bash
# Login to get a JWT
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@reconx.local","password":"password"}' | jq -r .token)

# Use the token
curl -i -H "Authorization: Bearer $TOKEN" http://localhost:8081/api/v1/trades
```

---

## ⚠️ Notes
- Test failures in `ReconciliationEngineTest` are **pre-existing** from Day 3 (TICKET-ADV018 not yet implemented) and are unrelated to Day 5 changes.
- The `ReconciliationIntegrationTest` requires Docker (Testcontainers) and will fail without a running Docker daemon.
- The `./mvnw clean compile` build succeeds with zero errors for all Day 5 code.
