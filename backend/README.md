# OpenSourceContributor Backend Service

Initial Spring Boot skeleton service for the **OpenSourceContributor** platform.

## Architecture & Specifications

- **Java Version:** Java 21 (`<java.version>21</java.version>`)
- **Framework:** Spring Boot 3.3.2
- **Build Tool:** Apache Maven
- **Packaging:** JAR (`target/backend-0.0.1-SNAPSHOT.jar`)
- **Group ID:** `com.osc`
- **Artifact ID:** `backend`
- **Root Package:** `com.osc.backend`

## Dependencies

Strictly configured with the foundational components without Lombok or unrequested dependencies:
1. **Spring Web:** `spring-boot-starter-web` (REST endpoints, embedded Tomcat web server)
2. **Spring Data JPA:** `spring-boot-starter-data-jpa` (ORM, Hibernate persistence, HikariCP)
3. **PostgreSQL Driver:** `postgresql` (PostgreSQL JDBC connectivity, runtime scope)
4. **Validation:** `spring-boot-starter-validation` (Jakarta Validation / Hibernate Validator)
5. **Spring Boot Actuator:** `spring-boot-starter-actuator` (Operational telemetry, `/actuator/health`, `/actuator/info`)
6. **Flyway Migration:** `flyway-core`, `flyway-database-postgresql` (Automated schema migration scripts)
7. *(Testing):* `spring-boot-starter-test` (JUnit Jupiter context smoke tests, test scope)

## Project Structure

```text
backend/
├── pom.xml                                      # Maven configuration and dependencies
├── README.md                                    # Backend documentation and run instructions
├── .gitignore                                   # Ignore rules for target/, IDE, and OS files
├── src/
│   ├── main/
│   │   ├── java/com/osc/backend/
│   │   │   └── BackendApplication.java         # Spring Boot entry point (@SpringBootApplication)
│   │   └── resources/
│   │       ├── application.properties           # Server, PostgreSQL, JPA, Flyway, Actuator configuration
│   │       └── db/migration/
│   │           ├── .gitkeep                     # Directory placeholder
│   │           └── V1__create_users_table.sql   # Initial schema migration for users table
│   └── test/
│       ├── java/com/osc/backend/
│       │   └── BackendApplicationTests.java     # Context loading smoke test
│       └── resources/
│           └── application.properties           # Test profile (excludes live DB autoconfig during unit tests)
```

## How to Build and Run

### 1. Build the Application

From the `backend/` directory:

```powershell
mvn clean package -DskipTests
```

This compiles the Java 21 sources, packages the application, and produces the executable jar:
`target/backend-0.0.1-SNAPSHOT.jar` (~51 MB).

### 2. Run Database Migrations & Application

Flyway migrations execute automatically on application startup when connected to a running PostgreSQL database.

#### Setting Environment Variables and Running:

**PowerShell:**
```powershell
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/osc_db"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_secure_password"
$env:SPRING_FLYWAY_ENABLED="true"

# Run via Maven
mvn spring-boot:run

# Or run the packaged jar
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

**Bash / Linux / macOS:**
```bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/osc_db"
export DB_USERNAME="postgres"
export DB_PASSWORD="your_secure_password"
export SPRING_FLYWAY_ENABLED="true"

mvn spring-boot:run
```

### 3. Environment Variables (PostgreSQL & Flyway)

All database credentials and settings are read from environment variables; passwords are never hardcoded.

| Variable | Default Value | Description |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/osc_db` | JDBC connection URL |
| `DB_USERNAME` / `SPRING_DATASOURCE_USERNAME` | `postgres` | Database username |
| `DB_PASSWORD` / `SPRING_DATASOURCE_PASSWORD` | *(None / Required from Env)* | Database password |
| `SPRING_FLYWAY_ENABLED` | `true` | Enables/disables Flyway migrations |

### 4. Verification & Health Check

Once the application is running on port 8080:
- Health check: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
- Info endpoint: [http://localhost:8080/actuator/info](http://localhost:8080/actuator/info)
