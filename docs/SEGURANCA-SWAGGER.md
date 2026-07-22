# Exposição segura do Swagger e OpenAPI

A documentação da API é desabilitada por padrão, inclusive em produção.

Para habilitá-la explicitamente em um ambiente interno, configure as duas variáveis:

```bash
SPRINGDOC_SWAGGER_UI_ENABLED=true
SPRINGDOC_API_DOCS_ENABLED=true
```

As rotas permanecem protegidas pelo Spring Security:

- `/docs` e `/docs/**` exigem JWT com role `ADMIN`;
- `/swagger-ui/**` exige JWT com role `ADMIN`;
- `/v3/api-docs/**` exige JWT com role `ADMIN`.

Não exponha essas rotas por regra pública no proxy reverso. O proxy deve encaminhar o cabeçalho `Authorization` ao backend.

Para desabilitar novamente, remova as variáveis ou defina ambas como `false`. O valor padrão do sistema é `false`.
