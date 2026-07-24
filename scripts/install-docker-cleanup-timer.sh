#!/usr/bin/env sh
set -eu

RETENTION_HOURS="${DOCKER_CLEANUP_RETENTION_HOURS:-24}"
RUN_NOW="${DOCKER_CLEANUP_RUN_NOW:-1}"

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

case "$RUN_NOW" in
    1|true|TRUE|yes|YES)
        RUN_NOW=1
        ;;
    0|false|FALSE|no|NO)
        RUN_NOW=0
        ;;
    *)
        echo "DOCKER_CLEANUP_RUN_NOW deve ser 1/0, true/false ou yes/no." >&2
        exit 2
        ;;
esac

if [ "$(id -u)" -ne 0 ]; then
    echo "Execute este instalador como root: sudo sh scripts/install-docker-cleanup-timer.sh" >&2
    exit 1
fi

if ! command -v systemctl >/dev/null 2>&1; then
    echo "systemd não está disponível neste servidor." >&2
    exit 1
fi

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
SOURCE_SCRIPT="$SCRIPT_DIR/docker-disk-cleanup.sh"
TARGET_SCRIPT="/usr/local/sbin/praxis-docker-disk-cleanup"
SERVICE_FILE="/etc/systemd/system/praxis-docker-disk-cleanup.service"
TIMER_FILE="/etc/systemd/system/praxis-docker-disk-cleanup.timer"

if [ ! -f "$SOURCE_SCRIPT" ]; then
    echo "Arquivo não encontrado: $SOURCE_SCRIPT" >&2
    exit 1
fi

install -m 0755 "$SOURCE_SCRIPT" "$TARGET_SCRIPT"

cat > "$SERVICE_FILE" <<EOF
[Unit]
Description=Limpeza segura de imagens e cache Docker do Práxis
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
Environment=DOCKER_CLEANUP_RETENTION_HOURS=$RETENTION_HOURS
ExecStart=/usr/local/sbin/praxis-docker-disk-cleanup
Nice=10
IOSchedulingClass=best-effort
IOSchedulingPriority=7
NoNewPrivileges=true
EOF

cat > "$TIMER_FILE" <<'EOF'
[Unit]
Description=Executa diariamente a limpeza de disco Docker do Práxis

[Timer]
OnCalendar=*-*-* 03:15:00
RandomizedDelaySec=15m
Persistent=true
Unit=praxis-docker-disk-cleanup.service

[Install]
WantedBy=timers.target
EOF

systemctl daemon-reload
systemctl enable --now praxis-docker-disk-cleanup.timer

if [ "$RUN_NOW" -eq 1 ]; then
    systemctl start praxis-docker-disk-cleanup.service
    echo "Rotina instalada e primeira limpeza executada."
else
    echo "Rotina instalada sem executar uma segunda limpeza imediata."
fi

echo "Retenção configurada: ${RETENTION_HOURS} hora(s)."
echo "Consulte: systemctl status praxis-docker-disk-cleanup.timer"
echo "Logs: journalctl -u praxis-docker-disk-cleanup.service"
