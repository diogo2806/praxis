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
- mantém retenção de 24 horas por padrão;
- registra a saída no journal do systemd.

A retenção do serviço pode ser definida durante a instalação:

```bash
sudo DOCKER_CLEANUP_RETENTION_HOURS=48 \
  sh scripts/install-docker-cleanup-timer.sh
```

Quando a limpeza já tiver sido executada antes da instalação, desative apenas a segunda execução imediata:

```bash
sudo DOCKER_CLEANUP_RETENTION_HOURS=24 \
  DOCKER_CLEANUP_RUN_NOW=0 \
  sh scripts/install-docker-cleanup-timer.sh
```

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

## Execução remota pelo GitHub Actions

O workflow `.github/workflows/docker-maintenance.yml` possui uma execução manual protegida para realizar a manutenção diretamente na VPS.

A execução remota:

1. somente aceita a branch `main`;
2. exige que o operador digite `EXECUTAR` no campo de confirmação;
3. utiliza o ambiente GitHub `production`;
4. valida a sintaxe e as salvaguardas antes de acessar a VPS;
5. usa SSH com verificação obrigatória da chave do host;
6. envia os scripts para um diretório temporário com permissão restrita;
7. executa a limpeza sem remover volumes;
8. instala ou atualiza o timer do systemd;
9. coleta evidências antes e depois da limpeza;
10. valida o timer, o backend e o frontend;
11. publica as evidências no resumo do job;
12. remove a chave SSH e os arquivos temporários ao final.

### Ambiente protegido

Crie o ambiente `production` em `Settings > Environments > New environment`.

Recomenda-se configurar:

- revisores obrigatórios;
- restrição de implantação para a branch `main`;
- secrets definidos no próprio ambiente;
- nenhuma aprovação automática para usuários sem responsabilidade operacional.

Mesmo sem uma regra de branch configurada no ambiente, o workflow interrompe a execução quando `github.ref` não é `refs/heads/main`.

### Secrets e variables

Configure os seguintes valores no ambiente `production`:

| Nome | Tipo recomendado | Obrigatório | Finalidade |
|---|---|---:|---|
| `VPS_SSH_HOST` | Secret ou variable | Sim | Hostname ou IP da VPS. |
| `VPS_SSH_PORT` | Secret ou variable | Não | Porta SSH. O padrão é `22`. |
| `VPS_SSH_USER` | Secret ou variable | Sim | Usuário operacional dedicado. |
| `VPS_SSH_PRIVATE_KEY` | Secret | Sim | Chave privada SSH sem interação por senha. |
| `VPS_SSH_KNOWN_HOSTS` | Secret | Sim | Linha confiável do `known_hosts` da VPS. |
| `BACKEND_HEALTH_URL` | Secret ou variable | Sim | Endpoint HTTP/HTTPS de saúde do backend. |
| `FRONTEND_HEALTH_URL` | Secret ou variable | Sim | Endpoint HTTP/HTTPS de saúde do frontend. |

Exemplos de endpoints:

```text
BACKEND_HEALTH_URL=https://api.exemplo.com/actuator/health
FRONTEND_HEALTH_URL=https://app.exemplo.com/
```

O workflow não imprime os endpoints, a chave privada nem o conteúdo do `known_hosts`. Host, usuário e URLs também são mascarados explicitamente durante a execução.

### Geração segura do known_hosts

Cole em `VPS_SSH_KNOWN_HOSTS` a chave do host previamente validada por um canal confiável.

Para porta padrão:

```bash
ssh-keyscan -H servidor.exemplo.com
```

Para porta diferente de 22:

```bash
ssh-keyscan -H -p 2222 servidor.exemplo.com
```

Não gere nem aceite a chave automaticamente dentro do workflow. Compare a impressão digital recebida com a chave apresentada pelo provedor ou pelo administrador da VPS antes de salvar o secret.

### Permissões do usuário SSH

Use um usuário dedicado para manutenção, sem login por senha, autorizado pela chave configurada no GitHub.

O workflow exige:

- acesso SSH não interativo;
- `sudo -n`, sem solicitação de senha;
- acesso ao daemon Docker;
- permissão para instalar arquivos em `/usr/local/sbin` e `/etc/systemd/system`;
- permissão para executar `systemctl` e consultar `journalctl`.

A conta não deve ser compartilhada com usuários pessoais. A chave deve ser exclusiva desta automação e deve ser substituída imediatamente em caso de exposição.

### Como executar

1. Acesse `Actions` no repositório.
2. Abra `Docker maintenance validation`.
3. Clique em `Run workflow`.
4. Selecione a branch `main`.
5. Informe a retenção em horas, normalmente `24`.
6. Digite exatamente `EXECUTAR`.
7. Confirme a execução.
8. Aprove o ambiente `production`, quando houver revisão obrigatória.

A retenção deve ser um número inteiro maior ou igual a 1.

### Evidências registradas

O resumo do job registra, sem exibir credenciais:

- `df -h` antes e depois;
- `docker system df` antes e depois;
- `docker ps --no-trunc` antes e depois;
- saída da rotina de limpeza;
- instalação ou atualização do timer;
- `systemctl is-enabled` do timer;
- `systemctl is-active` do timer;
- `systemctl status` e `systemctl list-timers`;
- até 200 linhas recentes do journal da unidade;
- status HTTP do backend e do frontend.

O job falha quando:

- o workflow não é executado a partir da `main`;
- a confirmação não é `EXECUTAR`;
- um secret ou endpoint obrigatório não está configurado;
- a conexão SSH ou a verificação da chave do host falha;
- `sudo` não pode ser usado de forma não interativa;
- o Docker está inacessível;
- a limpeza ou a instalação do timer falha;
- o timer não está habilitado ou ativo;
- backend ou frontend não respondem com HTTP `2xx` ou `3xx`.

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

Para diagnosticar uma execução remota com falha:

1. abra o job `Execute maintenance on production VPS`;
2. consulte a etapa que falhou;
3. abra o `Job summary` para verificar as evidências parciais;
4. confirme se os secrets pertencem ao ambiente `production`;
5. valide a chave do host e a porta SSH;
6. teste `sudo -n true` usando a mesma conta;
7. teste os dois endpoints a partir da própria VPS;
8. consulte o journal da unidade.

Comandos adicionais no host:

```bash
sudo systemctl is-enabled praxis-docker-disk-cleanup.timer
sudo systemctl is-active praxis-docker-disk-cleanup.timer
sudo systemctl status praxis-docker-disk-cleanup.timer
sudo journalctl -u praxis-docker-disk-cleanup.service --since '24 hours ago'
```

## Estados possíveis

- `Uso normal`: há espaço livre suficiente e a limpeza diária conclui sem erro.
- `Pressão de disco`: filesystem acima de 80%; execute a rotina e revise frequência de deploy.
- `Crítico`: filesystem acima de 90%; execute com retenção de uma hora e confirme os serviços após a limpeza.
- `Docker inacessível`: o script encerra sem alterações quando não consegue acessar o daemon.
- `Limpeza em andamento`: uma segunda execução encerra sem erro por causa da trava em `/tmp`.
- `Timer inativo`: o workflow falha até que a unidade esteja habilitada e ativa.
- `Serviço indisponível`: o workflow falha quando backend ou frontend não passam na verificação HTTP.
- `Aguardando aprovação`: o job permanece bloqueado pela proteção do ambiente `production`.

## Motivos de bloqueio

A rotina não executa quando:

- Docker não está instalado;
- o usuário não possui acesso ao socket do Docker;
- `DOCKER_CLEANUP_RETENTION_HOURS` não é um inteiro maior ou igual a 1;
- `DOCKER_CLEANUP_RUN_NOW` possui valor inválido;
- o daemon Docker está indisponível;
- a branch selecionada não é `main`;
- a confirmação manual está incorreta;
- o ambiente protegido não foi aprovado;
- a chave SSH, o `known_hosts` ou os endpoints não foram configurados;
- a conta remota não possui `sudo` não interativo.

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

O workflow também pesquisa estaticamente os scripts e falha caso encontre `docker volume prune` ou `docker system prune --volumes`.

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

Para remover somente a automação do systemd:

```bash
sudo systemctl disable --now praxis-docker-disk-cleanup.timer
sudo rm -f /etc/systemd/system/praxis-docker-disk-cleanup.timer
sudo rm -f /etc/systemd/system/praxis-docker-disk-cleanup.service
sudo rm -f /usr/local/sbin/praxis-docker-disk-cleanup
sudo systemctl daemon-reload
sudo systemctl reset-failed
```

Para revogar a execução remota:

1. remova ou desative a chave pública no `authorized_keys` da VPS;
2. apague `VPS_SSH_PRIVATE_KEY` do ambiente `production`;
3. remova os demais secrets operacionais;
4. revise o histórico de aprovações e execuções do ambiente;
5. gere uma nova chave antes de reativar a automação.
