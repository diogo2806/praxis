import type { Language } from "./index";

export type CandidateAccessTranslation = {
  loadingLabel: string;
  loadingTitle: string;
  loadingDescription: string;
  beforeStart: string;
  startTitle: string;
  startDescription: string;
  instructionsTitle: string;
  instructions: string[];
  consent: string;
  privacyLink: string;
  privacySummaryTitle: string;
  privacySummaryDescription: string;
  rightsTitle: string;
  rightsDescription: string;
  requestTypeLabel: string;
  requestTypes: {
    confirmationAccess: string;
    rectification: string;
    anonymizationDeletion: string;
    portability: string;
    deletionConsent: string;
    informationSharing: string;
    consentRevocation: string;
  };
  contactLabel: string;
  contactPlaceholder: string;
  detailsLabel: string;
  detailsPlaceholder: string;
  sendRequest: string;
  sendingRequest: string;
  requestSuccessTitle: string;
  requestSuccessDescription: string;
  requestErrorTitle: string;
  requestValidationError: string;
  startButton: string;
  completed: { label: string; title: string; description: string };
  expired: { label: string; title: string; description: string };
  abandoned: { label: string; title: string; description: string };
  closed: { label: string; title: string; description: string };
  privacyNotice: string;
};

export const candidateAccessTranslations: Record<Language, CandidateAccessTranslation> = {
  "pt-BR": {
    loadingLabel: "Preparando",
    loadingTitle: "Abrindo sua avaliação.",
    loadingDescription: "Aguarde só um instante.",
    beforeStart: "Antes de começar",
    startTitle: "Leia as orientações antes de iniciar sua avaliação.",
    startDescription: "A avaliação apresenta cenários e alternativas de resposta. Depois que você iniciar, leia cada situação com atenção, escolha a alternativa que melhor representa sua decisão e confirme a resposta final.",
    instructionsTitle: "Instruções importantes",
    instructions: [
      "Algumas etapas podem ter tempo limite. O contador começa após iniciar.",
      "Você pode trocar a alternativa antes de confirmar, mas a resposta confirmada é final.",
      "A pontuação segue critérios definidos previamente pela empresa responsável.",
      "A avaliação apoia a decisão humana; ela não decide sozinha sobre você.",
    ],
    consent: "Declaro que li as orientações e o aviso de privacidade. Estou ciente de que a empresa responsável tratará meus dados e respostas para executar esta avaliação, registrar o percurso, calcular resultados conforme critérios previamente definidos e cumprir obrigações legais.",
    privacyLink: "Consultar aviso de privacidade",
    privacySummaryTitle: "Privacidade e uso dos dados",
    privacySummaryDescription: "A empresa responsável pelo processo seletivo atua como controladora dos dados. O Práxis atua como operador, seguindo as instruções da empresa. O cálculo é determinístico, não usa IA generativa para julgar respostas e a decisão final deve permanecer humana.",
    rightsTitle: "Exercer um direito sobre meus dados",
    rightsDescription: "Registre uma solicitação vinculada a esta participação. O pedido ficará na trilha de auditoria para atendimento pela empresa responsável.",
    requestTypeLabel: "Direito solicitado",
    requestTypes: {
      confirmationAccess: "Confirmar tratamento e acessar meus dados",
      rectification: "Corrigir dados incompletos ou incorretos",
      anonymizationDeletion: "Anonimizar, bloquear ou eliminar dados excessivos",
      portability: "Solicitar portabilidade, quando aplicável",
      deletionConsent: "Eliminar dados tratados com base em consentimento",
      informationSharing: "Informar com quem meus dados foram compartilhados",
      consentRevocation: "Revogar um consentimento concedido",
    },
    contactLabel: "E-mail para retorno (opcional)",
    contactPlaceholder: "voce@exemplo.com",
    detailsLabel: "Detalhes do pedido (opcional)",
    detailsPlaceholder: "Descreva o que precisa ser analisado.",
    sendRequest: "Registrar solicitação",
    sendingRequest: "Registrando...",
    requestSuccessTitle: "Solicitação registrada",
    requestSuccessDescription: "O pedido foi vinculado à sua participação e encaminhado para tratamento pela empresa responsável.",
    requestErrorTitle: "Não foi possível registrar",
    requestValidationError: "Revise o e-mail informado e tente novamente.",
    startButton: "Iniciar avaliação",
    completed: { label: "Participação concluída", title: "Obrigado por concluir a avaliação.", description: "Suas respostas foram registradas e o resultado será processado pela equipe responsável." },
    expired: { label: "Participação expirada", title: "O prazo desta avaliação terminou.", description: "Não registramos uma conclusão completa dentro do prazo disponível. Entre em contato com a empresa responsável se precisar de um novo acesso ou tiver dúvidas." },
    abandoned: { label: "Participação abandonada", title: "Esta participação foi encerrada antes da conclusão.", description: "A avaliação não consta como concluída. Entre em contato com a empresa responsável se precisar retomar ou receber novo convite." },
    closed: { label: "Participação encerrada", title: "Esta participação não possui próximas etapas.", description: "Entre em contato com a empresa responsável se tiver dúvidas sobre o status da avaliação." },
    privacyNotice: "Seus dados são tratados conforme a LGPD e as instruções da empresa responsável. Você pode consultar o aviso de privacidade e exercer seus direitos nesta página.",
  },
  en: {
    loadingLabel: "Preparing",
    loadingTitle: "Opening your assessment.",
    loadingDescription: "Please wait a moment.",
    beforeStart: "Before you begin",
    startTitle: "Read the instructions before starting your assessment.",
    startDescription: "The assessment presents scenarios and response alternatives. Once you start, read each situation carefully, choose the alternative that best represents your decision, and confirm your final answer.",
    instructionsTitle: "Important instructions",
    instructions: [
      "Some steps may have a time limit. The timer starts after you begin.",
      "You may change an alternative before confirming it, but a confirmed answer is final.",
      "Scoring follows criteria defined in advance by the responsible company.",
      "The assessment supports human decision-making; it does not make decisions about you on its own.",
    ],
    consent: "I confirm that I have read the instructions and the privacy notice. I understand that the responsible company will process my data and answers to run this assessment, record the journey, calculate results according to predefined criteria, and comply with legal obligations.",
    privacyLink: "Read the privacy notice",
    privacySummaryTitle: "Privacy and data use",
    privacySummaryDescription: "The company responsible for the recruitment process is the data controller. Praxis acts as a processor following that company's instructions. Scoring is deterministic, does not use generative AI to judge answers, and the final decision must remain human.",
    rightsTitle: "Exercise a right over my data",
    rightsDescription: "Register a request linked to this participation. The request will be added to the audit trail for handling by the responsible company.",
    requestTypeLabel: "Requested right",
    requestTypes: {
      confirmationAccess: "Confirm processing and access my data",
      rectification: "Correct incomplete or inaccurate data",
      anonymizationDeletion: "Anonymize, block, or delete excessive data",
      portability: "Request portability, when applicable",
      deletionConsent: "Delete data processed based on consent",
      informationSharing: "Learn who my data was shared with",
      consentRevocation: "Withdraw a previously granted consent",
    },
    contactLabel: "Reply email (optional)",
    contactPlaceholder: "you@example.com",
    detailsLabel: "Request details (optional)",
    detailsPlaceholder: "Describe what should be reviewed.",
    sendRequest: "Register request",
    sendingRequest: "Registering...",
    requestSuccessTitle: "Request registered",
    requestSuccessDescription: "The request was linked to your participation and forwarded to the responsible company for handling.",
    requestErrorTitle: "The request could not be registered",
    requestValidationError: "Review the email address and try again.",
    startButton: "Start assessment",
    completed: { label: "Participation completed", title: "Thank you for completing the assessment.", description: "Your answers have been recorded and the result will be processed by the responsible team." },
    expired: { label: "Participation expired", title: "The deadline for this assessment has ended.", description: "We did not record a complete submission within the available time. Contact the responsible company if you need a new access link or have questions." },
    abandoned: { label: "Participation abandoned", title: "This participation was closed before completion.", description: "The assessment is not recorded as completed. Contact the responsible company if you need to resume it or receive a new invitation." },
    closed: { label: "Participation closed", title: "This participation has no further steps.", description: "Contact the responsible company if you have questions about the assessment status." },
    privacyNotice: "Your data is processed under the applicable privacy rules and the responsible company's instructions. You can read the privacy notice and exercise your rights on this page.",
  },
  "es-MX": {
    loadingLabel: "Preparando",
    loadingTitle: "Abriendo tu evaluación.",
    loadingDescription: "Espera un momento.",
    beforeStart: "Antes de comenzar",
    startTitle: "Lee las indicaciones antes de iniciar tu evaluación.",
    startDescription: "La evaluación presenta escenarios y alternativas de respuesta. Después de iniciar, lee cada situación con atención, elige la alternativa que mejor represente tu decisión y confirma tu respuesta final.",
    instructionsTitle: "Indicaciones importantes",
    instructions: [
      "Algunas etapas pueden tener límite de tiempo. El contador comienza después de iniciar.",
      "Puedes cambiar una alternativa antes de confirmarla, pero una respuesta confirmada es definitiva.",
      "La puntuación sigue criterios definidos previamente por la empresa responsable.",
      "La evaluación apoya la decisión humana; no decide por sí sola sobre ti.",
    ],
    consent: "Declaro que leí las indicaciones y el aviso de privacidad. Entiendo que la empresa responsable tratará mis datos y respuestas para ejecutar esta evaluación, registrar el recorrido, calcular resultados según criterios predefinidos y cumplir obligaciones legales.",
    privacyLink: "Consultar el aviso de privacidad",
    privacySummaryTitle: "Privacidad y uso de datos",
    privacySummaryDescription: "La empresa responsable del proceso de selección actúa como responsable del tratamiento. Práxis actúa como encargado siguiendo sus instrucciones. El cálculo es determinista, no usa IA generativa para juzgar respuestas y la decisión final debe seguir siendo humana.",
    rightsTitle: "Ejercer un derecho sobre mis datos",
    rightsDescription: "Registra una solicitud vinculada a esta participación. La solicitud quedará en la pista de auditoría para que la atienda la empresa responsable.",
    requestTypeLabel: "Derecho solicitado",
    requestTypes: {
      confirmationAccess: "Confirmar el tratamiento y acceder a mis datos",
      rectification: "Corregir datos incompletos o incorrectos",
      anonymizationDeletion: "Anonimizar, bloquear o eliminar datos excesivos",
      portability: "Solicitar portabilidad, cuando corresponda",
      deletionConsent: "Eliminar datos tratados con base en consentimiento",
      informationSharing: "Saber con quién se compartieron mis datos",
      consentRevocation: "Revocar un consentimiento otorgado",
    },
    contactLabel: "Correo para respuesta (opcional)",
    contactPlaceholder: "tu@ejemplo.com",
    detailsLabel: "Detalles de la solicitud (opcional)",
    detailsPlaceholder: "Describe qué debe revisarse.",
    sendRequest: "Registrar solicitud",
    sendingRequest: "Registrando...",
    requestSuccessTitle: "Solicitud registrada",
    requestSuccessDescription: "La solicitud se vinculó a tu participación y se envió a la empresa responsable para su atención.",
    requestErrorTitle: "No fue posible registrar la solicitud",
    requestValidationError: "Revisa el correo informado e inténtalo de nuevo.",
    startButton: "Iniciar evaluación",
    completed: { label: "Participación concluida", title: "Gracias por completar la evaluación.", description: "Tus respuestas fueron registradas y el resultado será procesado por el equipo responsable." },
    expired: { label: "Participación vencida", title: "El plazo de esta evaluación terminó.", description: "No registramos una finalización completa dentro del plazo disponible. Comunícate con la empresa responsable si necesitas un nuevo acceso o tienes dudas." },
    abandoned: { label: "Participación abandonada", title: "Esta participación se cerró antes de finalizar.", description: "La evaluación no consta como concluida. Comunícate con la empresa responsable si necesitas retomarla o recibir una nueva invitación." },
    closed: { label: "Participación cerrada", title: "Esta participación no tiene más etapas.", description: "Comunícate con la empresa responsable si tienes dudas sobre el estado de la evaluación." },
    privacyNotice: "Tus datos se tratan conforme a las reglas de privacidad aplicables y las instrucciones de la empresa responsable. Puedes consultar el aviso de privacidad y ejercer tus derechos en esta página.",
  },
};
