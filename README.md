# ATM Simulator

Spring Boot ATM simulator with a browser-based UI, REST endpoints, and an in-memory H2 database for local development.

## Features

- Check account balance
- Deposit and withdraw money
- View transaction history
- Open the app in a browser at `/`
- Browse the H2 console at `/h2-console`
- Optional command-line ATM mode controlled by configuration

## Tech Stack

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
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

The application loads two sample accounts from `src/main/resources/data.sql`:

- Account ID `1`, account number `1234567`
- Account ID `2`, account number `7654321`

## Available Endpoints

- `GET /api/accounts/{id}/balance`
- `POST /api/accounts/{id}/deposit`
- `POST /api/accounts/{id}/withdraw`
- `GET /api/accounts/{id}/transactions`

Example request body for deposit and withdraw:

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
- This project is intended for learning and local development
