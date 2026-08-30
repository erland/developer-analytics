# syntax=docker/dockerfile:1

FROM maven:3.9.11-eclipse-temurin-21 AS build
WORKDIR /workspace/backend

COPY backend/pom.xml ./
RUN mvn -B -ntp dependency:go-offline

COPY backend/src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:21-jre AS runtime
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
