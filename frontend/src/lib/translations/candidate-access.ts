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
    consent: "Li as orientações e concordo que a empresa responsável trate meus dados e respostas para realizar esta avaliação, gerar registros do percurso, calcular resultados conforme os critérios definidos e cumprir suas obrigações legais, conforme a LGPD e a política de privacidade aplicável.",
    startButton: "Iniciar avaliação",
    completed: { label: "Participação concluída", title: "Obrigado por concluir a avaliação.", description: "Suas respostas foram registradas e o resultado será processado pela equipe responsável." },
    expired: { label: "Participação expirada", title: "O prazo desta avaliação terminou.", description: "Não registramos uma conclusão completa dentro do prazo disponível. Entre em contato com a empresa responsável se precisar de um novo acesso ou tiver dúvidas." },
    abandoned: { label: "Participação abandonada", title: "Esta participação foi encerrada antes da conclusão.", description: "A avaliação não consta como concluída. Entre em contato com a empresa responsável se precisar retomar ou receber novo convite." },
    closed: { label: "Participação encerrada", title: "Esta participação não possui próximas etapas.", description: "Entre em contato com a empresa responsável se tiver dúvidas sobre o status da avaliação." },
    privacyNotice: "Seus dados são tratados conforme a LGPD e a política de privacidade da empresa responsável. A análise final permanece sob responsabilidade humana.",
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
    consent: "I have read the instructions and agree that the responsible company may process my data and answers to conduct this assessment, generate journey records, calculate results according to the defined criteria, and fulfill its legal obligations under the applicable privacy policy.",
    startButton: "Start assessment",
    completed: { label: "Participation completed", title: "Thank you for completing the assessment.", description: "Your answers have been recorded and the result will be processed by the responsible team." },
    expired: { label: "Participation expired", title: "The deadline for this assessment has ended.", description: "We did not record a complete submission within the available time. Contact the responsible company if you need a new access link or have questions." },
    abandoned: { label: "Participation abandoned", title: "This participation was closed before completion.", description: "The assessment is not recorded as completed. Contact the responsible company if you need to resume it or receive a new invitation." },
    closed: { label: "Participation closed", title: "This participation has no further steps.", description: "Contact the responsible company if you have questions about the assessment status." },
    privacyNotice: "Your data is processed according to the applicable privacy policy of the responsible company. The final analysis remains under human responsibility.",
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
    consent: "Leí las indicaciones y acepto que la empresa responsable trate mis datos y respuestas para realizar esta evaluación, generar registros del recorrido, calcular resultados según los criterios definidos y cumplir sus obligaciones legales, conforme a la política de privacidad aplicable.",
    startButton: "Iniciar evaluación",
    completed: { label: "Participación concluida", title: "Gracias por completar la evaluación.", description: "Tus respuestas fueron registradas y el resultado será procesado por el equipo responsable." },
    expired: { label: "Participación vencida", title: "El plazo de esta evaluación terminó.", description: "No registramos una finalización completa dentro del plazo disponible. Comunícate con la empresa responsable si necesitas un nuevo acceso o tienes dudas." },
    abandoned: { label: "Participación abandonada", title: "Esta participación se cerró antes de finalizar.", description: "La evaluación no consta como concluida. Comunícate con la empresa responsable si necesitas retomarla o recibir una nueva invitación." },
    closed: { label: "Participación cerrada", title: "Esta participación no tiene más etapas.", description: "Comunícate con la empresa responsable si tienes dudas sobre el estado de la evaluación." },
    privacyNotice: "Tus datos se tratan conforme a la política de privacidad aplicable de la empresa responsable. El análisis final sigue bajo responsabilidad humana.",
  },
};
