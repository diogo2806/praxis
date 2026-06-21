import type { Language } from "./translations";

export interface HelpStep {
  title: string;
  description: string;
}

export interface HelpContent {
  title: string;
  description: string;
  steps: HelpStep[];
}

type HelpMap = Record<string, HelpContent>;

const ptBr: HelpMap = {
  "/app": {
    title: "Painel Principal",
    description:
      "Visão geral de todos os testes criados na plataforma. Aqui você acompanha o status, acessa resultados e gerencia seus testes situacionais.",
    steps: [
      {
        title: "Visualizar testes",
        description:
          "A tabela principal lista todos os testes cadastrados com status, data de criação e número de candidatos.",
      },
      {
        title: "Criar novo teste",
        description:
          'Clique em "Criar novo teste" para iniciar o assistente de criação passo a passo.',
      },
      {
        title: "Filtrar e buscar",
        description:
          "Use a barra de busca e os filtros para encontrar testes específicos por nome, status ou data.",
      },
      {
        title: "Ações rápidas",
        description:
          "Em cada linha da tabela, acesse opções como editar, monitorar ou excluir um teste.",
      },
    ],
  },
  "/comecar": {
    title: "Primeiros Passos",
    description:
      "Guia introdutório da plataforma Práxis. Entenda o que é, como funciona e por onde começar.",
    steps: [
      {
        title: "Entender o Práxis",
        description:
          "Leia a explicação sobre o que é um teste situacional e como ele avalia candidatos de forma justa.",
      },
      {
        title: "Conhecer o fluxo",
        description:
          "Veja o passo a passo desde a criação do teste até a análise de resultados.",
      },
      {
        title: "Iniciar sua primeira avaliação",
        description:
          "Use os botões de ação rápida para criar seu primeiro teste ou explorar exemplos.",
      },
    ],
  },
  "/nova/blueprint": {
    title: "Plano do Teste",
    description:
      "Primeira etapa da criação do teste. Defina o cargo, a situação crítica, as competências avaliadas e o uso pretendido do resultado.",
    steps: [
      {
        title: "Definir o cargo",
        description:
          "Informe o cargo para o qual o teste será aplicado. Isso contextualiza a situação.",
      },
      {
        title: "Descrever a situação crítica",
        description:
          "Escreva o cenário real do trabalho que o candidato enfrentará durante o teste.",
      },
      {
        title: "Selecionar competências",
        description:
          "Escolha as competências que serão avaliadas neste teste a partir do catálogo.",
      },
      {
        title: "Definir uso do resultado",
        description:
          "Indique como o resultado será utilizado no processo seletivo.",
      },
    ],
  },
  "/nova/objetivo": {
    title: "Objetivos e Competências",
    description:
      "Defina os objetivos de avaliação e configure as competências que serão medidas pelo teste.",
    steps: [
      {
        title: "Configurar objetivos",
        description:
          "Estabeleça o que você espera avaliar com este teste situacional.",
      },
      {
        title: "Vincular competências",
        description:
          "Associe competências do catálogo aos objetivos definidos.",
      },
      {
        title: "Definir pesos",
        description:
          "Atribua pesos para cada competência conforme a importância para o cargo.",
      },
    ],
  },
  "/nova/personagem": {
    title: "Personagem e Primeiro Turno",
    description:
      "Configure o personagem que apresentará a situação ao candidato e defina o primeiro turno do diálogo.",
    steps: [
      {
        title: "Criar o personagem",
        description:
          "Defina nome, cargo e perfil do personagem que conduzirá a conversa.",
      },
      {
        title: "Escrever o primeiro turno",
        description:
          "Redija a mensagem inicial que apresenta a situação ao candidato.",
      },
      {
        title: "Revisar o tom",
        description:
          "Verifique se o tom é profissional e adequado ao contexto do cargo.",
      },
    ],
  },
  "/nova/dialogo": {
    title: "Editor de Diálogo",
    description:
      "Monte a árvore de diálogo com turnos, alternativas de resposta e critérios de pontuação para cada opção.",
    steps: [
      {
        title: "Adicionar turnos",
        description:
          "Crie os turnos de conversa que compõem o cenário do teste.",
      },
      {
        title: "Criar alternativas",
        description:
          "Para cada turno, defina as opções de resposta disponíveis para o candidato.",
      },
      {
        title: "Configurar pontuação",
        description:
          "Atribua pontuações baseadas em critérios objetivos para cada alternativa.",
      },
      {
        title: "Visualizar fluxo",
        description:
          "Use a visualização em árvore para conferir o fluxo completo do diálogo.",
      },
    ],
  },
  "/nova/validador": {
    title: "Validador de Qualidade",
    description:
      "Verifique a qualidade do teste antes de publicá-lo. O validador analisa completude, consistência e aderência às boas práticas.",
    steps: [
      {
        title: "Executar validação",
        description:
          "Clique em validar para analisar automaticamente a qualidade do teste.",
      },
      {
        title: "Revisar alertas",
        description:
          "Verifique os alertas e bloqueios apontados pelo validador.",
      },
      {
        title: "Corrigir problemas",
        description:
          "Volte às etapas anteriores para corrigir os itens apontados.",
      },
    ],
  },
  "/nova/piloto": {
    title: "Piloto e Calibração",
    description:
      "Acompanhe a aplicação piloto do teste e calibre os critérios de pontuação com base nos resultados iniciais.",
    steps: [
      {
        title: "Iniciar piloto",
        description:
          "Envie o teste para um grupo piloto de candidatos para calibração.",
      },
      {
        title: "Analisar resultados",
        description:
          "Revise os resultados do piloto para identificar ajustes necessários.",
      },
      {
        title: "Calibrar pontuação",
        description:
          "Ajuste os critérios e pesos de pontuação conforme necessário.",
      },
    ],
  },
  "/nova/mapa": {
    title: "Mapa de Pontuação",
    description:
      "Visualize como os critérios de pontuação se distribuem pelo teste. Confira se a cobertura está adequada.",
    steps: [
      {
        title: "Visualizar mapa",
        description:
          "Veja a distribuição de pontos por competência e alternativa.",
      },
      {
        title: "Identificar lacunas",
        description:
          "Verifique se há competências sem cobertura adequada de pontuação.",
      },
      {
        title: "Ajustar distribuição",
        description:
          "Volte ao editor de diálogo para balancear a distribuição de pontos se necessário.",
      },
    ],
  },
  "/nova/governanca": {
    title: "Governança e Publicação",
    description:
      "Gerencie versões do teste, controle o histórico de alterações e publique a versão final.",
    steps: [
      {
        title: "Revisar histórico",
        description:
          "Confira todas as alterações feitas no teste ao longo do tempo.",
      },
      {
        title: "Aprovar versão",
        description:
          "Marque a versão como aprovada para publicação após revisão.",
      },
      {
        title: "Publicar teste",
        description:
          "Publique o teste para que possa ser enviado aos candidatos.",
      },
    ],
  },
  "/nova/gupy": {
    title: "Integração Gupy",
    description:
      "Configure e ative a integração com a plataforma Gupy para envio automático de resultados.",
    steps: [
      {
        title: "Verificar conexão",
        description:
          "Confira se a integração com a Gupy está configurada e ativa.",
      },
      {
        title: "Mapear campos",
        description:
          "Associe os campos do Práxis aos campos correspondentes na Gupy.",
      },
      {
        title: "Ativar envio",
        description:
          "Ative o envio automático de resultados para a plataforma Gupy.",
      },
    ],
  },
  "/nova/competencias": {
    title: "Catálogo de Competências",
    description:
      "Gerencie o catálogo de competências da sua empresa. Adicione, edite ou remova competências usadas nos testes.",
    steps: [
      {
        title: "Visualizar catálogo",
        description:
          "Veja todas as competências cadastradas e suas descrições.",
      },
      {
        title: "Adicionar competência",
        description:
          'Clique em "Nova competência" para cadastrar uma nova competência ao catálogo.',
      },
      {
        title: "Editar competência",
        description:
          "Clique em uma competência existente para editar seu nome ou descrição.",
      },
      {
        title: "Remover competência",
        description:
          "Remova competências que não são mais utilizadas nos testes.",
      },
    ],
  },
  "/enviar-link": {
    title: "Enviar Link ao Candidato",
    description:
      "Gere e envie links de convite para que candidatos realizem o teste situacional.",
    steps: [
      {
        title: "Selecionar teste",
        description:
          "Escolha qual teste publicado será enviado ao candidato.",
      },
      {
        title: "Gerar link",
        description:
          "Gere um link único de acesso para o candidato realizar o teste.",
      },
      {
        title: "Enviar convite",
        description:
          "Copie o link ou envie diretamente por e-mail para o candidato.",
      },
    ],
  },
  "/monitoramento": {
    title: "Monitoramento",
    description:
      "Acompanhe em tempo real os testes em andamento. Veja quem iniciou, quem concluiu e os resultados parciais.",
    steps: [
      {
        title: "Visualizar status",
        description:
          "Veja o status de cada candidato: pendente, em andamento ou concluído.",
      },
      {
        title: "Acompanhar progresso",
        description:
          "Monitore o progresso de cada candidato durante a aplicação do teste.",
      },
      {
        title: "Acessar resultados",
        description:
          "Clique em um candidato concluído para ver o resultado detalhado.",
      },
    ],
  },
  "/talent-match": {
    title: "Talent Match",
    description:
      "Compare candidatos lado a lado com base nos resultados dos testes. Identifique o melhor perfil para a vaga.",
    steps: [
      {
        title: "Selecionar candidatos",
        description:
          "Escolha dois ou mais candidatos para comparação direta.",
      },
      {
        title: "Comparar resultados",
        description:
          "Visualize as pontuações por competência em formato comparativo.",
      },
      {
        title: "Analisar gráficos",
        description:
          "Use os gráficos para identificar pontos fortes e fracos de cada candidato.",
      },
    ],
  },
  "/governanca": {
    title: "Governança e Auditoria",
    description:
      "Central de governança com histórico completo de versões, trilha de auditoria e controle de alterações.",
    steps: [
      {
        title: "Consultar histórico",
        description:
          "Veja todas as versões publicadas e o histórico de alterações de cada teste.",
      },
      {
        title: "Trilha de auditoria",
        description:
          "Acesse o registro detalhado de quem alterou o quê e quando.",
      },
      {
        title: "Comparar versões",
        description:
          "Compare duas versões para identificar as diferenças entre elas.",
      },
    ],
  },
  "/compliance": {
    title: "Compliance",
    description:
      "Documentação de conformidade do Práxis. LGPD, transparência do resultado e base técnica da avaliação.",
    steps: [
      {
        title: "Consultar políticas",
        description:
          "Acesse as políticas de conformidade e proteção de dados da plataforma.",
      },
      {
        title: "Transparência do resultado",
        description:
          "Entenda como os resultados são calculados e apresentados ao candidato.",
      },
      {
        title: "Base técnica",
        description:
          "Veja a fundamentação técnica que sustenta a metodologia de avaliação.",
      },
    ],
  },
  "/defensabilidade": {
    title: "Defensabilidade",
    description:
      "Análise de confiabilidade e segurança técnica dos resultados. Demonstre que a pontuação é objetiva e auditável.",
    steps: [
      {
        title: "Selecionar teste",
        description:
          "Escolha um teste para ver sua análise de defensabilidade.",
      },
      {
        title: "Verificar indicadores",
        description:
          "Confira os indicadores de construto definido, pontuação auditável e determinística.",
      },
      {
        title: "Exportar relatório",
        description:
          "Gere um relatório de defensabilidade para documentação externa.",
      },
    ],
  },
  "/lgpd": {
    title: "LGPD e Transparência",
    description:
      "Informações sobre proteção de dados pessoais (LGPD) e transparência na apresentação dos resultados.",
    steps: [
      {
        title: "Consultar diretrizes",
        description:
          "Veja como o Práxis trata e protege os dados pessoais dos candidatos.",
      },
      {
        title: "Transparência",
        description:
          "Entenda como os resultados são apresentados de forma transparente.",
      },
      {
        title: "Direitos do candidato",
        description:
          "Conheça os direitos do candidato em relação aos seus dados.",
      },
    ],
  },
  "/configuracoes": {
    title: "Configurações",
    description:
      "Gerencie o perfil da empresa, configurações de integração e preferências da plataforma.",
    steps: [
      {
        title: "Perfil da empresa",
        description:
          "Atualize o nome, logo e informações da sua empresa.",
      },
      {
        title: "Integrações",
        description:
          "Configure integrações com plataformas externas como Gupy.",
      },
      {
        title: "Preferências",
        description:
          "Ajuste idioma, notificações e outras preferências da plataforma.",
      },
    ],
  },
  "/candidato": {
    title: "Área do Candidato",
    description:
      "Visualização da experiência do candidato durante o teste situacional.",
    steps: [
      {
        title: "Visão do candidato",
        description:
          "Veja exatamente como o candidato visualiza e interage com o teste.",
      },
      {
        title: "Fluxo do teste",
        description:
          "Acompanhe o fluxo completo que o candidato percorre ao responder.",
      },
    ],
  },
};

const enUs: HelpMap = {
  "/app": {
    title: "Main Dashboard",
    description:
      "Overview of all tests created on the platform. Track statuses, access results and manage your situational tests.",
    steps: [
      {
        title: "View tests",
        description:
          "The main table lists all registered tests with status, creation date and number of candidates.",
      },
      {
        title: "Create new test",
        description:
          'Click "Create new test" to start the step-by-step creation wizard.',
      },
      {
        title: "Filter and search",
        description:
          "Use the search bar and filters to find specific tests by name, status or date.",
      },
      {
        title: "Quick actions",
        description:
          "In each table row, access options such as edit, monitor or delete a test.",
      },
    ],
  },
  "/comecar": {
    title: "Getting Started",
    description:
      "Introductory guide to the Praxis platform. Understand what it is, how it works and where to start.",
    steps: [
      {
        title: "Understand Praxis",
        description:
          "Read about what a situational test is and how it fairly evaluates candidates.",
      },
      {
        title: "Learn the flow",
        description:
          "See the step-by-step process from test creation to results analysis.",
      },
      {
        title: "Start your first assessment",
        description:
          "Use the quick action buttons to create your first test or explore examples.",
      },
    ],
  },
  "/nova/blueprint": {
    title: "Assessment Plan",
    description:
      "First step in test creation. Define the role, critical situation, evaluated competencies and intended result usage.",
    steps: [
      {
        title: "Define the role",
        description: "Enter the role for which the test will be applied.",
      },
      {
        title: "Describe the critical situation",
        description:
          "Write the real work scenario the candidate will face during the test.",
      },
      {
        title: "Select competencies",
        description:
          "Choose the competencies to be evaluated from the catalog.",
      },
      {
        title: "Define result usage",
        description:
          "Indicate how the result will be used in the selection process.",
      },
    ],
  },
  "/nova/objetivo": {
    title: "Objectives and Competencies",
    description:
      "Define evaluation objectives and configure the competencies that will be measured by the test.",
    steps: [
      {
        title: "Configure objectives",
        description: "Set what you expect to evaluate with this situational test.",
      },
      {
        title: "Link competencies",
        description: "Associate catalog competencies with defined objectives.",
      },
      {
        title: "Set weights",
        description:
          "Assign weights to each competency according to importance for the role.",
      },
    ],
  },
  "/nova/personagem": {
    title: "Character and First Turn",
    description:
      "Set up the character who will present the situation to the candidate and define the first dialogue turn.",
    steps: [
      {
        title: "Create the character",
        description: "Define name, role and profile of the conversation character.",
      },
      {
        title: "Write the first turn",
        description:
          "Write the opening message that presents the situation to the candidate.",
      },
      {
        title: "Review the tone",
        description:
          "Check that the tone is professional and appropriate for the role context.",
      },
    ],
  },
  "/nova/dialogo": {
    title: "Dialogue Editor",
    description:
      "Build the dialogue tree with turns, response alternatives and scoring criteria for each option.",
    steps: [
      {
        title: "Add turns",
        description: "Create conversation turns that make up the test scenario.",
      },
      {
        title: "Create alternatives",
        description:
          "For each turn, define the response options available to the candidate.",
      },
      {
        title: "Configure scoring",
        description:
          "Assign scores based on objective criteria for each alternative.",
      },
      {
        title: "Visualize flow",
        description: "Use the tree view to check the complete dialogue flow.",
      },
    ],
  },
  "/nova/validador": {
    title: "Quality Validator",
    description:
      "Check test quality before publishing. The validator analyzes completeness, consistency and best practice adherence.",
    steps: [
      {
        title: "Run validation",
        description: "Click validate to automatically analyze test quality.",
      },
      {
        title: "Review alerts",
        description: "Check the alerts and blockers identified by the validator.",
      },
      {
        title: "Fix issues",
        description: "Go back to previous steps to fix the flagged items.",
      },
    ],
  },
  "/nova/piloto": {
    title: "Pilot and Calibration",
    description:
      "Monitor pilot test application and calibrate scoring criteria based on initial results.",
    steps: [
      {
        title: "Start pilot",
        description: "Send the test to a pilot group of candidates for calibration.",
      },
      {
        title: "Analyze results",
        description: "Review pilot results to identify necessary adjustments.",
      },
      {
        title: "Calibrate scoring",
        description: "Adjust scoring criteria and weights as needed.",
      },
    ],
  },
  "/nova/mapa": {
    title: "Scoring Map",
    description:
      "Visualize how scoring criteria are distributed across the test. Check if coverage is adequate.",
    steps: [
      {
        title: "View map",
        description: "See point distribution by competency and alternative.",
      },
      {
        title: "Identify gaps",
        description: "Check if any competencies lack adequate scoring coverage.",
      },
      {
        title: "Adjust distribution",
        description:
          "Go back to the dialogue editor to balance point distribution if needed.",
      },
    ],
  },
  "/nova/governanca": {
    title: "Governance and Publication",
    description:
      "Manage test versions, track change history and publish the final version.",
    steps: [
      {
        title: "Review history",
        description: "Check all changes made to the test over time.",
      },
      {
        title: "Approve version",
        description: "Mark the version as approved for publication after review.",
      },
      {
        title: "Publish test",
        description: "Publish the test so it can be sent to candidates.",
      },
    ],
  },
  "/nova/gupy": {
    title: "Gupy Integration",
    description:
      "Configure and activate integration with the Gupy platform for automatic result submission.",
    steps: [
      {
        title: "Check connection",
        description: "Verify that the Gupy integration is configured and active.",
      },
      {
        title: "Map fields",
        description: "Associate Praxis fields with corresponding Gupy fields.",
      },
      {
        title: "Activate submission",
        description: "Activate automatic result submission to the Gupy platform.",
      },
    ],
  },
  "/nova/competencias": {
    title: "Competency Catalog",
    description:
      "Manage your company's competency catalog. Add, edit or remove competencies used in tests.",
    steps: [
      {
        title: "View catalog",
        description: "See all registered competencies and their descriptions.",
      },
      {
        title: "Add competency",
        description:
          'Click "New competency" to register a new competency to the catalog.',
      },
      {
        title: "Edit competency",
        description:
          "Click on an existing competency to edit its name or description.",
      },
      {
        title: "Remove competency",
        description: "Remove competencies that are no longer used in tests.",
      },
    ],
  },
  "/enviar-link": {
    title: "Send Link to Candidate",
    description:
      "Generate and send invitation links for candidates to take the situational test.",
    steps: [
      {
        title: "Select test",
        description: "Choose which published test will be sent to the candidate.",
      },
      {
        title: "Generate link",
        description: "Generate a unique access link for the candidate to take the test.",
      },
      {
        title: "Send invitation",
        description: "Copy the link or send it directly by email to the candidate.",
      },
    ],
  },
  "/monitoramento": {
    title: "Monitoring",
    description:
      "Track tests in progress in real time. See who started, who completed and partial results.",
    steps: [
      {
        title: "View status",
        description:
          "See each candidate's status: pending, in progress or completed.",
      },
      {
        title: "Track progress",
        description: "Monitor each candidate's progress during test application.",
      },
      {
        title: "Access results",
        description: "Click on a completed candidate to see detailed results.",
      },
    ],
  },
  "/talent-match": {
    title: "Talent Match",
    description:
      "Compare candidates side by side based on test results. Identify the best profile for the position.",
    steps: [
      {
        title: "Select candidates",
        description: "Choose two or more candidates for direct comparison.",
      },
      {
        title: "Compare results",
        description: "View scores by competency in comparative format.",
      },
      {
        title: "Analyze charts",
        description:
          "Use charts to identify each candidate's strengths and weaknesses.",
      },
    ],
  },
  "/governanca": {
    title: "Governance and Audit",
    description:
      "Governance center with complete version history, audit trail and change control.",
    steps: [
      {
        title: "Check history",
        description:
          "See all published versions and change history for each test.",
      },
      {
        title: "Audit trail",
        description: "Access detailed records of who changed what and when.",
      },
      {
        title: "Compare versions",
        description: "Compare two versions to identify differences between them.",
      },
    ],
  },
  "/compliance": {
    title: "Compliance",
    description:
      "Praxis compliance documentation. Data protection, result transparency and technical basis of the assessment.",
    steps: [
      {
        title: "Check policies",
        description: "Access the platform's compliance and data protection policies.",
      },
      {
        title: "Result transparency",
        description:
          "Understand how results are calculated and presented to the candidate.",
      },
      {
        title: "Technical basis",
        description:
          "See the technical foundation supporting the assessment methodology.",
      },
    ],
  },
  "/defensabilidade": {
    title: "Defensibility",
    description:
      "Reliability and technical security analysis of results. Demonstrate that scoring is objective and auditable.",
    steps: [
      {
        title: "Select test",
        description: "Choose a test to view its defensibility analysis.",
      },
      {
        title: "Check indicators",
        description:
          "Review defined construct, auditable score and deterministic scoring indicators.",
      },
      {
        title: "Export report",
        description: "Generate a defensibility report for external documentation.",
      },
    ],
  },
  "/lgpd": {
    title: "Data Protection & Transparency",
    description:
      "Information about personal data protection (LGPD) and transparency in result presentation.",
    steps: [
      {
        title: "Check guidelines",
        description: "See how Praxis handles and protects candidate personal data.",
      },
      {
        title: "Transparency",
        description: "Understand how results are presented transparently.",
      },
      {
        title: "Candidate rights",
        description: "Learn about candidate rights regarding their data.",
      },
    ],
  },
  "/configuracoes": {
    title: "Settings",
    description:
      "Manage company profile, integration settings and platform preferences.",
    steps: [
      {
        title: "Company profile",
        description: "Update your company's name, logo and information.",
      },
      {
        title: "Integrations",
        description: "Configure integrations with external platforms like Gupy.",
      },
      {
        title: "Preferences",
        description: "Adjust language, notifications and other platform preferences.",
      },
    ],
  },
  "/candidato": {
    title: "Candidate Area",
    description:
      "View of the candidate experience during the situational test.",
    steps: [
      {
        title: "Candidate view",
        description:
          "See exactly how the candidate views and interacts with the test.",
      },
      {
        title: "Test flow",
        description: "Follow the complete flow the candidate goes through when responding.",
      },
    ],
  },
};

const esMx: HelpMap = {
  "/app": {
    title: "Panel Principal",
    description:
      "Visión general de todas las pruebas creadas en la plataforma. Aquí acompañas el estado, accedes a resultados y gestionas tus pruebas situacionales.",
    steps: [
      {
        title: "Ver pruebas",
        description:
          "La tabla principal lista todas las pruebas registradas con estado, fecha de creación y número de candidatos.",
      },
      {
        title: "Crear nueva prueba",
        description:
          'Haz clic en "Crear nueva prueba" para iniciar el asistente de creación paso a paso.',
      },
      {
        title: "Filtrar y buscar",
        description:
          "Usa la barra de búsqueda y los filtros para encontrar pruebas específicas por nombre, estado o fecha.",
      },
      {
        title: "Acciones rápidas",
        description:
          "En cada fila de la tabla, accede a opciones como editar, monitorear o eliminar una prueba.",
      },
    ],
  },
  "/comecar": {
    title: "Primeros Pasos",
    description:
      "Guía introductoria de la plataforma Praxis. Entiende qué es, cómo funciona y por dónde empezar.",
    steps: [
      {
        title: "Entender Praxis",
        description:
          "Lee la explicación sobre qué es una prueba situacional y cómo evalúa candidatos de forma justa.",
      },
      {
        title: "Conocer el flujo",
        description:
          "Ve el paso a paso desde la creación de la prueba hasta el análisis de resultados.",
      },
      {
        title: "Iniciar tu primera evaluación",
        description:
          "Usa los botones de acción rápida para crear tu primera prueba o explorar ejemplos.",
      },
    ],
  },
  "/nova/blueprint": {
    title: "Plan de Evaluación",
    description:
      "Primer paso en la creación de la prueba. Define el puesto, la situación crítica, las competencias evaluadas y el uso previsto del resultado.",
    steps: [
      {
        title: "Definir el puesto",
        description: "Informa el puesto para el cual se aplicará la prueba.",
      },
      {
        title: "Describir la situación crítica",
        description:
          "Escribe el escenario real de trabajo que el candidato enfrentará durante la prueba.",
      },
      {
        title: "Seleccionar competencias",
        description: "Elige las competencias a evaluar del catálogo.",
      },
      {
        title: "Definir uso del resultado",
        description:
          "Indica cómo se utilizará el resultado en el proceso de selección.",
      },
    ],
  },
  "/nova/objetivo": {
    title: "Objetivos y Competencias",
    description:
      "Define los objetivos de evaluación y configura las competencias que serán medidas por la prueba.",
    steps: [
      {
        title: "Configurar objetivos",
        description: "Establece lo que esperas evaluar con esta prueba situacional.",
      },
      {
        title: "Vincular competencias",
        description: "Asocia competencias del catálogo con los objetivos definidos.",
      },
      {
        title: "Definir pesos",
        description:
          "Asigna pesos a cada competencia según la importancia para el puesto.",
      },
    ],
  },
  "/nova/personagem": {
    title: "Personaje y Primer Turno",
    description:
      "Configura el personaje que presentará la situación al candidato y define el primer turno del diálogo.",
    steps: [
      {
        title: "Crear el personaje",
        description: "Define nombre, puesto y perfil del personaje de la conversación.",
      },
      {
        title: "Escribir el primer turno",
        description:
          "Redacta el mensaje inicial que presenta la situación al candidato.",
      },
      {
        title: "Revisar el tono",
        description:
          "Verifica que el tono sea profesional y adecuado al contexto del puesto.",
      },
    ],
  },
  "/nova/dialogo": {
    title: "Editor de Diálogo",
    description:
      "Monta el árbol de diálogo con turnos, alternativas de respuesta y criterios de puntuación para cada opción.",
    steps: [
      {
        title: "Agregar turnos",
        description: "Crea los turnos de conversación que componen el escenario de la prueba.",
      },
      {
        title: "Crear alternativas",
        description:
          "Para cada turno, define las opciones de respuesta disponibles para el candidato.",
      },
      {
        title: "Configurar puntuación",
        description:
          "Asigna puntuaciones basadas en criterios objetivos para cada alternativa.",
      },
      {
        title: "Visualizar flujo",
        description:
          "Usa la vista de árbol para verificar el flujo completo del diálogo.",
      },
    ],
  },
  "/nova/validador": {
    title: "Validador de Calidad",
    description:
      "Verifica la calidad de la prueba antes de publicarla. El validador analiza completitud, consistencia y adherencia a buenas prácticas.",
    steps: [
      {
        title: "Ejecutar validación",
        description: "Haz clic en validar para analizar automáticamente la calidad de la prueba.",
      },
      {
        title: "Revisar alertas",
        description: "Verifica las alertas y bloqueos identificados por el validador.",
      },
      {
        title: "Corregir problemas",
        description: "Vuelve a los pasos anteriores para corregir los ítems señalados.",
      },
    ],
  },
  "/nova/piloto": {
    title: "Piloto y Calibración",
    description:
      "Acompaña la aplicación piloto de la prueba y calibra los criterios de puntuación con base en los resultados iniciales.",
    steps: [
      {
        title: "Iniciar piloto",
        description: "Envía la prueba a un grupo piloto de candidatos para calibración.",
      },
      {
        title: "Analizar resultados",
        description: "Revisa los resultados del piloto para identificar ajustes necesarios.",
      },
      {
        title: "Calibrar puntuación",
        description: "Ajusta los criterios y pesos de puntuación según sea necesario.",
      },
    ],
  },
  "/nova/mapa": {
    title: "Mapa de Puntuación",
    description:
      "Visualiza cómo se distribuyen los criterios de puntuación en la prueba. Verifica si la cobertura es adecuada.",
    steps: [
      {
        title: "Ver mapa",
        description: "Ve la distribución de puntos por competencia y alternativa.",
      },
      {
        title: "Identificar brechas",
        description: "Verifica si hay competencias sin cobertura adecuada de puntuación.",
      },
      {
        title: "Ajustar distribución",
        description:
          "Vuelve al editor de diálogo para balancear la distribución de puntos si es necesario.",
      },
    ],
  },
  "/nova/governanca": {
    title: "Gobernanza y Publicación",
    description:
      "Gestiona versiones de la prueba, controla el historial de cambios y publica la versión final.",
    steps: [
      {
        title: "Revisar historial",
        description: "Consulta todos los cambios realizados en la prueba a lo largo del tiempo.",
      },
      {
        title: "Aprobar versión",
        description: "Marca la versión como aprobada para publicación tras la revisión.",
      },
      {
        title: "Publicar prueba",
        description: "Publica la prueba para que pueda ser enviada a los candidatos.",
      },
    ],
  },
  "/nova/gupy": {
    title: "Integración Gupy",
    description:
      "Configura y activa la integración con la plataforma Gupy para envío automático de resultados.",
    steps: [
      {
        title: "Verificar conexión",
        description: "Comprueba que la integración con Gupy esté configurada y activa.",
      },
      {
        title: "Mapear campos",
        description: "Asocia los campos de Praxis con los campos correspondientes en Gupy.",
      },
      {
        title: "Activar envío",
        description: "Activa el envío automático de resultados a la plataforma Gupy.",
      },
    ],
  },
  "/nova/competencias": {
    title: "Catálogo de Competencias",
    description:
      "Gestiona el catálogo de competencias de tu empresa. Agrega, edita o elimina competencias usadas en las pruebas.",
    steps: [
      {
        title: "Ver catálogo",
        description: "Ve todas las competencias registradas y sus descripciones.",
      },
      {
        title: "Agregar competencia",
        description:
          'Haz clic en "Nueva competencia" para registrar una nueva competencia al catálogo.',
      },
      {
        title: "Editar competencia",
        description:
          "Haz clic en una competencia existente para editar su nombre o descripción.",
      },
      {
        title: "Eliminar competencia",
        description: "Elimina competencias que ya no se utilizan en las pruebas.",
      },
    ],
  },
  "/enviar-link": {
    title: "Enviar Enlace al Candidato",
    description:
      "Genera y envía enlaces de invitación para que los candidatos realicen la prueba situacional.",
    steps: [
      {
        title: "Seleccionar prueba",
        description: "Elige cuál prueba publicada será enviada al candidato.",
      },
      {
        title: "Generar enlace",
        description: "Genera un enlace único de acceso para que el candidato realice la prueba.",
      },
      {
        title: "Enviar invitación",
        description: "Copia el enlace o envíalo directamente por correo electrónico al candidato.",
      },
    ],
  },
  "/monitoramento": {
    title: "Monitoreo",
    description:
      "Acompaña en tiempo real las pruebas en curso. Ve quién inició, quién completó y los resultados parciales.",
    steps: [
      {
        title: "Ver estado",
        description:
          "Ve el estado de cada candidato: pendiente, en curso o completado.",
      },
      {
        title: "Acompañar progreso",
        description: "Monitorea el progreso de cada candidato durante la aplicación de la prueba.",
      },
      {
        title: "Acceder a resultados",
        description: "Haz clic en un candidato completado para ver el resultado detallado.",
      },
    ],
  },
  "/talent-match": {
    title: "Talent Match",
    description:
      "Compara candidatos lado a lado basándose en los resultados de las pruebas. Identifica el mejor perfil para la vacante.",
    steps: [
      {
        title: "Seleccionar candidatos",
        description: "Elige dos o más candidatos para comparación directa.",
      },
      {
        title: "Comparar resultados",
        description: "Visualiza las puntuaciones por competencia en formato comparativo.",
      },
      {
        title: "Analizar gráficos",
        description:
          "Usa los gráficos para identificar puntos fuertes y débiles de cada candidato.",
      },
    ],
  },
  "/governanca": {
    title: "Gobernanza y Auditoría",
    description:
      "Central de gobernanza con historial completo de versiones, pista de auditoría y control de cambios.",
    steps: [
      {
        title: "Consultar historial",
        description:
          "Ve todas las versiones publicadas y el historial de cambios de cada prueba.",
      },
      {
        title: "Pista de auditoría",
        description: "Accede al registro detallado de quién cambió qué y cuándo.",
      },
      {
        title: "Comparar versiones",
        description: "Compara dos versiones para identificar las diferencias entre ellas.",
      },
    ],
  },
  "/compliance": {
    title: "Cumplimiento",
    description:
      "Documentación de cumplimiento de Praxis. Protección de datos, transparencia del resultado y base técnica de la evaluación.",
    steps: [
      {
        title: "Consultar políticas",
        description: "Accede a las políticas de cumplimiento y protección de datos de la plataforma.",
      },
      {
        title: "Transparencia del resultado",
        description:
          "Entiende cómo se calculan y presentan los resultados al candidato.",
      },
      {
        title: "Base técnica",
        description:
          "Ve la fundamentación técnica que sustenta la metodología de evaluación.",
      },
    ],
  },
  "/defensabilidade": {
    title: "Defensabilidad",
    description:
      "Análisis de confiabilidad y seguridad técnica de los resultados. Demuestra que la puntuación es objetiva y auditable.",
    steps: [
      {
        title: "Seleccionar prueba",
        description: "Elige una prueba para ver su análisis de defensabilidad.",
      },
      {
        title: "Verificar indicadores",
        description:
          "Revisa los indicadores de constructo definido, puntuación auditable y determinística.",
      },
      {
        title: "Exportar informe",
        description: "Genera un informe de defensabilidad para documentación externa.",
      },
    ],
  },
  "/lgpd": {
    title: "Protección de Datos y Transparencia",
    description:
      "Información sobre protección de datos personales y transparencia en la presentación de resultados.",
    steps: [
      {
        title: "Consultar directrices",
        description: "Ve cómo Praxis trata y protege los datos personales de los candidatos.",
      },
      {
        title: "Transparencia",
        description: "Entiende cómo se presentan los resultados de forma transparente.",
      },
      {
        title: "Derechos del candidato",
        description: "Conoce los derechos del candidato en relación con sus datos.",
      },
    ],
  },
  "/configuracoes": {
    title: "Configuración",
    description:
      "Gestiona el perfil de la empresa, configuraciones de integración y preferencias de la plataforma.",
    steps: [
      {
        title: "Perfil de la empresa",
        description: "Actualiza el nombre, logo e información de tu empresa.",
      },
      {
        title: "Integraciones",
        description: "Configura integraciones con plataformas externas como Gupy.",
      },
      {
        title: "Preferencias",
        description: "Ajusta idioma, notificaciones y otras preferencias de la plataforma.",
      },
    ],
  },
  "/candidato": {
    title: "Área del Candidato",
    description:
      "Vista de la experiencia del candidato durante la prueba situacional.",
    steps: [
      {
        title: "Vista del candidato",
        description:
          "Ve exactamente cómo el candidato visualiza e interactúa con la prueba.",
      },
      {
        title: "Flujo de la prueba",
        description: "Sigue el flujo completo que el candidato recorre al responder.",
      },
    ],
  },
};

const helpByLanguage: Record<string, HelpMap> = {
  "pt-BR": ptBr,
  en: enUs,
  "es-MX": esMx,
};

function resolveRoute(pathname: string): string {
  if (pathname.startsWith("/candidato/")) return "/candidato";
  return pathname;
}

export function getHelpContent(
  pathname: string,
  language: Language,
): HelpContent | null {
  const map = helpByLanguage[language] ?? ptBr;
  const route = resolveRoute(pathname);
  return map[route] ?? null;
}

export function hasHelpContent(pathname: string): boolean {
  const route = resolveRoute(pathname);
  return route in ptBr;
}
