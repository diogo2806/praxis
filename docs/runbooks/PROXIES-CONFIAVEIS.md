# Proxies confiáveis e endereço do cliente

O backend não lê `X-Forwarded-For` diretamente. O endereço usado em auditoria e rate limit vem de `HttpServletRequest.getRemoteAddr()`, depois que o Tomcat aplica sua política de proxy confiável.

## Configuração

A aplicação usa `server.forward-headers-strategy=native` e configura o `RemoteIpValve` do Tomcat. Por padrão, apenas loopback e redes privadas comuns são aceitos como proxies internos.

Quando a infraestrutura usar outra faixa, defina `PRAXIS_TRUSTED_PROXY_REGEX` com uma expressão regular que corresponda somente aos endereços dos proxies reversos controlados pela operação.

Exemplo para um proxy fixo:

```env
PRAXIS_TRUSTED_PROXY_REGEX=10\.20\.30\.40
```

Não use padrões amplos como `.*`, pois qualquer cliente passaria a conseguir controlar o endereço encaminhado.

## Reverse proxy

O proxy deve substituir ou acrescentar corretamente os cabeçalhos:

```nginx
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $scheme;
proxy_set_header Host $host;
```

A aplicação deve permanecer inacessível diretamente pela internet quando a implantação depender do endereço encaminhado pelo proxy.

## Validação operacional

1. Em uma chamada direta ao backend, envie um `X-Forwarded-For` arbitrário e confirme que o endereço registrado continua sendo o peer real.
2. Acesse pelo proxy autorizado e confirme que o endereço registrado corresponde ao cliente original.
3. Verifique que uma cadeia `cliente, proxy-intermediário` é processada apenas quando o peer imediato pertence à lista confiável.
4. Confirme que o rate limit de recuperação de senha não muda de chave ao alterar somente o cabeçalho em acesso direto.
