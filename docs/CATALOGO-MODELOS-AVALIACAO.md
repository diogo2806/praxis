# Biblioteca governada de modelos de avaliação

## Objetivo

A biblioteca transforma avaliações versionadas e revisadas em modelos pesquisáveis sem criar dependência mutável entre o catálogo e as avaliações dos clientes. Quando um usuário utiliza um modelo, o Práxis cria uma nova avaliação independente, versão 1 em rascunho, e registra na auditoria a origem exata do conteúdo.

O catálogo não agrega resultados de clientes e não produz benchmark entre empresas. A estrutura armazenada referencia somente a avaliação e a versão de origem, além dos metadados de classificação e governança do modelo.

## Escopos

- `INTERNAL`: visível somente para a empresa proprietária.
- `SHARED`: visível para outras empresas depois de revisão e aprovação administrativa.
- `OFFICIAL`: modelo mantido como referência oficial do Práxis, também sujeito a revisão e aprovação administrativa.

A criação de modelos `SHARED` e `OFFICIAL` exige perfil `ADMIN`. Modelos internos podem ser cadastrados pela empresa proprietária.

## Estados

- `DRAFT`: cadastro inicial, ainda não disponível para gerar avaliações.
- `IN_REVIEW`: enviado para parecer independente.
- `APPROVED`: disponível conforme o escopo.
- `REJECTED`: devolvido para correção e novo envio.
- `ARCHIVED`: retirado do uso futuro sem alterar avaliações já criadas.

O autor não pode aprovar o próprio modelo. Para modelos compartilhados e oficiais, a aprovação exige perfil `ADMIN`.

## Fluxo operacional

1. Abra uma avaliação e uma versão na Central de Avaliações.
2. Acesse `/avaliacoes/modelos?simulationId={id}&versionNumber={versao}`.
3. Cadastre título, resumo, cargo, área, senioridade, setor, duração, idioma, complexidade, evidências metodológicas e limitações.
4. Selecione o escopo. O cadastro nasce em `DRAFT`.
5. Envie o modelo para revisão. A versão de origem precisa estar publicada.
6. Um revisor diferente do autor registra o parecer e aprova ou rejeita.
7. Depois da aprovação, usuários autorizados pesquisam, filtram, favoritam e comparam modelos.
8. Ao escolher **Usar modelo**, informe o nome da nova avaliação.
9. O sistema copia grafo, alternativas, destinos, pontuação, competências, acessibilidade e mídia para uma nova avaliação independente em `DRAFT`.
10. A cópia passa pelos validadores, gabarito por especialistas e governança normais antes da publicação.

## Classificação e pesquisa

Os modelos podem ser localizados por:

- título e resumo;
- cargo;
- área de negócio;
- senioridade;
- setor;
- competência;
- idioma;
- complexidade;
- favoritos do usuário.

A comparação lado a lado apresenta cargo, área, senioridade, setor, duração, quantidade de cenários e alternativas, competências, requisitos de acessibilidade e limitações de uso.

## Prévia do modelo

A prévia é calculada diretamente da versão de origem e apresenta:

- nó inicial;
- quantidade de cenários não finais;
- quantidade de finais;
- quantidade de alternativas;
- duração estimada cadastrada;
- cobertura de competências;
- recursos encontrados de texto simples, audiodescrição, transcrição e legenda.

## Independência das cópias

`SimulationDuplicateService.duplicateFromCatalog` carrega a versão de origem autorizada pelo catálogo e grava uma nova avaliação na empresa atual. A cópia sempre possui:

- novo identificador;
- versão número 1;
- estado `DRAFT`;
- entidades próprias de competências, nós, alternativas e escores;
- evento `SIMULATION_VERSION_CLONED` com modelo, versão do modelo, avaliação de origem e versão de origem.

Alterações ou arquivamento posteriores do modelo não modificam cópias existentes. Como a disponibilização exige versão de origem publicada, o conteúdo usado pelo modelo permanece imutável.

## Isolamento e permissões

A consulta inclui:

- modelos pertencentes à empresa atual, em qualquer estado permitido ao proprietário;
- modelos `SHARED` ou `OFFICIAL` com estado `APPROVED`.

Modelos `INTERNAL` de outra empresa nunca são retornados. Apenas o proprietário envia para revisão ou altera a governança. A duplicação entre empresas somente ocorre depois que o catálogo validou visibilidade e aprovação da referência.

## Motivos de bloqueio

- avaliação ou versão de origem inexistente;
- competência de classificação ausente na versão de origem;
- tentativa de criar modelo compartilhado ou oficial sem perfil `ADMIN`;
- tentativa de enviar para revisão uma origem ainda não publicada;
- estado incompatível com envio ou revisão;
- autor tentando aprovar o próprio modelo;
- revisor sem perfil `ADMIN` para escopo compartilhado ou oficial;
- tentativa de instanciar modelo não aprovado;
- modelo invisível para a empresa atual.

## API

Base: `/api/v1/assessment-templates`

- `GET /` pesquisa modelos visíveis;
- `GET /{templateId}` consulta prévia e governança;
- `POST /` cadastra um modelo;
- `POST /{templateId}/submit` envia para revisão;
- `POST /{templateId}/review` aprova ou rejeita;
- `POST /{templateId}/favorite` alterna favorito;
- `POST /{templateId}/instantiate` cria avaliação independente.

## Auditoria

As tabelas do catálogo recebem as colunas e os gatilhos da auditoria universal por meio de `V1103__refresh_universal_table_auditing`. A criação da avaliação a partir do modelo também registra um evento de versão com a referência da origem.

## Limitações atuais

- o catálogo não compara resultados de candidatos ou clientes;
- o catálogo não atualiza cópias já criadas;
- a versão inicial não possui avaliação pública por estrelas ou comentários;
- o arquivamento permanece previsto no estado do domínio, mas ainda não possui ação na tela;
- modelos multilíngues completos dependerão do conteúdo versionado por locale.
