# Minuta — Consentimento e Termos para a vertical de Saúde (uso educativo)

> **Status: MINUTA para validação jurídica.** Os textos abaixo são propostas de
> redação. Não constituem aconselhamento jurídico e **não devem ir a produção sem
> revisão de advogado(a)** especializado(a) em proteção de dados e direito da
> saúde. Servem como ponto de partida para o time de Compliance/Jurídico (REQ-L6).

## 1. Por que este documento existe

A landing page oferece um caso de uso em saúde. Mesmo reposicionado como **material
de apoio educativo** (não diagnóstico, não conduta clínica, não dispositivo
médico), quando o participante é um **paciente** e os cenários tocam hábitos,
sintomas, tratamento ou bem-estar, há tratamento potencial de **dado pessoal
sensível de saúde** (LGPD, art. 5º, II e art. 11).

Isso muda o regime jurídico em relação ao recrutamento comum:

- **Base legal mais restrita.** Dado sensível, em regra, exige **consentimento
  específico e destacado** do titular (LGPD, art. 11, I), salvo hipóteses do art.
  11, II (ex.: tutela da saúde por profissional/serviço de saúde). A base usada
  precisa ser escolhida pelo Jurídico conforme o contexto real de cada cliente.
- **Consentimento informado.** Precisa ser livre, informado, inequívoco e para
  finalidade determinada; não pode ser genérico nem condição abusiva.
- **Menor de idade / vulnerável.** Se o paciente puder ser criança/adolescente,
  aplica-se o art. 14 da LGPD (melhor interesse, consentimento de responsável).
- **Fronteira regulatória.** O posicionamento educativo deve ser mantido com rigor
  para não caracterizar software como dispositivo médico (SaMD) sujeito à ANVISA.

## 2. Princípios de redação adotados

1. **Sem promessa de resultado clínico ou de saúde.**
2. **Finalidade explícita e limitada** (prática/educação), com menção a que não
   substitui avaliação profissional.
3. **Quem é o controlador**: a empresa/instituição que aplica a avaliação. A iForce
   (Práxis) atua como **operadora** (processadora), conforme contrato/DPA.
4. **Direitos do titular** sempre visíveis (acesso, correção, revogação, revisão
   humana).
5. **Granularidade**: consentimento para saúde é separado do aceite geral.

---

## 3. Minuta A — Consentimento do titular (paciente)

> Exibir **antes** do início da avaliação na vertical de saúde, com aceite ativo
> (checkbox não pré-marcado) e link para a política de privacidade do controlador.
> Registrar data, hora, versão do texto e identificador da participação.

**Título:** Antes de começar — uso dos seus dados nesta atividade

Esta atividade é um **exercício educativo de tomada de decisão**. Ela apresenta
situações do dia a dia para você praticar escolhas. **Não é uma consulta, não é
diagnóstico e não substitui a orientação de um profissional de saúde.**

Para realizar a atividade, **[NOME DO CONTROLADOR]** vai tratar respostas suas que
podem revelar informações relacionadas à sua saúde ou aos seus hábitos. Esses dados
serão usados **somente** para:

- gerar o resultado educativo desta atividade; e
- as finalidades descritas na Política de Privacidade de **[NOME DO CONTROLADOR]**.

**O que você precisa saber:**

- A pontuação segue critérios definidos antes da atividade. **Não há inteligência
  artificial julgando você.**
- Seus dados **não** serão usados para decidir, sozinhos e de forma automatizada,
  sobre tratamento, atendimento ou acesso a serviços.
- Você pode **pedir que uma pessoa revise** o resultado.
- Você pode **acessar, corrigir ou excluir** seus dados e **revogar este
  consentimento** a qualquer momento, pelo canal **[CANAL/E-MAIL DO ENCARREGADO]**.
  A revogação não afeta atividades já realizadas.
- Os dados serão mantidos por **[PERÍODO]** e depois anonimizados ou eliminados,
  conforme a Política de Privacidade.

☐ **Li e concordo** que **[NOME DO CONTROLADOR]** trate os dados sensíveis de saúde
informados por mim nesta atividade, exclusivamente para as finalidades educativas
acima.

[Continuar] · [Não concordo e sair]

> **Variante para responsável legal (menor/vulnerável):** trocar "por mim" por "pela
> pessoa sob minha responsabilidade" e coletar a identificação do responsável.

---

## 4. Minuta B — Aviso curto in-product (rodapé do candidato)

> Já implementado em versão genérica no fluxo do candidato. Para saúde, usar a
> variante abaixo quando a vertical estiver ativa.

Esta é uma atividade educativa. Seus dados são tratados apenas para esta finalidade,
conforme a LGPD e a política de **[NOME DO CONTROLADOR]**. Não é diagnóstico nem
substitui avaliação profissional. A decisão é de uma pessoa, não de um sistema
automático, e você pode pedir revisão humana.

---

## 5. Minuta C — Termo adicional do cliente contratante (controlador)

> Acréscimo ao Termo de Responsabilidade existente (`ResponsibilityTerm`),
> condicionado à habilitação da vertical de saúde. Exigir aceite específico do
> recrutador/administrador antes de publicar avaliação classificada como "saúde".

Ao usar a Práxis para avaliações na área de saúde, você declara e se responsabiliza
por:

1. **Finalidade educativa.** Utilizar a ferramenta como apoio educativo/treinamento
   de decisão, **sem** fins de diagnóstico, prescrição ou conduta clínica.
2. **Papel de controlador.** Você é o controlador dos dados dos participantes e
   define finalidade, base legal e período de retenção. A iForce/Práxis atua como
   **operadora**, nos limites do contrato e do DPA.
3. **Base legal e consentimento.** Garantir base legal válida para dado sensível de
   saúde (LGPD, art. 11) e coletar o consentimento específico quando aplicável,
   inclusive para menores/vulneráveis (art. 14).
4. **Não automatização de decisão sensível.** Não usar o resultado, isoladamente,
   para decisão automatizada sobre tratamento, atendimento ou acesso a serviços de
   saúde, assegurando revisão humana.
5. **Conteúdo dos cenários.** Responder pelo conteúdo dos cenários e critérios, sem
   linguagem que prometa cura, resultado clínico ou que possa causar dano.
6. **Transparência ao titular.** Disponibilizar política de privacidade e canal do
   Encarregado (DPO) aos participantes.

☐ Li e aceito as condições acima para uso da Práxis na vertical de saúde.

---

## 6. Onde cada texto entra no fluxo

| Texto | Audiência | Momento | Como registrar |
| --- | --- | --- | --- |
| Minuta A | Paciente | Antes da 1ª etapa, vertical de saúde | Aceite ativo + versão + timestamp |
| Minuta B | Paciente | Rodapé durante a atividade | Exibição (não exige aceite) |
| Minuta C | Cliente (controlador) | Antes de publicar avaliação "saúde" | Estende `ResponsibilityTerm` (novo `TYPE`/`VERSION`) |

## 7. Pontos em aberto para o Jurídico decidir

1. **Base legal por contexto.** Consentimento (art. 11, I) vs. tutela da saúde por
   serviço/profissional (art. 11, II) — depende de quem é o cliente.
2. **Operador × controlador.** Confirmar enquadramento e refletir no DPA.
3. **Menores e vulneráveis.** Definir se a vertical pode atendê-los e o fluxo de
   consentimento do responsável.
4. **Enquadramento ANVISA.** Validar que o posicionamento educativo afasta
   classificação como dispositivo médico (SaMD).
5. **Retenção e anonimização.** Definir prazos específicos para dado de saúde
   (hoje o padrão é `PRAXIS_PRIVACY_RETENTION_DAYS`).
6. **Necessidade de gate técnico.** Decidir se a vertical de saúde deve ser uma
   trava de produto (só habilita após aceite da Minuta C e configuração da Minuta A).

## 8. Implementação (entregue)

> **Status: implementado** (validação jurídica registrada). A vertical permanece
> **desligada por padrão** em todos os empresas; habilite-a conscientemente por empresa.

O que foi construído:

- **Termo `HEALTH_USE`** (Minuta C) no domínio `term`, espelhando `ResponsibilityTerm`
  (`HealthUseTerm`, texto + versão + registro de aceite por usuário). Endpoints:
  `GET/POST /api/v1/terms/health-use[/acceptance]`.
- **Flag por empresa** `empresas.health_vertical` (migração `V38`, default `false`),
  resolvida por `HealthVerticalService`.
- **Trava de publicação** (Minuta C): `SimulationAdminService.publishVersion` bloqueia
  com `409` quando o empresa é da vertical de saúde e o recrutador não aceitou o termo
  corrente. A tela `/nova/governanca` mostra o aceite quando o `409` ocorre.
- **Consentimento do paciente** (Minuta A): o estado da tentativa expõe `verticalSaude`;
  o fluxo do candidato (`/candidato/$token`) exibe o aviso de consentimento antes de
  iniciar (cronômetro pausado) e troca o rodapé para a Minuta B. O aceite é registrado
  na trilha via `POST /candidate/attempts/{id}/health-consent` (evento de auditoria
  `healthConsentRecorded`, com versão do aviso, timestamp e marcação de responsável legal).

### Como habilitar a vertical para um empresa

```sql
UPDATE empresas SET health_vertical = TRUE WHERE id = '<empresa-id>';
```

A partir daí: o recrutador precisa aceitar o termo de uso em saúde para publicar, e o
participante precisa consentir antes de iniciar a atividade.

### Pendências para evolução futura

- UI administrativa para alternar a flag por empresa (hoje via SQL/configuração).
- Versionamento do texto da Minuta A em fonte única (hoje o texto é copy de UI e a
  versão é a constante `HEALTH_CONSENT_VERSION` no frontend / `noticeVersion` na trilha).
- Fluxo dedicado de identificação do responsável legal (menor/vulnerável) além do
  checkbox atual.
