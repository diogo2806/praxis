# Módulo de parceiros e especialistas

## Objetivo

Permitir que plataformas de recrutamento, consultorias e demais parceiros organizem seus especialistas, clientes e catálogos de avaliações dentro do Práxis.

O módulo não calcula preço, comissão, cobrança ou repasse financeiro. A relação comercial continua sendo tratada pelos sistemas e contratos existentes.

## Fluxo operacional

1. O parceiro acessa `/parceiros`.
2. Convida especialistas, que recebem o perfil restrito `PARTNER_SPECIALIST`.
3. Os especialistas usam o editor atual do Práxis para criar e revisar rascunhos de avaliações.
4. O parceiro valida e publica as avaliações concluídas.
5. O parceiro cadastra seus clientes e informa o identificador da empresa no ATS correspondente.
6. O parceiro libera avaliações publicadas no catálogo de cada cliente.
7. O parceiro gera um token exclusivo para o cliente.
8. Gupy ou Recrutei usa esse token para listar somente o catálogo liberado, registrar candidatos e consultar resultados.

## Permissões

- Usuário `EMPRESA`: administra especialistas, clientes, tokens, publicação e catálogo.
- Usuário `PARTNER_SPECIALIST`: cria e edita rascunhos, inclui mídias e executa validações estruturais.
- Especialistas não acessam clientes, integrações, resultados, cobrança ou administração da equipe.
- Especialistas não publicam, arquivam ou clonam versões publicadas.
- O token do cliente não permite acessar testes fora do catálogo liberado.

## APIs internas

Base: `/api/v1/partners`

- `GET /specialists`
- `POST /specialists/invite`
- `POST /specialists/{userId}`
- `POST /specialists/{userId}/remove`
- `GET /clients`
- `POST /clients`
- `POST /clients/{clientId}/activate`
- `POST /clients/{clientId}/deactivate`
- `POST /clients/{clientId}/token`
- `GET /clients/{clientId}/catalog`
- `PUT /clients/{clientId}/catalog`

## Tokens por cliente

Os tokens continuam armazenados apenas como SHA-256 Base64URL. O valor completo é retornado somente no momento da geração.

Cada token de cliente registra:

- empresa parceira proprietária;
- cliente do parceiro;
- provedor de integração;
- identificador externo da empresa cliente;
- hash do token;
- data de criação.

A rotação do token principal da integração não remove tokens específicos de clientes. Ao desativar um cliente, seus tokens são revogados.

## Isolamento do catálogo

A listagem externa da Gupy e da Recrutei é interceptada quando o token pertence a um cliente do parceiro. Nesse caso:

- somente avaliações publicadas e explicitamente liberadas são retornadas;
- busca e paginação são aplicadas depois do filtro de catálogo;
- tentativa de iniciar um teste não liberado retorna `404`;
- cliente desativado retorna acesso negado.

## Persistência

A migration `V1009_1__create_partner_distribution_module.sql` cria:

- `partner_clients`;
- `partner_catalog_access`;
- vínculo opcional de cliente em `integration_tokens`.
