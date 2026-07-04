# Capturas para apresentação pública

As imagens do README precisam provar o fluxo do produto, não apenas mostrar interface. Use uma empresa de demonstração e nunca exponha nomes, e-mails, links de candidato, tokens, resultados reais ou configurações de integração.

## Arquivos esperados

Salve os arquivos nesta pasta usando exatamente estes nomes:

1. `01-dashboard.png` — painel com indicadores agregados e dados fictícios.
2. `02-criacao-avaliacao.png` — etapa de criação contendo objetivo, competências e pesos.
3. `03-validador-publicacao.png` — revisão com blockers, warnings ou quality score, seguida da condição de publicação.
4. `04-experiencia-candidato.png` — uma questão pública com alternativas, sem token visível na URL.
5. `05-resultados-evidencias.png` — resultado por competência, evidências e campo de decisão do recrutador.
6. `06-integracoes-monitoramento.png` — central de integrações ou monitoramento de entregas, sempre sem segredos, URLs privadas ou tokens.

## Como capturar

- Use viewport de desktop de 1440 px de largura e zoom do navegador em 100%.
- Prefira PNG e remova barras, extensões ou notificações que revelem ambiente local, dados pessoais ou credenciais.
- Use cenários coerentes entre telas: a mesma avaliação demonstrada na criação deve aparecer no validador e no resultado.
- Mostre conteúdo suficiente para leitura, mas evite páginas excessivamente longas. Uma captura por tela é melhor do que vários recortes pequenos.
- Revise manualmente cada imagem antes do commit.

## Inserção no README

Após incluir as imagens, substitua a tabela da seção `Demonstração visual` por blocos como este:

```md
### Criação com critérios explícitos

![Criação de avaliação com competências e pesos](docs/screenshots/02-criacao-avaliacao.png)

O RH define o contexto, as competências e os critérios de pontuação antes da publicação.
```

Mantenha textos factuais. Não descreva como disponível uma capacidade que não esteja visível e funcional na versão demonstrada.
