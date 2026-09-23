# syntax=docker/dockerfile:1.7

########################################
# Build stage — compiles, lints, tests, and packages the application.
# Nothing from this stage ships in the final image.
########################################
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace

# The Maven Wrapper (.mvn/wrapper/maven-wrapper.properties) downloads a pinned Maven 3.9.9
# distribution zip on first use — the base JDK image has neither wget nor unzip installed.
RUN apk add --no-cache wget unzip

# Resolve dependencies in their own layer so `docker build` only re-downloads them when
# pom.xml or the checkstyle config changes, not on every source edit.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY config/ config/
RUN ./mvnw -q -B dependency:go-offline

COPY src/ src/

# Runs the same gates the harness Evaluator runs (checkstyle, compile -Werror, unit +
# ArchUnit module-boundary tests, JaCoCo coverage check) before producing the jar — a build
# that fails any of these does not produce an image.
RUN ./mvnw -q -B verify

########################################
# Runtime stage — JRE + the built jar only. No Maven, no source, no build cache.
########################################
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Alpine's busybox ships a wget applet used by the HEALTHCHECK below; no extra package needed.
RUN addgroup -S storeops && adduser -S storeops -G storeops

# Glob matches only the repackaged fat jar (com.storeops:storeops-api) — Spring Boot's
# repackage goal renames the original thin jar to *.jar.original, which this pattern excludes.
COPY --from=build /workspace/target/storeops-api-*.jar app.jar
RUN chown storeops:storeops app.jar
USER storeops

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=5 \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
