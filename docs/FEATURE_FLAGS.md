# Feature flags e rollout progressivo

## Finalidade

O mecanismo permite liberar funcionalidades de forma gradual sem criar forks nem espalhar condicionais pela aplicação. As flags são administradas por operadores `ADMIN`, avaliadas no backend e expostas ao frontend somente quando marcadas como públicas.

Feature flags não podem alterar pontuação, gabarito, critérios de aprovação ou tratamento de candidatos dentro de uma mesma versão publicada. Mudanças desse tipo exigem nova versão da avaliação e o fluxo normal de governança.

## Precedência

A avaliação usa a seguinte ordem, da maior para a menor prioridade:

1. `kill switch`: sempre desliga o recurso;
2. usuário explicitamente liberado;
3. empresa explicitamente liberada;
4. papel explicitamente liberado;
5. plano explicitamente liberado;
6. ambiente explicitamente liberado;
7. rollout percentual determinístico;
8. override global;
9. valor padrão seguro.

Flags inativas ou expiradas retornam o valor padrão. A expiração não exclui registros nem auditoria.

## Rollout determinístico

O bucket usa SHA-256 sobre a chave da flag e um identificador estável. O valor fica entre `0` e `9999`; uma flag com 25% é ligada para buckets abaixo de `2500`.

O mesmo identificador recebe sempre a mesma variante enquanto a chave e o percentual permanecerem iguais. Não use dados pessoais diretamente como identificador; prefira IDs internos opacos.

## Escopos

- global: override explícito ON ou OFF;
- ambiente: por exemplo `production` ou `staging`;
- empresa: IDs internos das empresas piloto;
- plano: código comercial do plano;
- usuário: IDs internos de usuários;
- papel: papéis de acesso sem o prefixo `ROLE_`;
- percentual: distribuição estável para quem não foi decidido por escopo explícito.

## Ciclo operacional

1. Criar a flag com descrição, responsável e padrão seguro.
2. Para flags temporárias, informar expiração e plano de remoção.
3. Configurar empresas piloto ou outro escopo explícito.
4. Simular identificadores representativos no painel administrativo.
5. Ativar e acompanhar métricas ON/OFF de erro, latência, abandono e falhas de integração.
6. Aumentar o percentual gradualmente quando os indicadores permanecerem aceitáveis.
7. Acionar o kill switch quando houver risco operacional.
8. Remover a flag expirada conforme o plano, substituindo a condição por comportamento definitivo.

## APIs

### Administração

Base: `/api/admin/feature-flags`

- `GET /`: busca, filtro e resumo de governança;
- `POST /`: criação;
- `PUT /{flagId}`: atualização;
- `POST /{flagId}/active`: ativação ou desativação;
- `POST /{flagId}/kill-switch`: bloqueio ou liberação imediata;
- `POST /{flagId}/evaluate`: simulação de um contexto;
- `POST /{flagId}/metrics`: registro de amostra por variante;
- `GET /{flagId}/metrics`: métricas agregadas;
- `GET /{flagId}/history`: trilha append-only relacionada à flag.

Todos os endpoints administrativos exigem papel `ADMIN` pela política `/api/admin/**`.

### Frontend autenticado

`GET /api/v1/feature-flags` retorna apenas um mapa `chave -> boolean` das flags com `frontendExposed=true`. Descrição, listas de empresas, percentuais, responsáveis e regras internas não são enviados ao navegador.

## Métricas

Cada amostra possui:

- chave da flag;
- variante `ON` ou `OFF`;
- nome da métrica;
- valor numérico.

O backend mantém contagem, soma e média por combinação. Nomes recomendados:

- `error`;
- `latency_ms`;
- `abandonment`;
- `integration_failure`;
- `completion`.

## Auditoria

Criação, atualização, ativação, desativação e alteração do kill switch geram eventos append-only com ator, horário, chave, responsável, percentual, expiração e empresas-alvo. Segredos não devem ser colocados em chaves, descrições ou escopos.

## Expiração

O monitor executa por padrão a cada hora e registra aviso quando existem flags expiradas. O intervalo pode ser alterado por `praxis.feature-flags.expiry-check-ms`.

A verificação não remove automaticamente a flag. A remoção precisa seguir o plano registrado para evitar excluir uma condição ainda presente no código.
