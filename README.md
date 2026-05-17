# ATM Simulator

Spring Boot ATM simulator with a browser-based UI, REST endpoints, an optional CLI mode, and an in-memory H2 database for local development.

## Features

- ATM login with account number and PIN
- Session-based balance inquiry
- Withdraw cash
- Transfer money
- Top up stored-value services
- Change PIN
- View transaction history
- Open the app in a browser at `/`
- Browse the H2 console at `/h2-console`

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Security Crypto
- H2 Database
- Maven Wrapper

## Getting Started

### Prerequisites

- Java 21
- `JAVA_HOME` pointing to the JDK root directory

### Run the web application

```powershell
./mvnw spring-boot:run
```

Open:

- `http://localhost:8080/` for the web UI
- `http://localhost:8080/h2-console` for the database console

H2 connection details:

- JDBC URL: `jdbc:h2:mem:atmdb`
- Username: `sa`
- Password: leave blank

### Run tests

```powershell
./mvnw test
```

### Run the CLI simulator

The command-line ATM flow is disabled by default so the web app and tests can start normally.

```powershell
./mvnw spring-boot:run -Dspring-boot.run.arguments="--atm.simulator.cli.enabled=true"
```

## Sample Data

Sample accounts are created at startup by [DataInitializer](src/main/java/com/atm/atm_simulator/config/DataInitializer.java), not by `data.sql`.

- `1234567` / PIN `1234` / Taro Tanaka / balance `100000`
- `7654321` / PIN `5678` / Hanako Suzuki / balance `500000`
- `9999999` / PIN `0000` / Jiro Yamada / balance `1000000`

`src/main/resources/data.sql` is kept as a placeholder so Spring's SQL initialization remains valid, while BCrypt PIN hashes are generated in Java code.

## API Endpoints

All ATM actions go through the session-based `/api/atm` API.

- `POST /api/atm/login`
- `POST /api/atm/logout`
- `GET /api/atm/account`
- `GET /api/atm/balance`
- `POST /api/atm/withdraw`
- `POST /api/atm/transfer`
- `POST /api/atm/topup`
- `POST /api/atm/change-pin`
- `GET /api/atm/transactions`

Example login request:

```json
{
  "accountNumber": "1234567",
  "pin": "1234"
}
```

Example withdraw request:

```json
{
  "amount": 10000
}
```

## Logging

- Application logs are kept at `INFO`
- Spring and Hibernate framework logs are reduced to `WARN`
- SQL statements are hidden by default to keep local runs easier to read

## Notes

- The database is in-memory, so data resets every time the application restarts
- ATM sessions are stored in memory and are cleared when the application restarts
- This project is intended for learning and local development
