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

Native builds need GraalVM for JDK 25 as `JAVA_HOME` and take 6–11 min of image compilation after the tests. Run
them in the background rather than blocking on them.

## Verifying changes

- JVM changes: `./mvnw clean verify` must pass with all 4 tests *run* — check surefire doesn't report them as skipped.
- Anything touching native config, dependencies, or new library features: do a single clean native build
  (`./mvnw clean package -Dpackaging=native-image`, exactly what CI does — a second incremental build can hide
  problems) and smoke-test the binary against a fresh Postgres, mirroring the CI `native` job:

  ```bash
  docker run -d --name pg -p 5432:5432 -e POSTGRES_PASSWORD=postgres postgres:16
  ./target/mn-testcontainers-shenanigans &
  curl -sf localhost:8080/books
  curl -sf -X POST -H 'Content-Type: application/json' -d '{"title":"Native","isbn":"123"}' localhost:8080/books
  curl -sf localhost:8080/books/isbn/123
  docker exec pg psql -U postgres -tc 'select md5sum from databasechangelog'   # must equal the JVM build's checksum
  docker rm -f pg
  ```

  A Liquibase checksum that differs from the JVM build means native reflection metadata is missing.
- CI (`.github/workflows/ci.yml`) runs the JVM job, then the native job; both must be green before merging.

## Claude Code settings

`.claude/settings.json` (checked in) pre-approves the commands above — Maven wrapper, the `pg` Docker container,
`curl` against `localhost:8080`, the native binary, and read-only git — and asks before force-pushes and hard resets.
Put personal overrides in `.claude/settings.local.json` (gitignored).

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
- The agent runs in `direct` mode (writes straight into `agent-output/test`). Don't switch back to the default mode:
  it writes per-session dirs that `native:merge-agent-files` merges only on its first execution per build, which is
  before surefire — so a single clean `package` (as in CI) would compile the image without any test metadata.
- The upstream GraalVM metadata repository doesn't match our versions (e.g. Liquibase 4.33 falls back to 5.0.1
  metadata); relying on it alone produced wrong Liquibase changeset checksums in native.
- `jpa.default.compile-time-hibernate-proxies: true` is required: runtime ByteBuddy proxies don't work in native.
- `-H:+SharedArenaSupport` is required for Netty on JDK 25 (buffer release uses shared `Arena`s).
- For your own classes prefer Micronaut annotations (`@Introspected`, `@Serdeable`, `@ReflectiveAccess`, `@TypeHint`)
  over hand-written JSON metadata.

## Gotchas

- Tests need a running Docker daemon. Testcontainers is pinned to 1.21.4 (`testcontainers.version`) because the
  Micronaut BOM's 1.20.x can't talk to Docker Engine 29+. If tests fail with "Could not find a valid Docker
  environment", check `docker info` before suspecting the code.
- After a native-image build, an incremental JVM build can leave stale test classes and every `@MicronautTest` is
  reported as *skipped* ("Test is not bean"). Run `./mvnw clean verify`.
- Spotless runs `apply` (not `check`) in the `verify` phase, so builds reformat sources in place (Palantir style,
  cleanthat). CI fails if that leaves a diff — commit the formatted result.
- `logback.xml.to.java.enabled=false` in the AOT configs: the project logs through `slf4j-simple-logger`, not Logback,
  and the optimizer fails if it's enabled.
- Lombok is an annotation processor listed before the Micronaut processors in `pom.xml`; keep that order.
