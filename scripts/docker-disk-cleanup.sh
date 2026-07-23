#!/usr/bin/env sh
set -eu

RETENTION_HOURS="${DOCKER_CLEANUP_RETENTION_HOURS:-24}"
LOCK_DIR="${TMPDIR:-/tmp}/praxis-docker-disk-cleanup.lock"

case "$RETENTION_HOURS" in
    ''|*[!0-9]*)
        echo "DOCKER_CLEANUP_RETENTION_HOURS deve ser um número inteiro de horas." >&2
        exit 2
        ;;
esac

if [ "$RETENTION_HOURS" -lt 1 ]; then
    echo "DOCKER_CLEANUP_RETENTION_HOURS deve ser maior ou igual a 1." >&2
    exit 2
fi

if ! command -v docker >/dev/null 2>&1; then
    echo "Docker não está instalado ou não está disponível no PATH." >&2
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    echo "Não foi possível acessar o daemon Docker. Execute como root ou com permissão no socket Docker." >&2
    exit 1
fi

if ! mkdir "$LOCK_DIR" 2>/dev/null; then
    echo "Já existe uma limpeza Docker em execução; encerrando sem erro."
    exit 0
fi

cleanup_lock() {
    rmdir "$LOCK_DIR" 2>/dev/null || true
}
trap cleanup_lock EXIT HUP INT TERM

show_usage() {
    echo
    echo "Uso do filesystem:"
    if [ -d /var/lib/docker ]; then
        df -h /var/lib/docker || true
    else
        df -h / || true
    fi

    echo
    echo "Uso interno do Docker:"
    docker system df || true
}

run_prune() {
    description="$1"
    shift
    echo
    echo "==> $description"
    "$@"
}

echo "Limpeza Docker do Práxis"
echo "Retenção mínima: ${RETENTION_HOURS} hora(s)"
echo "Volumes não serão removidos."

show_usage

run_prune \
    "Removendo containers parados antigos" \
    docker container prune --force --filter "until=${RETENTION_HOURS}h"

run_prune \
    "Removendo imagens sem uso antigas" \
    docker image prune --all --force --filter "until=${RETENTION_HOURS}h"

run_prune \
    "Removendo cache BuildKit sem uso antigo" \
    docker builder prune --all --force --filter "until=${RETENTION_HOURS}h"

run_prune \
    "Removendo redes sem uso antigas" \
    docker network prune --force --filter "until=${RETENTION_HOURS}h"

show_usage

echo
echo "Limpeza concluída sem remover volumes persistentes."
