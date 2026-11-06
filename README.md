# Users Service — DevSecOps Learning Project

A small Spring Boot REST API used as the application for the DevSecOps pipeline.

## Technology
- Java 21
- Spring Boot 3.5.0
- Spring MVC
- Spring Data JPA
- H2 in-memory database
- Jakarta Bean Validation
- Maven

## Run locally

Make sure Java 21 and Maven are installed, then run:

```bash
mvn spring-boot:run
```

The API starts at `http://localhost:8080`.

Alternatively, build and run the executable JAR:

```bash
mvn clean package
java -jar target/users-service-devsecops-0.0.1-SNAPSHOT.jar
```

## H2 console

Open `http://localhost:8080/h2-console`.

Use:
- JDBC URL: `jdbc:h2:mem:usersdb`
- User Name: `sa`
- Password: leave blank

The database is in-memory and resets when the application stops.

## REST API

Base URL: `http://localhost:8080/api/users`

| Method | Endpoint | Purpose | Success |
|---|---|---|---|
| POST | `/api/users` | Create a user | 201 Created |
| GET | `/api/users` | List all users | 200 OK |
| GET | `/api/users/{id}` | Get one user | 200 OK |
| PUT | `/api/users/{id}` | Replace user details | 200 OK |
| PATCH | `/api/users/{id}/email` | Change only email | 200 OK |
| DELETE | `/api/users/{id}` | Delete a user | 204 No Content |

### Example requests

Create:
```http
POST /api/users
Content-Type: application/json

{
  "firstName": "Asha",
  "lastName": "Sharma",
  "email": "asha.sharma@example.com"
}
```

Update:
```http
PUT /api/users/1
Content-Type: application/json

{
  "firstName": "Asha",
  "lastName": "Patel",
  "email": "asha.patel@example.com"
}
```

Change email:
```http
PATCH /api/users/1/email
Content-Type: application/json

{
  "email": "asha.new@example.com"
}
```

## Validation and error behavior
- Names are required and limited to 60 characters.
- Email is required, must have a valid email format, and is limited to 254 characters.
- Email addresses are unique (case-insensitive application check).
- Missing user returns 404.
- Duplicate email returns 409.
- Invalid request data returns 400 with field-level validation details.

## Current project boundary

This initial application intentionally contains no test classes and no JaCoCo configuration. After we run the application and verify all endpoints manually, we will add JUnit 5/Mockito tests and JaCoCo as the next step.
