# Pendências externas obrigatórias antes da operação comercial

As correções técnicas deste repositório não substituem validações, informações e
assinaturas que dependem da iForce, dos clientes, dos fornecedores e de
profissionais habilitados.

## Bloqueios de negócio

1. Informar razão social, CNPJ, endereço, contato de suporte e foro aplicável no
   contrato comercial.
2. Nomear ou formalizar o canal do Encarregado/DPO conforme o enquadramento da
   empresa.
3. Validar e assinar Política de Privacidade, Termos, DPA e RIPD.
4. Preencher os fornecedores reais de infraestrutura, banco, backup, e-mail,
   observabilidade e armazenamento, incluindo país e região.
5. Definir o mecanismo aplicável a cada transferência internacional.
6. Definir SLA contratual de incidente da operadora para a controladora.
7. Definir política comercial de cancelamento, reembolso, créditos não usados,
   parcelamento, inadimplência e emissão fiscal.
8. Homologar formalmente integrações com ATS antes de anunciá-las como
   homologadas.
9. Manter a vertical de saúde desabilitada até parecer jurídico e regulatório
   formal, aprovação registrada e configuração completa do controlador.
10. Não admitir menores ou vulneráveis na vertical de saúde até existir fluxo de
    identificação e validação do responsável legal.

## Controles técnicos entregues

- configuração de controlador e canal por empresa;
- ciência versionada do aviso antes da avaliação;
- workflow de direitos do titular com prazo e responsável;
- workflow de revisão humana e bloqueio de pontuação externa;
- liberação idempotente do resultado após revisão;
- anonimização abrangente de respostas, pontuações, URLs e identificadores;
- registro de incidentes com retenção mínima de cinco anos;
- gate formal para a vertical de saúde;
- páginas públicas de Termos, Cookies e Suboperadores;
- redução da persistência do JWT no navegador e cabeçalhos de segurança.

## Regra operacional

Produção deve manter `PRAXIS_PRIVACY_ENFORCE_READINESS=true`. Cada empresa deve
configurar o endpoint `PUT /api/v1/privacy/configuration` antes de enviar o
primeiro convite real. A ausência de configuração impede a abertura da avaliação.

Última revisão técnica: 20/07/2026.
