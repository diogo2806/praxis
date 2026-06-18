# Arquitetura de Outbox e Integracao ATS

## Estado Atual

- O backend possui outbox transacional para entrega assincrona de eventos.
- A integracao ATS registrada em runtime e a Gupy, via `GupyAdapter`.
- Nenhum adapter sem integracao real deve ser registrado como bean Spring.
- Novas plataformas de ATS devem entrar somente quando houver chamada real, contrato de autenticacao, tratamento de erro e teste de integracao do fluxo.

## Fluxo

1. O dominio cria ou atualiza a tentativa do candidato.
2. O evento de entrega e persistido no outbox na mesma transacao.
3. O processador assincrono le eventos pendentes.
4. A entrega chama o cliente real configurado para Gupy.
5. Falhas entram em retry ou DLQ conforme a politica do backend.

## Regra Para Novos ATS

Para adicionar um novo ATS, implemente `ATSAdapter` apenas depois de definir:

- endpoint real ou webhook real;
- credenciais/configuracao por tenant;
- mapeamento de payload de entrada e saida;
- timeout, retry e tratamento de erro;
- testes cobrindo sucesso e falha contra o cliente real ou contrato HTTP controlado.
