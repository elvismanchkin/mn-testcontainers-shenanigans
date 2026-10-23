# CLAUDE.md

Micronaut 4.10 demo service (books CRUD over PostgreSQL) showing Testcontainers-based integration tests,
Micronaut AOT and GraalVM native image builds. Java 25, Maven (wrapper), package `dev.tsvinc`.

## Commands

```bash
./mvnw verify                                  # compile, tests (Docker required), AOT, shaded jar, Spotless apply
./mvnw clean verify                            # use after building native-image (see Gotchas)
./mvnw test -Dtest=BookControllerTest          # single test class
./mvnw mn:run                                  # run locally; expects Postgres on localhost:5432 (postgres/postgres)
./mvnw package -Dpackaging=native-image        # native executable at target/mn-testcontainers-shenanigans
```

Native builds need GraalVM for JDK 25 as `JAVA_HOME` and take ~7 min of image compilation after the tests.

## Layout

- `src/main/java/dev/tsvinc` — `Application`, `Book` (JPA entity, `@Serdeable`), `BookRepository`
  (Micronaut Data `CrudRepository`), `BookController` (`/books`)
- `src/main/resources/db/changelog` — Liquibase changelogs (YAML); schema is owned by Liquibase, `hbm2ddl` is `none`
- `src/test/resources/application-test.yml` — `jdbc:tc:postgresql:16` Testcontainers JDBC URL (the `test` environment
  is activated automatically by `@MicronautTest`)
- `aot-jar.properties` / `aot-native-image.properties` — Micronaut AOT optimizer config, selected by `${packaging}`
- `src/main/resources/META-INF/native-image/dev.tsvinc/mn-testcontainers-shenanigans/native-image.properties` —
  extra `native-image` build args
- `.github/workflows/ci.yml` — JVM build/tests job, then native build + smoke test against Postgres

## Native image

- Reachability metadata for third-party libraries is **not** checked in. The `native-agent` Maven profile (active when
  `-Dpackaging=native-image`) runs the JVM tests with the GraalVM tracing agent and passes
  `target/native/agent-output/test` to `native-image`. Never build native with `-DskipTests`, and when adding code paths
  that use new library features, make sure a test exercises them — otherwise the native binary fails at runtime.
- The upstream GraalVM metadata repository doesn't match our versions (e.g. Liquibase 4.33 falls back to 5.0.1
  metadata); relying on it alone produced wrong Liquibase changeset checksums in native.
- `jpa.default.compile-time-hibernate-proxies: true` is required: runtime ByteBuddy proxies don't work in native.
- `-H:+SharedArenaSupport` is required for Netty on JDK 25 (buffer release uses shared `Arena`s).
- For your own classes prefer Micronaut annotations (`@Introspected`, `@Serdeable`, `@ReflectiveAccess`, `@TypeHint`)
  over hand-written JSON metadata.

## Gotchas

- Tests need a running Docker daemon. Testcontainers is pinned to 1.21.4 (`testcontainers.version`) because the
  Micronaut BOM's 1.20.x can't talk to Docker Engine 29+.
- After a native-image build, an incremental JVM build can leave stale test classes and every `@MicronautTest` is
  reported as *skipped* ("Test is not bean"). Run `./mvnw clean verify`.
- Spotless runs `apply` (not `check`) in the `verify` phase, so builds reformat sources in place (Palantir style,
  cleanthat). CI fails if that leaves a diff — commit the formatted result.
- `logback.xml.to.java.enabled=false` in the AOT configs: the project logs through `slf4j-simple-logger`, not Logback,
  and the optimizer fails if it's enabled.
- Lombok is an annotation processor listed before the Micronaut processors in `pom.xml`; keep that order.
