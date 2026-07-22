# Runbook — migrations Flyway fora de ordem

## Regra operacional

O Práxis mantém `spring.flyway.out-of-order=false` por padrão e força esse valor no perfil `prod`. Uma implantação normal de produção deve rejeitar migrations com versão inferior à versão mais alta já aplicada.

## Diagnóstico

Quando o startup falhar por migration fora de ordem:

1. interrompa a implantação;
2. compare a migration rejeitada com o histórico de `flyway_schema_history`;
3. confirme se existe colisão de versão, arquivo renumerado ou migration adicionada tardiamente;
4. prefira renumerar a migration para uma versão posterior ainda não utilizada;
5. valide a sequência e a unicidade das migrations antes de uma nova implantação.

Não edite manualmente `flyway_schema_history` e não habilite `out-of-order` no perfil `prod`.

## Exceção temporária

Uma exceção somente pode ser executada em ambiente controlado e fora do perfil `prod`, após revisão técnica e backup validado. Registre no chamado de mudança:

- migration e versão envolvidas;
- motivo pelo qual a renumeração não é possível;
- ambiente e janela de execução;
- responsável pela aprovação;
- evidência do backup;
- plano de validação e reversão.

Para a janela excepcional, defina `SPRING_FLYWAY_OUT_OF_ORDER=true` sem ativar o perfil `prod`. Após a execução:

1. remova imediatamente a variável;
2. restaure `SPRING_FLYWAY_OUT_OF_ORDER=false`;
3. valide `flyway_schema_history` e o estado funcional do banco;
4. anexe as evidências ao chamado;
5. confirme que o próximo startup ocorre com migrations fora de ordem desabilitadas.

## Validação antes do deploy

Execute no backend:

```bash
./mvnw test -Dtest=FlywayConfigurationTest
```

A implantação de produção deve usar o perfil `prod`. O arquivo `application-prod.properties` fixa `spring.flyway.out-of-order=false`, impedindo que uma variável de ambiente permissiva substitua a proteção.
