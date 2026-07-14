# REQ-INTEGRACOES-REATIVACAO-TOKEN-ATS

## Objetivo

Corrigir a reativação das integrações Gupy e Recrutei para que a nova credencial não seja perdida e para que o sistema não declare conexão antes de uma comunicação real do ATS.

## Regras implementadas

- A reativação revoga imediatamente a credencial anterior e gera um token novo.
- O banco persiste somente o hash e a prévia segura do novo token.
- O token completo é incluído somente na resposta HTTP da reativação.
- Consultas posteriores da integração nunca retornam o token completo.
- O frontend apresenta o token retornado para cópia, usando o mesmo fluxo seguro de geração e rotação.
- A integração reativada fica em `PENDENTE`.
- A evidência de atividade anterior é limpa porque pertence à credencial revogada.
- A integração muda para `CONECTADA` somente quando o novo token for usado em uma requisição autenticada real.

## Critérios de aceite

### CA-01 — Retorno único do novo token

**Dado** que uma integração ATS está `DESATIVADA`  
**Quando** o usuário reativá-la  
**Então** o token anterior deve ser invalidado  
**E** um token novo deve ser retornado integralmente na resposta  
**E** esse valor deve ser apresentado ao usuário para cópia.

### CA-02 — Persistência segura

**Dado** que o token novo foi gerado  
**Quando** a reativação for persistida  
**Então** somente o hash e a prévia segura devem permanecer no banco  
**E** uma consulta posterior não deve retornar o token completo.

### CA-03 — Estado pendente

**Dado** que a integração foi reativada  
**E** a Gupy ou o Recrutei ainda não utilizou o token novo  
**Então** o status deve ser `PENDENTE`  
**E** a atividade anterior deve ser limpa  
**E** o sistema não deve mostrar a integração como conectada.

### CA-04 — Conexão posterior

**Dado** que a integração está `PENDENTE` após a reativação  
**Quando** o ATS fizer uma requisição autenticada usando o token novo  
**Então** o fluxo existente de registro de atividade poderá alterar o status para `CONECTADA`.
