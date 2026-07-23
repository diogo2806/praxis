# Portabilidade de avaliações versionadas

## Finalidade

O pacote portátil do Práxis serve para backup lógico, migração entre ambientes, criação de modelos e intercâmbio controlado com parceiros. Ele transporta apenas conteúdo autoral e metadados de governança. Candidatos, participações, respostas, resultados, tokens, credenciais, cobranças e dados operacionais não pertencem ao contrato.

## Formato atual

Versão: `praxis-assessment-package/1.0`.

O arquivo JSON possui:

- `formatVersion`: versão do contrato;
- `exportedAt`: momento da exportação;
- `contentHash`: SHA-256 do manifesto serializado de forma canônica;
- `manifest.origin`: sistema, avaliação, versão, autor da exportação e data;
- `manifest.assessment`: nome, descrição, situação crítica e finalidade do resultado;
- `manifest.version`: nó inicial, competências, nós, alternativas, destinos, pontuação e campos de acessibilidade;
- `manifest.mediaAssets`: referências declaradas de mídia, tipo, tamanho declarado, hash da referência, licença, origem e modo de empacotamento.

Na versão 1.0, mídias são transportadas por referência segura. O pacote não incorpora bytes de arquivos. Antes de publicar uma avaliação importada, a organização deve confirmar licença, disponibilidade e integridade operacional das mídias.

## Fluxo operacional

1. Abra uma avaliação e uma versão na Central de Avaliações.
2. Acesse `/nova/portabilidade?simulationId={id}&versionNumber={versao}`.
3. Exporte o pacote JSON.
4. No ambiente de destino, selecione o arquivo.
5. Execute `Validar sem gravar`.
6. Revise erros por caminho, avisos, competências e remapeamento planejado.
7. Confirme as competências e a importação.
8. Informe um nome para a nova avaliação.
9. Importe. A avaliação sempre nasce independente, como versão 1 em `DRAFT`.
10. Revise o rascunho nos validadores, execute a aprovação científica e publique pelos fluxos normais.

## Validação

A validação é executada antes de qualquer persistência e verifica:

- versão do formato;
- integridade do hash;
- presença e limites dos campos;
- soma dos pesos das competências;
- unicidade de competências, nós e alternativas;
- existência do nó inicial e dos destinos;
- quantidade de alternativas por cenário;
- texto de relatório em nós finais;
- referências de pontuação somente para competências declaradas;
- notas entre 0 e 100;
- URLs internas ou HTTPS;
- bloqueio de extensões executáveis;
- limite declarado de 50 MB por mídia;
- declaração das referências de mídia no manifesto;
- correspondência do hash de cada referência.

Os diagnósticos retornam `path`, `code` e `message`, permitindo localizar o campo exato do manifesto.

## Segurança e privacidade

O contrato não possui campos para dados pessoais ou operacionais. A importação exige autenticação e utiliza a empresa do usuário atual. A exportação busca a avaliação por `empresa_id`, impedindo acesso cruzado entre organizações.

Pacotes alterados são rejeitados por divergência de hash. Arquivos executáveis e URLs sem HTTPS são bloqueados. A importação somente ocorre após duas confirmações explícitas: diagnóstico revisado e competências aceitas.

A origem, versão, hash, exportador, importador e remapeamento de identificadores são gravados na auditoria da nova avaliação.

## Colisões e remapeamento

O identificador da nova avaliação é gerado a partir do nome e dos oito primeiros caracteres do hash. Quando já existe, o Práxis acrescenta um sufixo incremental. Nós e alternativas preservam seus identificadores internos porque a nova avaliação possui namespace independente.

O retorno da importação contém o mapa entre a origem e os novos identificadores. Esse mapa também é registrado no evento de auditoria.

## Estados e bloqueios

Estados da tela:

- sem contexto de exportação;
- pacote selecionado;
- validação em andamento;
- pacote rejeitado;
- pacote íntegro;
- confirmação pendente;
- avaliação importada em rascunho.

Bloqueios principais:

- formato incompatível;
- hash divergente;
- grafo inválido;
- pesos incorretos;
- competência desconhecida;
- mídia insegura, executável, não declarada ou acima do limite;
- confirmação ausente;
- avaliação fora da empresa atual.

## Evolução do formato

Mudanças compatíveis adicionam campos opcionais à mesma versão. Mudanças que alterem semântica, obrigatoriedade, representação do grafo ou mecanismo de integridade exigem nova versão, por exemplo `praxis-assessment-package/2.0`.

O importador deve rejeitar versões desconhecidas, nunca interpretar silenciosamente um contrato mais novo. Uma futura migração entre formatos deverá:

1. ler a versão de origem;
2. transformar para uma estrutura intermediária validada;
3. registrar avisos de perda ou conversão;
4. recalcular o hash no formato de destino;
5. exigir nova confirmação antes de persistir.

## API

- `GET /api/v1/simulations/{simulationId}/versions/{versionNumber}/package`
- `POST /api/v1/simulation-packages/validate`
- `POST /api/v1/simulation-packages/import`

## Auditoria universal

A migração Java `V1101__refresh_universal_table_auditing` reaplica de forma idempotente a infraestrutura criada por `V1011__universal_table_auditing`. Assim, tabelas adicionadas depois da migração inicial também recebem `created_at`, `created_by`, `updated_at`, `updated_by`, `trg_praxis_audit_columns` e `trg_praxis_row_history`.
