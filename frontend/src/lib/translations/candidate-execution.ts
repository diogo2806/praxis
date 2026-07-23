import type { Language } from "./index";

export type CandidateExecutionTranslation = {
  entry: {
    title: string;
    description: string;
    placeholder: string;
    openAssessment: string;
    backToDashboard: string;
  };
  errors: {
    submitBadRequest: string;
    submitConflict: string;
    submitFallback: string;
    loadBadRequest: string;
    loadNotFound: string;
    loadConflict: string;
    loadFallback: string;
  };
  accessibility: {
    highContrast: string;
    highContrastTitle: string;
    largerText: string;
    normalText: string;
    largerTextTitle: string;
    dyslexiaFont: string;
    dyslexiaFontTitle: string;
  };
  media: {
    unsupportedAudio: string;
    scenarioAudio: string;
    scenarioAudioDescription: string;
    optionAudio: (option: string | number) => string;
    optionAudioDescription: (option: string | number) => string;
  };
  loading: {
    label: string;
    title: string;
  };
  accessError: {
    label: string;
    title: string;
  };
  scenario: {
    ariaLabel: string;
    progress: (current: number, total: number) => string;
    participant: string;
    assessment: string;
    question: string;
    confirmHint: string;
    confirming: string;
    confirm: string;
    note: string;
  };
  completion: {
    label: string;
    title: string;
    redirecting: string;
    processing: string;
    explanation: string;
  };
  review: {
    request: string;
    placeholder: string;
    sending: string;
    send: string;
    success: string;
    error: string;
  };
  healthConsent: {
    beforeStart: string;
    title: string;
    educationalPrefix: string;
    educationalStrong: string;
    educationalSuffix: string;
    purposePrefix: string;
    purposeStrong: string;
    purposeSuffix: string;
    bullets: string[];
    consent: string;
    guardian: string;
    registering: string;
    continue: string;
    error: string;
  };
  footer: {
    health: string;
    standard: string;
  };
};

export const candidateExecutionTranslations: Record<Language, CandidateExecutionTranslation> = {
  "pt-BR": {
    entry: {
      title: "Código de acesso obrigatório",
      description:
        "Para abrir a avaliação, use o código de acesso enviado pelo convite. Cole aqui ou abra o link do e-mail.",
      placeholder: "Código de acesso",
      openAssessment: "Abrir avaliação",
      backToDashboard: "Voltar ao painel",
    },
    errors: {
      submitBadRequest: "Não consegui registrar agora. Toque na resposta de novo, por favor.",
      submitConflict: "O tempo desta etapa terminou. Atualize a página para conferir a etapa atual.",
      submitFallback: "Tivemos um problema ao salvar. Tente novamente.",
      loadBadRequest: "Não consegui abrir esta avaliação. Verifique o link recebido e tente novamente.",
      loadNotFound: "Não encontrei esta avaliação. Verifique o link recebido e tente novamente.",
      loadConflict: "Esta avaliação não está mais disponível.",
      loadFallback: "Verifique o link recebido e tente novamente.",
    },
    accessibility: {
      highContrast: "Alto contraste",
      highContrastTitle: "Alterna para alto contraste",
      largerText: "Texto maior",
      normalText: "Texto normal",
      largerTextTitle: "Aumenta o tamanho do texto",
      dyslexiaFont: "Fonte para dislexia",
      dyslexiaFontTitle: "Troca para uma fonte mais fácil de ler",
    },
    media: {
      unsupportedAudio: "Seu navegador não suporta áudio.",
      scenarioAudio: "Áudio do cenário",
      scenarioAudioDescription: "Audiodescrição do cenário",
      optionAudio: (option) => `Áudio da alternativa ${option}`,
      optionAudioDescription: (option) => `Audiodescrição da alternativa ${option}`,
    },
    loading: {
      label: "Carregando",
      title: "Preparando sua avaliação.",
    },
    accessError: {
      label: "Acesso indisponível",
      title: "Não foi possível carregar a avaliação.",
    },
    scenario: {
      ariaLabel: "Cenário da avaliação",
      progress: (current, total) => `Cenário ${current}/${total}`,
      participant: "Participante",
      assessment: "avaliação",
      question: "Como você agiria?",
      confirmHint: "Confirme para enviar sua resposta final.",
      confirming: "Registrando...",
      confirm: "Confirmar resposta final",
      note:
        "Escolha uma alternativa. Você pode trocar antes de confirmar; depois de confirmar, a resposta é final.",
    },
    completion: {
      label: "Participação finalizada",
      title: "Obrigado por participar.",
      redirecting: "Avaliação concluída. Redirecionando você de volta para a Gupy...",
      processing: "O resultado será processado e entregue para a equipe responsável.",
      explanation:
        "Esta avaliação mede como você age em uma situação de trabalho, por competência. Ela é apoio à decisão: quem decide sobre a sua candidatura é uma pessoa, não um sistema automático. Você pode pedir que uma pessoa revise o resultado.",
    },
    review: {
      request: "Solicitar revisão humana",
      placeholder: "Se quiser, conte por que (opcional).",
      sending: "Enviando...",
      send: "Enviar pedido",
      success: "Pedido de revisão registrado. Uma pessoa da equipe responsável vai analisar.",
      error: "Não consegui registrar agora. Tente novamente em instantes.",
    },
    healthConsent: {
      beforeStart: "Antes de começar",
      title: "Uso dos seus dados nesta atividade",
      educationalPrefix: "Esta atividade é um ",
      educationalStrong: "exercício educativo de tomada de decisão",
      educationalSuffix: ". Ela apresenta situações do dia a dia para você praticar escolhas. ",
      purposePrefix:
        "Para realizar a atividade, a empresa responsável vai tratar respostas suas que podem revelar informações relacionadas à sua saúde ou aos seus hábitos. Esses dados serão usados ",
      purposeStrong: "somente",
      purposeSuffix:
        " para gerar o resultado educativo desta atividade e para as finalidades descritas na política de privacidade da empresa responsável.",
      bullets: [
        "A pontuação segue critérios definidos antes da atividade. Não há IA julgando você.",
        "Seus dados não serão usados para decidir, sozinhos e de forma automatizada, sobre tratamento, atendimento ou acesso a serviços.",
        "Você pode pedir que uma pessoa revise o resultado.",
        "Você pode acessar, corrigir ou excluir seus dados e revogar este consentimento a qualquer momento, pelo canal indicado pela empresa responsável. A revogação não afeta atividades já realizadas.",
      ],
      consent:
        "Li e concordo que a empresa responsável trate os dados sensíveis de saúde informados por mim nesta atividade, para as finalidades educativas descritas acima.",
      guardian: "Estou concordando como responsável legal pela pessoa sob minha responsabilidade.",
      registering: "Registrando consentimento...",
      continue: "Concordar e continuar",
      error: "Não consegui registrar o consentimento agora. Tente novamente em instantes.",
    },
    footer: {
      health:
        "Esta é uma atividade educativa. Seus dados são tratados para esta finalidade, conforme a LGPD e a política de privacidade da empresa responsável. Não é diagnóstico nem substitui avaliação profissional. A decisão é de uma pessoa, não de um sistema automático, e você pode pedir revisão humana.",
      standard:
        "Seus dados são tratados para fins desta avaliação, conforme a LGPD e a política de privacidade da empresa responsável. A pontuação segue critérios definidos antes da avaliação, sem IA julgando você. A decisão sobre a sua candidatura é tomada por uma pessoa, não por um sistema automático, e você pode solicitar revisão humana do resultado.",
    },
  },
  en: {
    entry: {
      title: "Access code required",
      description:
        "To open the assessment, use the access code sent with your invitation. Paste it here or open the link from the email.",
      placeholder: "Access code",
      openAssessment: "Open assessment",
      backToDashboard: "Back to dashboard",
    },
    errors: {
      submitBadRequest: "I could not record your answer. Select it again and retry.",
      submitConflict: "The time for this step has ended. Refresh the page to check the current step.",
      submitFallback: "There was a problem saving your answer. Try again.",
      loadBadRequest: "I could not open this assessment. Check the link you received and try again.",
      loadNotFound: "I could not find this assessment. Check the link you received and try again.",
      loadConflict: "This assessment is no longer available.",
      loadFallback: "Check the link you received and try again.",
    },
    accessibility: {
      highContrast: "High contrast",
      highContrastTitle: "Toggle high contrast",
      largerText: "Larger text",
      normalText: "Normal text",
      largerTextTitle: "Increase text size",
      dyslexiaFont: "Dyslexia-friendly font",
      dyslexiaFontTitle: "Switch to an easier-to-read font",
    },
    media: {
      unsupportedAudio: "Your browser does not support audio.",
      scenarioAudio: "Scenario audio",
      scenarioAudioDescription: "Scenario audio description",
      optionAudio: (option) => `Option ${option} audio`,
      optionAudioDescription: (option) => `Option ${option} audio description`,
    },
    loading: {
      label: "Loading",
      title: "Preparing your assessment.",
    },
    accessError: {
      label: "Access unavailable",
      title: "The assessment could not be loaded.",
    },
    scenario: {
      ariaLabel: "Assessment scenario",
      progress: (current, total) => `Scenario ${current}/${total}`,
      participant: "Participant",
      assessment: "assessment",
      question: "What would you do?",
      confirmHint: "Confirm to submit your final answer.",
      confirming: "Saving...",
      confirm: "Confirm final answer",
      note:
        "Choose an option. You may change it before confirming; after confirmation, the answer is final.",
    },
    completion: {
      label: "Participation completed",
      title: "Thank you for participating.",
      redirecting: "Assessment completed. Redirecting you back to Gupy...",
      processing: "The result will be processed and delivered to the responsible team.",
      explanation:
        "This assessment measures how you act in a work situation by competency. It supports decision-making: a person, not an automated system, decides about your application. You may request a human review of the result.",
    },
    review: {
      request: "Request human review",
      placeholder: "Optionally, explain what should be reviewed.",
      sending: "Sending...",
      send: "Send request",
      success: "The review request was recorded. A person from the responsible team will analyze it.",
      error: "The request could not be recorded. Try again shortly.",
    },
    healthConsent: {
      beforeStart: "Before you begin",
      title: "Use of your data in this activity",
      educationalPrefix: "This activity is an ",
      educationalStrong: "educational decision-making exercise",
      educationalSuffix: ". It presents everyday situations so you can practice making choices. ",
      purposePrefix:
        "To perform the activity, the responsible company will process answers that may reveal information related to your health or habits. This data will be used ",
      purposeStrong: "only",
      purposeSuffix:
        " to generate the educational result of this activity and for the purposes described in the responsible company's privacy policy.",
      bullets: [
        "Scoring follows criteria defined before the activity. No AI judges you.",
        "Your data will not be used on its own and automatically to decide about treatment, care, or access to services.",
        "You may ask a person to review the result.",
        "You may access, correct, or delete your data and withdraw this consent at any time through the channel provided by the responsible company. Withdrawal does not affect activities already completed.",
      ],
      consent:
        "I have read and agree that the responsible company may process sensitive health data I provide in this activity for the educational purposes described above.",
      guardian: "I am agreeing as the legal guardian of the person under my responsibility.",
      registering: "Recording consent...",
      continue: "Agree and continue",
      error: "The consent could not be recorded. Try again shortly.",
    },
    footer: {
      health:
        "This is an educational activity. Your data is processed for this purpose under the responsible company's privacy policy and applicable data protection rules. It is not a diagnosis and does not replace professional evaluation. A person, not an automated system, makes the decision, and you may request human review.",
      standard:
        "Your data is processed for this assessment under the responsible company's privacy policy and applicable data protection rules. Scoring follows criteria defined before the assessment, without AI judging you. A person, not an automated system, decides about your application, and you may request human review of the result.",
    },
  },
  "es-MX": {
    entry: {
      title: "Código de acceso obligatorio",
      description:
        "Para abrir la evaluación, usa el código de acceso enviado con la invitación. Pégalo aquí o abre el enlace del correo.",
      placeholder: "Código de acceso",
      openAssessment: "Abrir evaluación",
      backToDashboard: "Volver al panel",
    },
    errors: {
      submitBadRequest: "No fue posible registrar tu respuesta. Selecciónala nuevamente e inténtalo otra vez.",
      submitConflict: "El tiempo de esta etapa terminó. Actualiza la página para revisar la etapa actual.",
      submitFallback: "Hubo un problema al guardar tu respuesta. Inténtalo de nuevo.",
      loadBadRequest: "No fue posible abrir esta evaluación. Revisa el enlace recibido e inténtalo de nuevo.",
      loadNotFound: "No se encontró esta evaluación. Revisa el enlace recibido e inténtalo de nuevo.",
      loadConflict: "Esta evaluación ya no está disponible.",
      loadFallback: "Revisa el enlace recibido e inténtalo de nuevo.",
    },
    accessibility: {
      highContrast: "Alto contraste",
      highContrastTitle: "Alternar alto contraste",
      largerText: "Texto más grande",
      normalText: "Texto normal",
      largerTextTitle: "Aumentar el tamaño del texto",
      dyslexiaFont: "Fuente para dislexia",
      dyslexiaFontTitle: "Cambiar a una fuente más fácil de leer",
    },
    media: {
      unsupportedAudio: "Tu navegador no admite audio.",
      scenarioAudio: "Audio del escenario",
      scenarioAudioDescription: "Audiodescripción del escenario",
      optionAudio: (option) => `Audio de la alternativa ${option}`,
      optionAudioDescription: (option) => `Audiodescripción de la alternativa ${option}`,
    },
    loading: {
      label: "Cargando",
      title: "Preparando tu evaluación.",
    },
    accessError: {
      label: "Acceso no disponible",
      title: "No fue posible cargar la evaluación.",
    },
    scenario: {
      ariaLabel: "Escenario de la evaluación",
      progress: (current, total) => `Escenario ${current}/${total}`,
      participant: "Participante",
      assessment: "evaluación",
      question: "¿Qué harías?",
      confirmHint: "Confirma para enviar tu respuesta final.",
      confirming: "Registrando...",
      confirm: "Confirmar respuesta final",
      note:
        "Elige una alternativa. Puedes cambiarla antes de confirmar; después de confirmar, la respuesta es definitiva.",
    },
    completion: {
      label: "Participación finalizada",
      title: "Gracias por participar.",
      redirecting: "Evaluación concluida. Redirigiéndote de vuelta a Gupy...",
      processing: "El resultado será procesado y entregado al equipo responsable.",
      explanation:
        "Esta evaluación mide cómo actúas en una situación de trabajo por competencia. Sirve como apoyo para la decisión: una persona, no un sistema automatizado, decide sobre tu candidatura. Puedes solicitar una revisión humana del resultado.",
    },
    review: {
      request: "Solicitar revisión humana",
      placeholder: "Si quieres, explica qué debe revisarse (opcional).",
      sending: "Enviando...",
      send: "Enviar solicitud",
      success: "La solicitud de revisión fue registrada. Una persona del equipo responsable la analizará.",
      error: "No fue posible registrar la solicitud. Inténtalo de nuevo en unos momentos.",
    },
    healthConsent: {
      beforeStart: "Antes de comenzar",
      title: "Uso de tus datos en esta actividad",
      educationalPrefix: "Esta actividad es un ",
      educationalStrong: "ejercicio educativo de toma de decisiones",
      educationalSuffix: ". Presenta situaciones cotidianas para que practiques tus decisiones. ",
      purposePrefix:
        "Para realizar la actividad, la empresa responsable tratará respuestas que pueden revelar información relacionada con tu salud o tus hábitos. Estos datos se usarán ",
      purposeStrong: "solamente",
      purposeSuffix:
        " para generar el resultado educativo de esta actividad y para las finalidades descritas en la política de privacidad de la empresa responsable.",
      bullets: [
        "La puntuación sigue criterios definidos antes de la actividad. Ninguna IA te juzga.",
        "Tus datos no se usarán por sí solos y de forma automatizada para decidir sobre tratamiento, atención o acceso a servicios.",
        "Puedes pedir que una persona revise el resultado.",
        "Puedes acceder, corregir o eliminar tus datos y revocar este consentimiento en cualquier momento mediante el canal indicado por la empresa responsable. La revocación no afecta las actividades ya realizadas.",
      ],
      consent:
        "Leí y acepto que la empresa responsable trate los datos sensibles de salud que proporcione en esta actividad para las finalidades educativas descritas anteriormente.",
      guardian: "Estoy aceptando como responsable legal de la persona bajo mi responsabilidad.",
      registering: "Registrando consentimiento...",
      continue: "Aceptar y continuar",
      error: "No fue posible registrar el consentimiento. Inténtalo de nuevo en unos momentos.",
    },
    footer: {
      health:
        "Esta es una actividad educativa. Tus datos se tratan para esta finalidad conforme a la política de privacidad de la empresa responsable y las normas aplicables de protección de datos. No es un diagnóstico ni sustituye una evaluación profesional. La decisión la toma una persona, no un sistema automatizado, y puedes solicitar revisión humana.",
      standard:
        "Tus datos se tratan para esta evaluación conforme a la política de privacidad de la empresa responsable y las normas aplicables de protección de datos. La puntuación sigue criterios definidos antes de la evaluación, sin IA que te juzgue. Una persona, no un sistema automatizado, decide sobre tu candidatura, y puedes solicitar revisión humana del resultado.",
    },
  },
};
