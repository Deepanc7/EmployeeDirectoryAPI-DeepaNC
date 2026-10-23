# Employee Directory API

Spring Boot 3 REST API for an internal employee directory: **departments**, **employees** and
**skills** (employee ↔ skill is many-to-many through the `employee_skill` join table), with
filtered lookups for HR / team-management tooling.

* Java 17, Spring Boot 3.3, Spring Data JPA (Hibernate), in-memory **H2**
* Bean Validation, global exception handler, pagination + sorting
* Unit tests (JUnit 5 + Mockito) and integration tests (`@SpringBootTest` + MockMvc)
* Hand-written OpenAPI 3 spec (`openapi.yaml`) rendered by Swagger UI

## Prerequisites

* JDK 17+
* Maven 3.9+ (no external database needed)

## Setup Instructions

### 1. Prerequisites

For running locally, install:

- **JDK 17 or later** (`java -version`)
- **Apache Maven 3.9 or later** (`mvn -version`)

No external database is required. The application uses an in-memory H2 database.

### 2. Extract and open the project

Extract the ZIP file and open a terminal in the project root, the directory containing `pom.xml`.

```bash
cd EmployeeDirectoryAPI-DeepaNC
```

### 3. Run the application

Start the API with Maven:

```bash
mvn spring-boot:run
```

The application is configured for HTTPS on port `8443`. Open:

- API base URL: `https://localhost:8443`
- Swagger UI: `https://localhost:8443/swagger-ui.html`
- OpenAPI specification: `https://localhost:8443/openapi.yaml`
- Application information: `https://localhost:8443/api/info`

The project includes a development PKCS#12 keystore for local HTTPS. Because it is self-signed, your browser or HTTP client may show a certificate warning during local testing.

Example request with `curl` (the `-k` flag skips verification of the local self-signed certificate):

```bash
curl -k https://localhost:8443/api/info
```

### 4. Run tests

Run the automated unit and integration tests:

```bash
mvn test
```

To build the executable JAR:

```bash
mvn clean package
```

Then run it with:

```bash
java -jar target/employee-directory-1.0.0.jar
```

### 5. Run with Docker (optional)

If Docker and the Docker Compose plugin are installed, build and start the service:

```bash
docker compose up --build
```

The API will be available at `https://localhost:8443`. Stop the service with `Ctrl+C`, or run `docker compose down` in another terminal.

### 6. Database

H2 runs in memory and is initialized automatically when the application starts. No database installation or separate migration step is needed. Data is cleared when the application stops.

## Run the tests

```bash
mvn test
```

* `service/*Test` – unit tests, repositories mocked with Mockito
* `util/PaginationSupportTest` – paging/sorting rules
* `controller/EmployeeDirectoryIntegrationTest` – full stack against H2

## Configuration (`src/main/resources/application.properties`)

| Property | Default | Used for |
|---|---|---|
| `app.name` / `app.version` | `Employee Directory Service` / `1.0.0` | `GET /api/info` |
| `directory.max-skills-per-employee` | `10` | Rejects (HTTP 422) adding/assigning more skills than this |
| `pagination.default-page-size` | `20` | Page size when `size` is not supplied |
| `pagination.max-page-size` | `100` | Upper bound for `size` |

Values are bound to typed beans (`AppProperties`, `DirectoryProperties`, `PaginationProperties`
via `@ConfigurationProperties`) and injected into the services/controllers — nothing is hard-coded.
Override at runtime, e.g. `java -jar app.jar --directory.max-skills-per-employee=3` or
the env var `DIRECTORY_MAXSKILLSPEREMPLOYEE=3`.

## Project layout

```
com.example.directory
├── config       typed @ConfigurationProperties
├── controller   thin REST controllers
├── service      business rules + transactions
├── repository   Spring Data repos, employee Specifications
├── domain       JPA entities (Department, Employee, Skill)
├── dto          request/response records (with validation annotations)
├── exception    custom exceptions + GlobalExceptionHandler
└── util         PaginationSupport
```

## Behaviour notes

* **List endpoints are paged** and return
  `{content, page, size, totalElements, totalPages, first, last}`.
  Query params: `page` (0-based), `size`, `sort=field[,asc|desc]` (whitelisted fields; bad values → 400).
* **`headCount`** on a department is computed from the employees table (one grouped query per page).
* **Employee search** `GET /api/employees` filters (all optional, AND-ed):
  `departmentId`, `skillId`, `skill` (name), `designation`, `name` (contains),
  `joinedAfter`, `joinedBefore` (ISO dates).
* **Update employee** (`PUT`): if `skillIds` is omitted the skills stay as they are; if provided they are replaced.
* **Deleting** a department that still has employees, or a skill still assigned to someone → **409**.
* Emails are stored lower-cased and are unique case-insensitively. Department and skill names are unique case-insensitively.
* Beyond the required endpoints, skills also support `GET/PUT/DELETE /api/skills/{id}` and a `category` filter.

### Error format

All errors share one shape:

```json
{
  "timestamp": "2025-01-15T10:15:30.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/employees",
  "fieldErrors": [ { "field": "email", "message": "email must be a valid address" } ]
}
```

| Status | When |
|---|---|
| 400 | Bean Validation failure, malformed JSON/date, bad paging or sort parameter |
| 404 | Employee / department / skill (or a referenced id) not found |
| 409 | Duplicate name/email, duplicate skill on employee, delete blocked by references |
| 422 | Max skills per employee exceeded |
| 500 | Unexpected error (details logged, not leaked) |

## Sample requests

```bash
# 1. Departments
curl -s -X POST localhost:8080/api/departments -H 'Content-Type: application/json' \
  -d '{"name":"Engineering","location":"Bengaluru"}'
curl -s localhost:8080/api/departments
curl -s localhost:8080/api/departments/1
curl -s -X PUT localhost:8080/api/departments/1 -H 'Content-Type: application/json' \
  -d '{"name":"Engineering","location":"Hyderabad"}'

# 2. Skills
curl -s -X POST localhost:8080/api/skills -H 'Content-Type: application/json' \
  -d '{"name":"Java","category":"Backend"}'
curl -s -X POST localhost:8080/api/skills -H 'Content-Type: application/json' \
  -d '{"name":"SQL","category":"Data"}'
curl -s "localhost:8080/api/skills?category=backend"

# 3. Employee with skills
curl -s -X POST localhost:8080/api/employees -H 'Content-Type: application/json' -d '{
  "name":"Asha Rao","email":"asha.rao@corp.com","designation":"Engineer",
  "joiningDate":"2023-04-01","departmentId":1,"skillIds":[1]}'
curl -s localhost:8080/api/employees/1

# 4. Add / remove a skill
curl -s -X POST   localhost:8080/api/employees/1/skills/2
curl -s -X DELETE localhost:8080/api/employees/1/skills/2

# 5. Filtered lookups
curl -s "localhost:8080/api/employees?departmentId=1"
curl -s "localhost:8080/api/employees?skill=java&designation=engineer"
curl -s "localhost:8080/api/employees?joinedAfter=2023-01-01&sort=joiningDate,desc&size=10"
curl -s localhost:8080/api/departments/1/employees

# 6. Update / delete employee
curl -s -X PUT localhost:8080/api/employees/1 -H 'Content-Type: application/json' -d '{
  "name":"Asha Rao","email":"asha.rao@corp.com","designation":"Senior Engineer",
  "joiningDate":"2023-04-01","departmentId":1}'
curl -s -X DELETE localhost:8080/api/employees/1 -i

# 7. Config-driven endpoint and an error example
curl -s localhost:8080/api/info
curl -s localhost:8080/api/employees/9999
```

Note: H2 is in-memory, so data is lost on restart (ids restart at 1, matching the examples above).
