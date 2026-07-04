# Auditoria agregada de equidade

> Status: especificacao de produto e governanca. Nao habilitar coleta adicional por padrao.

## Objetivo

Permitir que uma empresa avalie, sempre de forma agregada, se uma avaliacao situacional apresenta diferencas relevantes de resultado, conclusao, abandono ou decisao humana entre grupos de analise previamente autorizados.

Essa auditoria deve apoiar governanca, revisao de criterios e melhoria da avaliacao. Ela nao deve ser usada para tomar decisao individual sobre candidato.

## Principios obrigatorios

1. Coleta desativada por padrao.
2. Base legal, finalidade, retencao e responsavel interno documentados antes de qualquer estudo.
3. Relatorios apenas com amostra minima por grupo, por exemplo `n >= 20`.
4. Atributos de grupo nunca aparecem no resultado individual.
5. Atributos de grupo nunca influenciam score.
6. Acesso restrito a perfis autorizados de governanca/compliance.
7. Toda consulta, exportacao ou alteracao de configuracao registra auditoria.

## Metricas recomendadas

- Taxa de conclusao por grupo.
- Taxa de abandono por grupo.
- Media e dispersao do score geral por grupo.
- Media por competencia por grupo.
- Distribuicao de decisoes humanas por grupo.
- Diferenca absoluta entre grupos e alerta quando passar do limite configurado.

## Fluxo recomendado

```text
Configurar estudo -> confirmar base legal -> importar grupos separados do resultado individual -> gerar agregados -> revisar alertas -> documentar acao corretiva
```

## Interface sugerida

Rota futura: `/compliance/auditoria-equidade`

A tela deve mostrar avaliacao, versao, periodo, grupos incluidos, tamanho de amostra, metricas agregadas, alertas, recomendacoes de revisao e historico de acoes corretivas.

## Fora de escopo por seguranca

- Mostrar grupo no detalhe do candidato.
- Usar grupo para alterar score.
- Automatizar aprovacao ou reprovacao com base em grupo.
- Exibir metricas de grupos com amostra pequena.
- Prometer ausencia absoluta de vies. A promessa correta e monitoramento, revisao e documentacao.

## Roadmap tecnico

1. Tabela de configuracao de estudo por empresa, avaliacao e versao.
2. Entrada segregada de atributos de grupo com base legal registrada.
3. Job de agregacao com limiar minimo de amostra.
4. Endpoint agregado para compliance.
5. Tela em `/compliance/auditoria-equidade`.
6. Eventos de auditoria para consulta, exportacao e alteracao de configuracao.
