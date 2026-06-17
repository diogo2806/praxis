# syntax=docker/dockerfile:1.7

# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

ENV MAVEN_OPTS="-Xmx512m -XX:MaxRAMPercentage=75"

# Copy pom.xml and download dependencies
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
    sh -c 'echo "[build][backend][maven-offline][start] $(date -u +%Y-%m-%dT%H:%M:%SZ)"; pipe=/tmp/build-log.$$; mkfifo "$pipe"; while IFS= read -r line; do echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] $line"; done < "$pipe" & logger=$!; mvn dependency:go-offline -B > "$pipe" 2>&1; status=$?; wait "$logger"; rm -f "$pipe"; echo "[build][backend][maven-offline][end] $(date -u +%Y-%m-%dT%H:%M:%SZ) status=$status"; exit "$status"'

# Copy source code
COPY src ./src

# Build the application
RUN --mount=type=cache,target=/root/.m2 \
    sh -c 'echo "[build][backend][maven-package][start] $(date -u +%Y-%m-%dT%H:%M:%SZ)"; pipe=/tmp/build-log.$$; mkfifo "$pipe"; while IFS= read -r line; do echo "[$(date -u +%Y-%m-%dT%H:%M:%SZ)] $line"; done < "$pipe" & logger=$!; mvn clean package -DskipTests -B > "$pipe" 2>&1; status=$?; wait "$logger"; rm -f "$pipe"; echo "[build][backend][maven-package][end] $(date -u +%Y-%m-%dT%H:%M:%SZ) status=$status"; exit "$status"'

# Runtime stage
FROM eclipse-temurin:21-jre

WORKDIR /app

ENV JAVA_OPTS="-Xms64m -Xmx256m -XX:MaxMetaspaceSize=160m -XX:ActiveProcessorCount=1 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom"

# Copy the jar from builder
COPY --from=builder /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=120s --retries=3 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
