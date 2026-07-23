# Fonte canônica da integração Gupy

> **Finalidade:** impedir que o Práxis replique páginas, aliases e contratos inconsistentes do portal da Gupy. Este documento é o único ponto do repositório autorizado a referenciar diretamente `developers.gupy.io`.

## Fonte externa oficial

A integração do Práxis usa exclusivamente o contrato de **testes de provedores externos** publicado pela Gupy:

- [Integração com testes de provedores externos](https://developers.gupy.io/docs/integra%C3%A7%C3%A3o-com-testes-de-provedores-externos)

O portal externo é uma dependência documental, não uma parte deste repositório. Slugs, páginas, menus e conteúdo do portal só podem ser corrigidos pela Gupy.

## Fontes internas de verdade

| Assunto | Documento | Responsabilidade |
| --- | --- | --- |
| Contrato implementado | [Integração Praxis como provedor](INTEGRACAO-GUPY-PROVEDOR.md) | Autenticação, endpoints, payloads, validações, estados e segurança. |
| Fluxos adjacentes | [Matriz de aderência dos fluxos adjacentes](GUPY-FLUXOS-ADJACENTES.md) | Webhooks gerais, atualização de vagas, pré-admissão e folha, sem ampliar o contrato de provedor externo. |
| Homologação | [Centro de homologação técnica](HOMOLOGACAO-GUPY.md) | Evidências, bloqueios, prontidão e validação em vaga real. |
| Entrega assíncrona | [Arquitetura de Outbox](ARQUITETURA_OUTBOX_PATTERN.md) | Processamento, retry, DLQ e operação das entregas. |
| Operação geral | [Operação em produção](OPERACAO.md) | Monitoramento, incidentes e procedimentos operacionais. |
| Implementação | `backend/src/main/java/br/com/iforce/praxis/gupy` | Comportamento executável do contrato Gupy. |
| Testes de contrato | `backend/src/test/java/br/com/iforce/praxis/gupy` | Evidências automatizadas de compatibilidade. |

Em caso de divergência, código e testes prevalecem sobre documentos internos. A documentação deve ser corrigida no mesmo pull request que alterar o contrato público.

## Mapeamento de URLs revisado em 23/07/2026

A coleta integral foi comparada por título, conteúdo e finalidade. As cinco URLs registradas como falha correspondem a quatro páginas canônicas:

| URL registrada na coleta | Página canônica | Situação |
| --- | --- | --- |
| `configurando-webhooks-e-obtendo-payloads-reais` | [Configurando webhooks e obtendo payloads reais](https://developers.gupy.io/docs/configurando-webhooks-e-obtendo-payloads-reais-) | Alias sem hífen final. O conteúdo útil está na rota com hífen final. |
| `configurando-webhooks-e-obtendo-payloads-reais-copy` | [Configurando webhooks e obtendo payloads reais](https://developers.gupy.io/docs/configurando-webhooks-e-obtendo-payloads-reais-) | Duplicata com sufixo `copy`; não deve ser usada como fonte. |
| `fluxo-de-atualizacao-de-vagas` | [Fluxo de Atualização de vagas](https://developers.gupy.io/docs/atualiza%C3%A7%C3%A3o-de-vagas) | O slug canônico usa `atualização-de-vagas`, sem o prefixo `fluxo-de`. |
| `fluxo-de-pré-admissão-da-pessoa-contratada-copy` | [Fluxo de Admissão da Pessoa Contratada](https://developers.gupy.io/docs/fluxo-de-admiss%C3%A3o-da-pessoa-contratada) | O conteúdo canônico usa “admissão”, sem `pré-` e sem `copy`. |
| `fluxo-de-admissão-para-sistema-de-folha-copy` | [Fluxo de Admissão para Sistema de Folha](https://developers.gupy.io/docs/fluxo-de-admiss%C3%A3o-para-sistema-de-folha-) | Duplicata com `copy`; o conteúdo útil está na rota com hífen final. |

As rotas canônicas acima pertencem a fluxos gerais de R&S ou ao produto Gupy Admissão. Elas não substituem o contrato de testes de provedores externos.

## Problemas externos observados

A auditoria realizada em 18/07/2026 e revisada em 23/07/2026 encontrou no portal da Gupy:

- páginas com sufixos `copy`, `copy-of` e `-1`;
- rotas com hífen final;
- slugs com erro de digitação;
- títulos genéricos repetidos;
- conteúdo de navegação replicado em todas as páginas;
- referências sem corpo útil;
- páginas diferentes cobrindo o mesmo fluxo.

Esses problemas não devem ser reproduzidos no Práxis. Nenhum alias externo provisório é tratado como fonte normativa.

## Regras de manutenção

1. Não copiar a documentação completa da Gupy para o repositório.
2. Não criar links diretos para `developers.gupy.io` fora deste arquivo.
3. Não referenciar URLs com `copy`, `copy-of`, sufixo numérico ou hífen final, salvo nesta tabela de saneamento documental.
4. Manter exemplos de payload somente no documento do contrato implementado.
5. Manter retry, backoff e DLQ somente no documento de Outbox.
6. Manter checklist e evidências somente no documento de homologação.
7. Diferenciar sempre:
   - compatibilidade técnica;
   - prontidão para validação externa;
   - homologação formal aprovada.
8. Executar `python3 scripts/validate_docs.py` antes de publicar alterações.

## Processo de atualização

1. Revisar a alteração publicada pela Gupy.
2. Confirmar o comportamento no código e nos testes do Práxis.
3. Atualizar primeiro o documento responsável pelo assunto.
4. Atualizar referências secundárias sem repetir o contrato.
5. Executar a validação documental.
6. Registrar no pull request se a mudança foi apenas documental ou se alterou o contrato público.

## Limite de responsabilidade

O Práxis controla sua implementação, documentação, testes e evidências. A correção de links, aliases, títulos ou conteúdo hospedado no portal da Gupy depende do mantenedor externo.

Última revisão: 23/07/2026.
