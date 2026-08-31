# syntax=docker/dockerfile:1

FROM maven:3.9.15-eclipse-temurin-26 AS build
WORKDIR /workspace/backend

ARG MAVEN_RETRY_ATTEMPTS=3

COPY backend/pom.xml ./
RUN set -eu; \
    attempt=1; \
    while true; do \
      if mvn -B -ntp dependency:go-offline; then \
        break; \
      fi; \
      if [ "$attempt" -ge "$MAVEN_RETRY_ATTEMPTS" ]; then \
        echo "Maven dependency resolution failed after ${MAVEN_RETRY_ATTEMPTS} attempts." >&2; \
        exit 1; \
      fi; \
      delay=$((attempt * 10)); \
      echo "Maven dependency resolution failed (attempt ${attempt}/${MAVEN_RETRY_ATTEMPTS}); retrying in ${delay}s..." >&2; \
      sleep "$delay"; \
      attempt=$((attempt + 1)); \
    done

COPY backend/src ./src
RUN set -eu; \
    attempt=1; \
    while true; do \
      if mvn -B -ntp -DskipTests package; then \
        break; \
      fi; \
      if [ "$attempt" -ge "$MAVEN_RETRY_ATTEMPTS" ]; then \
        echo "Maven package build failed after ${MAVEN_RETRY_ATTEMPTS} attempts." >&2; \
        exit 1; \
      fi; \
      delay=$((attempt * 10)); \
      echo "Maven package build failed (attempt ${attempt}/${MAVEN_RETRY_ATTEMPTS}); retrying in ${delay}s..." >&2; \
      sleep "$delay"; \
      attempt=$((attempt + 1)); \
    done

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

RUN groupadd --system app && useradd --system --gid app --home-dir /app app

COPY --from=build --chown=app:app /workspace/backend/target/quarkus-app/lib/ ./lib/
COPY --from=build --chown=app:app /workspace/backend/target/quarkus-app/*.jar ./
COPY --from=build --chown=app:app /workspace/backend/target/quarkus-app/app/ ./app/
COPY --from=build --chown=app:app /workspace/backend/target/quarkus-app/quarkus/ ./quarkus/

USER app
EXPOSE 8080

ENV JAVA_OPTS_APPEND="-Djava.util.logging.manager=org.jboss.logmanager.LogManager"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS $JAVA_OPTS_APPEND -jar /app/quarkus-run.jar"]
