# syntax=docker/dockerfile:1.7

# Build and test stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

ENV MAVEN_OPTS="-Xmx512m -XX:MaxRAMPercentage=75"

SHELL ["/bin/bash", "-o", "pipefail", "-c"]

# PostgreSQL local para executar a suíte completa durante o build da imagem.
# O projeto usa Testcontainers no CI, mas o daemon Docker não fica disponível
# dentro de um docker build. O banco local mantém o mesmo comportamento de
# integração sem permitir que um artefato com testes quebrados seja publicado.
RUN apt-get update \
    && apt-get install -y --no-install-recommends postgresql \
    && rm -rf /var/lib/apt/lists/*

# Copy project files
COPY pom.xml .

# Copy source code
COPY src ./src

# Build the application only after the complete backend test suite succeeds.
RUN --mount=type=cache,target=/root/.m2 <<'EOF'
set -euo pipefail

POSTGRES_VERSION="$(find /usr/lib/postgresql -mindepth 1 -maxdepth 1 -type d -printf '%f\n' | sort -V | tail -n 1)"
POSTGRES_BIN="/usr/lib/postgresql/${POSTGRES_VERSION}/bin"
TEST_DB_NAME="praxis_test"
TEST_DB_USER="praxis_test"
TEST_DB_PASSWORD="praxis_test"
TEST_DB_PORT="55432"
TEST_PGDATA="/tmp/praxis-postgres-data"
TEST_PGSOCKET="/tmp/praxis-postgres-socket"

# Cria um cluster exclusivo do estágio builder. Isso elimina dependência do
# cluster Debian criado pelo pacote e garante autenticação host/local trust
# somente dentro deste estágio descartável.
install -d -m 0700 -o postgres -g postgres "${TEST_PGDATA}"
install -d -m 0770 -o postgres -g postgres "${TEST_PGSOCKET}"

su postgres -c "${POSTGRES_BIN}/initdb --pgdata=${TEST_PGDATA} --username=postgres --auth-local=trust --auth-host=trust --encoding=UTF8 --no-locale"
su postgres -c "${POSTGRES_BIN}/pg_ctl --pgdata=${TEST_PGDATA} --log=${TEST_PGDATA}/postgres.log --options='-c listen_addresses=127.0.0.1 -p ${TEST_DB_PORT} -k ${TEST_PGSOCKET}' --wait start"

stop_test_postgres() {
    su postgres -c "${POSTGRES_BIN}/pg_ctl --pgdata=${TEST_PGDATA} --mode=fast --wait stop" >/dev/null 2>&1 || true
}
trap stop_test_postgres EXIT

"${POSTGRES_BIN}/psql" \
    --host=127.0.0.1 \
    --port="${TEST_DB_PORT}" \
    --username=postgres \
    --dbname=postgres \
    --set=ON_ERROR_STOP=1 \
    --command="CREATE ROLE ${TEST_DB_USER} LOGIN PASSWORD '${TEST_DB_PASSWORD}';"

"${POSTGRES_BIN}/createdb" \
    --host=127.0.0.1 \
    --port="${TEST_DB_PORT}" \
    --username=postgres \
    --owner="${TEST_DB_USER}" \
    "${TEST_DB_NAME}"

# Confirma que o banco está acessível pelo mesmo endpoint usado pela suíte.
PGPASSWORD="${TEST_DB_PASSWORD}" "${POSTGRES_BIN}/psql" \
    --host=127.0.0.1 \
    --port="${TEST_DB_PORT}" \
    --username="${TEST_DB_USER}" \
    --dbname="${TEST_DB_NAME}" \
    --command='SELECT 1' \
    >/dev/null

# Variáveis de ambiente são herdadas pelo processo forkado do Surefire.
# Isso evita que application.properties ou variáveis do ambiente de deploy
# restaurem as credenciais padrão dos testes baseados em Testcontainers.
export SPRING_DATASOURCE_URL="jdbc:postgresql://127.0.0.1:${TEST_DB_PORT}/${TEST_DB_NAME}"
export SPRING_DATASOURCE_USERNAME="${TEST_DB_USER}"
export SPRING_DATASOURCE_PASSWORD="${TEST_DB_PASSWORD}"
export SPRING_DATASOURCE_DRIVER_CLASS_NAME="org.postgresql.Driver"
export DB_HOST="127.0.0.1"
export DB_PORT="${TEST_DB_PORT}"
export DB_NAME="${TEST_DB_NAME}"
export DB_USER="${TEST_DB_USER}"
export DB_PASS="${TEST_DB_PASSWORD}"
export DB_SCHEMA="public"

echo "[build][backend][maven-verify][start] $(date -u +%Y-%m-%dT%H:%M:%SZ)"
mvn clean verify -B -ntp 2>&1 | tee build.log
echo "[build][backend][maven-verify][end] $(date -u +%Y-%m-%dT%H:%M:%SZ) status=0"
EOF

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
