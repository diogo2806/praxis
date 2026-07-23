# Manutenção de disco Docker — Práxis

## Finalidade

Evitar indisponibilidade do servidor por esgotamento de disco causado por imagens antigas, containers parados e cache BuildKit acumulado pelos deploys do backend e do frontend.

A rotina deste repositório nunca executa limpeza de volumes. O volume do PostgreSQL e quaisquer outros volumes persistentes devem ser preservados.

## Causa do incidente

Os builds anteriores usavam caches persistentes do BuildKit:

- backend: `/root/.m2`, usado pelo Maven;
- frontend: `/root/.npm`, usado pelo npm.

Além desses caches, cada novo deploy pode deixar imagens antigas sem containers associados. Sem política periódica de retenção, o armazenamento em `/var/lib/docker` cresce até ocupar todo o filesystem.

## Correção aplicada nos builds

Os arquivos `backend/Dockerfile` e `frontend/Dockerfile` não usam mais `--mount=type=cache`. Os caches temporários do Maven e npm são removidos no mesmo passo do build, impedindo que sejam gravados como cache mutável permanente no host.

Imagens e camadas antigas ainda podem existir após atualizações. Por isso, a limpeza periódica do host continua necessária.

## Recuperação imediata

Na raiz do repositório, execute no servidor:

```bash
sudo sh scripts/docker-disk-cleanup.sh
```

A retenção padrão é de 24 horas. Para um servidor já lotado, é possível reduzir temporariamente para uma hora:

```bash
sudo DOCKER_CLEANUP_RETENTION_HOURS=1 sh scripts/docker-disk-cleanup.sh
```

A rotina executa, nesta ordem:

1. diagnóstico com `df -h` e `docker system df`;
2. remoção de containers parados mais antigos que a retenção;
3. remoção de imagens sem uso mais antigas que a retenção;
4. remoção de cache BuildKit sem uso mais antigo que a retenção;
5. remoção de redes sem uso mais antigas que a retenção;
6. novo diagnóstico de disco.

Imagens usadas por containers em execução não são removidas. Volumes não são removidos.

## Instalação preventiva diária

Para instalar o script em `/usr/local/sbin` e criar um timer do systemd:

```bash
sudo sh scripts/install-docker-cleanup-timer.sh
```

O instalador:

- executa uma primeira limpeza imediatamente;
- agenda a rotina diariamente às 03:15, com atraso aleatório de até 15 minutos;
- mantém retenção de 24 horas;
- registra a saída no journal do systemd.

Verifique o agendamento:

```bash
systemctl status praxis-docker-disk-cleanup.timer
systemctl list-timers praxis-docker-disk-cleanup.timer
```

Consulte as execuções:

```bash
journalctl -u praxis-docker-disk-cleanup.service --since today
```

Execute manualmente a unidade instalada:

```bash
sudo systemctl start praxis-docker-disk-cleanup.service
```

## Diagnóstico

Comandos seguros para identificar consumo:

```bash
df -h /var/lib/docker
docker system df
docker system df -v
docker ps --size
docker image ls
docker builder du
```

Se `/var/lib/docker` não estiver em um filesystem separado, use `df -h /`.

## Estados possíveis

- `Uso normal`: há espaço livre suficiente e a limpeza diária conclui sem erro.
- `Pressão de disco`: filesystem acima de 80%; execute a rotina e revise frequência de deploy.
- `Crítico`: filesystem acima de 90%; execute com retenção de uma hora e confirme os serviços após a limpeza.
- `Docker inacessível`: o script encerra sem alterações quando não consegue acessar o daemon.
- `Limpeza em andamento`: uma segunda execução encerra sem erro por causa da trava em `/tmp`.

## Motivos de bloqueio

A rotina não executa quando:

- Docker não está instalado;
- o usuário não possui acesso ao socket do Docker;
- `DOCKER_CLEANUP_RETENTION_HOURS` não é um inteiro maior ou igual a 1;
- o daemon Docker está indisponível.

## Regras de segurança

Nunca execute no servidor de produção:

```bash
docker volume prune
docker system prune --volumes
```

Esses comandos podem remover volumes sem containers associados e causar perda de dados, inclusive do PostgreSQL.

Antes de qualquer intervenção manual adicional, confirme os volumes existentes:

```bash
docker volume ls
```

## Validação após limpeza

Após liberar espaço:

```bash
docker ps
curl -fsS http://localhost:8080/actuator/health
curl -fsS http://localhost/
```

Quando o acesso ocorrer apenas por proxy ou domínio público, valide as URLs externas do backend e frontend em vez de `localhost`.

## Rollback

A limpeza remove apenas recursos Docker sem uso e não possui rollback. Imagens removidas devem ser reconstruídas ou baixadas novamente em um redeploy. Containers em execução e volumes persistentes permanecem intactos.
