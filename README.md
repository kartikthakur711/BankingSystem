# BankingSystem

Production-style banking backend (incremental build) using Spring Boot, MongoDB, and JWT.

## Current Status
Implemented phases:
1. Phase 1: Auth + Users + JWT + Refresh Tokens
2. Phase 2: Accounts + Deposit/Withdraw + Transfer + Transaction History
3. Phase 3: Beneficiaries + Notifications + Monthly CSV Statement + Admin Freeze/Unfreeze
4. Phase 4: Admin listings + Dockerized runtime + Postman collection

## Stack
- Java 21
- Spring Boot 3.5.11
- Spring Security (JWT)
- Spring Data MongoDB
- Bean Validation
- OpenAPI/Swagger
- Maven

## Project Structure
`src/main/java/com/kartik/bankingsystem`
- `config` -> security, auditing, bootstrap admin
- `controller` -> REST APIs
- `dto` -> request/response contracts
- `entity` -> MongoDB documents
- `exception` -> global error handling
- `repository` -> Mongo repositories
- `security` -> JWT + auth filter + user details
- `service` -> business logic

## Data Collections
- `users`
- `refresh_tokens`
- `accounts`
- `transactions`
- `beneficiaries`
- `notifications`

## Run Locally
1. Start MongoDB:
   - `mongodb://localhost:27017/banking_system`
2. Set env vars (optional):
   - `MONGODB_URI` (default: `mongodb://localhost:27017/banking_system`)
   - `JWT_SECRET`
   - `JWT_ACCESS_EXPIRATION_MS`
   - `JWT_REFRESH_EXPIRATION_MS`
   - `BOOTSTRAP_ADMIN_ENABLED` (default: `true`)
   - `BOOTSTRAP_ADMIN_FULL_NAME` (default: `Platform Admin`)
   - `BOOTSTRAP_ADMIN_EMAIL` (default: `admin@banking.local`)
   - `BOOTSTRAP_ADMIN_PASSWORD` (default: `Admin@12345`)
3. Start app:
   - `mvn spring-boot:run`

## Compile Check
- `mvn -DskipTests compile`

## Swagger
- [Swagger UI](http://localhost:8080/swagger-ui/index.html)

## API Base
- `/api/v1`

## Endpoints

### Auth
- `POST /auth/register`
- `POST /auth/login`
- `POST /auth/refresh`

### User
- `GET /users/me` (Bearer token required)

### Accounts (Bearer token required)
- `POST /accounts`
- `GET /accounts`
- `POST /accounts/{accountNumber}/deposit`
- `POST /accounts/{accountNumber}/withdraw`
- `POST /accounts/transfer`
- `GET /accounts/{accountNumber}/transactions?page=0&size=20`
- `GET /accounts/{accountNumber}/statement?year=2026&month=2` (CSV download)
- `GET /accounts/{accountNumber}/statement/pdf?year=2026&month=2` (PDF download)

### Beneficiaries (Bearer token required)
- `POST /beneficiaries`
- `GET /beneficiaries`
- `DELETE /beneficiaries/{beneficiaryId}`

### Notifications (Bearer token required)
- `GET /notifications?page=0&size=20`

### Admin (ADMIN role required)
- `PATCH /accounts/{accountNumber}/status`
- `GET /admin/users?email=<query>`
- `GET /admin/accounts?accountNumber=<query>`

## Working Flow (How to Test)
1. Login as admin:
   - `POST /api/v1/auth/login`
   - default admin: `admin@banking.local` / `Admin@12345`
2. Copy `accessToken`.
3. Authorize in Swagger with `Bearer <accessToken>`.
4. Create account: `POST /api/v1/accounts`.
5. Add beneficiary: `POST /api/v1/beneficiaries`.
6. Deposit/Withdraw from account.
7. Transfer to beneficiary account.
8. Check transactions and notifications.
9. Download monthly statement CSV.
10. (Admin) freeze/unfreeze account.

## Important Business Rules
1. Transfer destination must exist in beneficiary list.
2. Withdrawal/transfer blocked on insufficient balance.
3. Inactive accounts cannot be used.
4. Transfer supports idempotency key check.
5. Access token is short-lived; refresh token is persisted.

## Sample Requests

### Register
```bash
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{
    "fullName": "Kartik",
    "email": "kartik@example.com",
    "password": "Password@123"
  }'
```

### Login
```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{
    "email": "admin@banking.local",
    "password": "Admin@12345"
  }'
```

### Create Account
```bash
curl -X POST http://localhost:8080/api/v1/accounts \
  -H 'Content-Type: application/json' \
  -H 'Authorization: Bearer <accessToken>' \
  -d '{
    "accountType": "SAVINGS",
    "initialDeposit": 1000
  }'
```

## Next Planned Phase
1. Integration tests for complete happy path + failure path
2. Statement PDF generation
3. Beneficiary ownership + business rule extensions
4. Audit/event logging improvements

## Docker Run
1. Build jar:
   - `mvn -DskipTests package`
2. Start containers:
   - `docker compose up --build`
3. App:
   - `http://localhost:8080`

## Postman
- Collection file: `postman/BankingSystem.postman_collection.json`

## Integration Tests
- Testcontainers-based auth flow integration test:
  - `src/test/java/com/kartik/bankingsystem/integration/AuthFlowIntegrationTest.java`
- Run tests:
  - `mvn test`
