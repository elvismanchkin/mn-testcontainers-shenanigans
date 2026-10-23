# Book Management Service with Micronaut

Simple app to show how to use testcontainers in
## Project Overview

This service provides endpoints to manage books with the following capabilities:

- List all books
- Retrieve a book by ID
- Retrieve a book by ISBN
- Create new books

## Technologies Used

- **Java 25**: Latest LTS Java release
- **Micronaut 4.10**: Fast, lightweight microservices framework
- **PostgreSQL**: Relational database for persistence
- **Hibernate/JPA**: For data persistence and object-relational mapping
- **Liquibase**: For database schema management and versioning
- **TestContainers**: For integration testing with real database instances
- **Lombok**: To reduce boilerplate code
- **Jakarta EE**: For standardized enterprise APIs
- **JUnit 5**: For unit and integration testing
- **DataFaker**: For generating test data
- **Micronaut AOT**: Build-time optimizations for faster startup
- **GraalVM Native Image**: Ahead-of-time compilation to a native executable

## API Endpoints

- `GET /books` - List all books
- `GET /books/{id}` - Get book by ID
- `GET /books/isbn/{isbn}` - Get book by ISBN
- `POST /books` - Create a new book

## Setup and Running

### Prerequisites

- Java 25 (GraalVM for JDK 25 to build native executables)
- Maven
- Docker (for running tests with TestContainers)
- PostgreSQL (for local development)

### Running the Application

1. Clone the repository
2. Configure your PostgreSQL connection in `application.yml` if needed
3. Run the application:

```bash
./mvnw mn:run
```

### Building

```bash
# Runnable (shaded) JAR with Micronaut AOT optimizations, runs tests
./mvnw verify
java -jar target/mn-testcontainers-shenanigans-0.1.jar

# GraalVM native executable (requires GraalVM for JDK 25 and Docker).
# Tests run on the JVM with the GraalVM tracing agent to collect reachability metadata
# for third-party libraries, so don't pass -DskipTests here.
./mvnw package -Dpackaging=native-image
./target/mn-testcontainers-shenanigans
```

AOT optimizations are configured in `aot-jar.properties` (JVM) and `aot-native-image.properties` (native).

### Continuous Integration

GitHub Actions (`.github/workflows/ci.yml`) builds and tests the app on JDK 25 with AOT,
then compiles a GraalVM native executable and smoke-tests it against PostgreSQL.

## Micronaut 4.10 Documentation

- [User Guide](https://docs.micronaut.io/4.10.x/guide/index.html)
- [API Reference](https://docs.micronaut.io/4.10.x/api/index.html)
- [Configuration Reference](https://docs.micronaut.io/4.10.x/guide/configurationreference.html)
- [Micronaut Guides](https://guides.micronaut.io/index.html)

---

- [Micronaut Maven Plugin documentation](https://micronaut-projects.github.io/micronaut-maven-plugin/latest/)

## Feature serialization-jackson documentation

- [Micronaut Serialization Jackson Core documentation](https://micronaut-projects.github.io/micronaut-serialization/latest/guide/)

## Feature jdbc-hikari documentation

- [Micronaut Hikari JDBC Connection Pool documentation](https://micronaut-projects.github.io/micronaut-sql/latest/guide/index.html#jdbc)

## Feature testcontainers documentation

- [https://www.testcontainers.org/](https://www.testcontainers.org/)

## Feature maven-enforcer-plugin documentation

- [https://maven.apache.org/enforcer/maven-enforcer-plugin/](https://maven.apache.org/enforcer/maven-enforcer-plugin/)

## Feature jul-to-slf4j documentation

- [https://www.slf4j.org/legacy.html#jul-to-slf4jBridge](https://www.slf4j.org/legacy.html#jul-to-slf4jBridge)

## Feature slf4j-simple-logger documentation

- [https://github.com/GoodforGod/slf4j-simple-logger](https://github.com/GoodforGod/slf4j-simple-logger)

## Feature lombok documentation

- [Micronaut Project Lombok documentation](https://docs.micronaut.io/latest/guide/index.html#lombok)

- [https://projectlombok.org/features/all](https://projectlombok.org/features/all)

## Feature micronaut-aot documentation

- [Micronaut AOT documentation](https://micronaut-projects.github.io/micronaut-aot/latest/guide/)


