from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: esperado 1 trecho, encontrados {count}")
    return text.replace(old, new, 1)


def replace_all(text: str, old: str, new: str, minimum: int, label: str) -> str:
    count = text.count(old)
    if count < minimum:
        raise RuntimeError(f"{label}: esperado ao menos {minimum} trecho(s), encontrados {count}")
    return text.replace(old, new)


translations_path = ROOT / "frontend/src/lib/translations/candidate-experience.ts"
translations_path.write_text(
    '''import type { Language } from "./index";

export type CandidateExperienceTranslation = {
  entryTitle: string;
  entryDescription: string;
  accessCodePlaceholder: string;
  openAssessment: string;
  backToDashboard: string;
  unsupportedAudio: string;
  submitError400: string;
  submitError409: string;
  submitErrorFallback: string;
  loadError400: string;
  loadError404: string;
  loadError409: string;
  loadErrorFallback: string;
  highContrastTitle: string;
  highContrast: string;
  largeTextTitle: string;
  normalText: string;
  largerText: string;
  dyslexiaFontTitle: string;
  dyslexiaFont: string;
  loadingLabel: string;
  loadingTitle: string;
  unavailableLabel: string;
  loadFailedTitle: string;
  scenarioProgress: string;
  secondsRemaining: string;
  scenarioAria: string;
  defaultParticipant: string;
  defaultParticipantInitial: string;
  defaultAssessment: string;
  scenarioAudioDescription: string;
  scenarioAudio: string;
  actionQuestion: string;
  optionAudioDescription: string;
  optionAudio: string;
  confirmHint: string;
  recording: string;
  confirmFinal: string;
  selectionNote: string;
  completedLabel: string;
  completedTitle: string;
  redirecting: string;
  resultProcessing: string;
  humanDecisionNotice: string;
  healthFooter: string;
  standardFooter: string;
  reviewSuccess: string;
  requestReview: string;
  reviewReasonPlaceholder: string;
  sendingReview: string;
  sendReview: string;
  reviewError: string;
  healthConsentBeforeStart: string;
  healthConsentTitle: string;
  healthConsentIntroPrefix: string;
  healthConsentIntroEmphasis: string;
  healthConsentIntroMiddle: string;
  healthConsentWarning: string;
  healthConsentDataPrefix: string;
  healthConsentOnly: string;
  healthConsentDataSuffix: string;
  healthConsentBullets: string[];
  healthConsentAgreement: string;
  healthConsentGuardianAgreement: string;
  healthConsentRegistering: string;
  healthConsentContinue: string;
  healthConsentError: string;
};

export const candidateExperienceTranslations: Record<Language, CandidateExperienceTranslation> = {
  "pt-BR": {
    entryTitle: "Código de acesso obrigatório",
    entryDescription:
      "Para abrir a avaliação, use o código de acesso enviado pelo convite. Cole aqui ou abra o link do e-mail.",
    accessCodePlaceholder: "Código de acesso",
    openAssessment: "Abrir avaliação",
    backToDashboard: "Voltar ao painel",
    unsupportedAudio: "Seu navegador não suporta áudio.",
    submitError400: "Não consegui registrar agora. Toque na resposta de novo, por favor.",
    submitError409: "O tempo desta etapa terminou. Atualize a página para conferir a etapa atual.",
    submitErrorFallback: "Tivemos um problema ao salvar. Tente novamente.",
    loadError400: "Não consegui abrir esta avaliação. Verifique o link recebido e tente novamente.",
    loadError404: "Não encontrei esta avaliação. Verifique o link recebido e tente novamente.",
    loadError409: "Esta avaliação não está mais disponível.",
    loadErrorFallback: "Verifique o link recebido e tente novamente.",
    highContrastTitle: "Alterna para alto contraste",
    highContrast: "Alto contraste",
    largeTextTitle: "Aumenta o tamanho do texto",
    normalText: "normal",
    largerText: "maior",
    dyslexiaFontTitle: "Troca para uma fonte mais fácil de ler",
    dyslexiaFont: "Fonte para dislexia",
    loadingLabel: "Carregando",
    loadingTitle: "Preparando sua avaliação.",
    unavailableLabel: "Acesso indisponível",
    loadFailedTitle: "Não foi possível carregar a avaliação.",
    scenarioProgress: "Cenário {current}/{total}",
    secondsRemaining: "{seconds}s",
    scenarioAria: "Cenário da avaliação",
    defaultParticipant: "Participante",
    defaultParticipantInitial: "P",
    defaultAssessment: "avaliação",
    scenarioAudioDescription: "Audiodescrição do cenário",
    scenarioAudio: "Áudio do cenário",
    actionQuestion: "Como você agiria?",
    optionAudioDescription: "Audiodescrição da alternativa {option}",
    optionAudio: "Áudio da alternativa {option}",
    confirmHint: "Confirme para enviar sua resposta final.",
    recording: "Registrando...",
    confirmFinal: "Confirmar resposta final",
    selectionNote:
      "Escolha uma alternativa. Você pode trocar antes de confirmar; depois de confirmar, a resposta é final.",
    completedLabel: "Participação finalizada",
    completedTitle: "Obrigado por participar.",
    redirecting: "Avaliação concluída. Redirecionando você de volta para a Gupy...",
    resultProcessing: "O resultado será processado e entregue para a equipe responsável.",
    humanDecisionNotice:
      "Esta avaliação mede como você age em uma situação de trabalho, por competência. Ela é apoio à decisão: quem decide sobre a sua candidatura é uma pessoa, não um sistema automático. Você pode pedir que uma pessoa revise o resultado.",
    healthFooter:
      "Esta é uma atividade educativa. Seus dados são tratados para esta finalidade, conforme a LGPD e a política de privacidade da empresa responsável. Não é diagnóstico nem substitui avaliação profissional. A decisão é de uma pessoa, não de um sistema automático, e você pode pedir revisão humana.",
    standardFooter:
      "Seus dados são tratados para fins desta avaliação, conforme a LGPD e a política de privacidade da empresa responsável. A pontuação segue critérios definidos antes da avaliação, sem IA julgando você. A decisão sobre a sua candidatura é tomada por uma pessoa, não por um sistema automático, e você pode solicitar revisão humana do resultado.",
    reviewSuccess: "Pedido de revisão registrado. Uma pessoa da equipe responsável vai analisar.",
    requestReview: "Solicitar revisão humana",
    reviewReasonPlaceholder: "Se quiser, conte por que (opcional).",
    sendingReview: "Enviando...",
    sendReview: "Enviar pedido",
    reviewError: "Não consegui registrar agora. Tente novamente em instantes.",
    healthConsentBeforeStart: "Antes de começar",
    healthConsentTitle: "Uso dos seus dados nesta atividade",
    healthConsentIntroPrefix: "Esta atividade é um",
    healthConsentIntroEmphasis: "exercício educativo de tomada de decisão",
    healthConsentIntroMiddle: "Ela apresenta situações do dia a dia para você praticar escolhas.",
    healthConsentWarning:
      "Não é uma consulta, não é diagnóstico e não substitui a orientação de um profissional de saúde.",
    healthConsentDataPrefix:
      "Para realizar a atividade, a empresa responsável vai tratar respostas suas que podem revelar informações relacionadas à sua saúde ou aos seus hábitos. Esses dados serão usados",
    healthConsentOnly: "somente",
    healthConsentDataSuffix:
      "para gerar o resultado educativo desta atividade e para as finalidades descritas na política de privacidade da empresa responsável.",
    healthConsentBullets: [
      "A pontuação segue critérios definidos antes da atividade. Não há IA julgando você.",
      "Seus dados não serão usados para decidir, sozinhos e de forma automatizada, sobre tratamento, atendimento ou acesso a serviços.",
      "Você pode pedir que uma pessoa revise o resultado.",
      "Você pode acessar, corrigir ou excluir seus dados e revogar este consentimento a qualquer momento, pelo canal indicado pela empresa responsável. A revogação não afeta atividades já realizadas.",
    ],
    healthConsentAgreement:
      "Li e concordo que a empresa responsável trate os dados sensíveis de saúde informados por mim nesta atividade, para as finalidades educativas descritas acima.",
    healthConsentGuardianAgreement:
      "Estou concordando como responsável legal pela pessoa sob minha responsabilidade.",
    healthConsentRegistering: "Registrando consentimento...",
    healthConsentContinue: "Concordar e continuar",
    healthConsentError: "Não consegui registrar o consentimento agora. Tente novamente em instantes.",
  },
  en: {
    entryTitle: "Access code required",
    entryDescription:
      "To open the assessment, use the access code sent with the invitation. Paste it here or open the link in the email.",
    accessCodePlaceholder: "Access code",
    openAssessment: "Open assessment",
    backToDashboard: "Back to dashboard",
    unsupportedAudio: "Your browser does not support audio.",
    submitError400: "I could not register your answer right now. Please select it again.",
    submitError409: "The time for this step has ended. Refresh the page to check the current step.",
    submitErrorFallback: "There was a problem saving your answer. Please try again.",
    loadError400: "I could not open this assessment. Check the link you received and try again.",
    loadError404: "I could not find this assessment. Check the link you received and try again.",
    loadError409: "This assessment is no longer available.",
    loadErrorFallback: "Check the link you received and try again.",
    highContrastTitle: "Toggle high contrast",
    highContrast: "High contrast",
    largeTextTitle: "Increase text size",
    normalText: "normal",
    largerText: "larger",
    dyslexiaFontTitle: "Switch to a font that is easier to read",
    dyslexiaFont: "Dyslexia-friendly font",
    loadingLabel: "Loading",
    loadingTitle: "Preparing your assessment.",
    unavailableLabel: "Access unavailable",
    loadFailedTitle: "The assessment could not be loaded.",
    scenarioProgress: "Scenario {current}/{total}",
    secondsRemaining: "{seconds}s",
    scenarioAria: "Assessment scenario",
    defaultParticipant: "Participant",
    defaultParticipantInitial: "P",
    defaultAssessment: "assessment",
    scenarioAudioDescription: "Scenario audio description",
    scenarioAudio: "Scenario audio",
    actionQuestion: "What would you do?",
    optionAudioDescription: "Audio description for option {option}",
    optionAudio: "Audio for option {option}",
    confirmHint: "Confirm to submit your final answer.",
    recording: "Saving...",
    confirmFinal: "Confirm final answer",
    selectionNote:
      "Choose an option. You may change it before confirming; after confirmation, the answer is final.",
    completedLabel: "Participation completed",
    completedTitle: "Thank you for participating.",
    redirecting: "Assessment completed. Redirecting you back to Gupy...",
    resultProcessing: "The result will be processed and delivered to the responsible team.",
    humanDecisionNotice:
      "This assessment measures how you act in a work situation, by competency. It supports a decision: a person, not an automated system, decides about your application. You may request a human review of the result.",
    healthFooter:
      "This is an educational activity. Your data is processed for this purpose under applicable data-protection rules and the responsible company's privacy policy. It is not a diagnosis and does not replace professional assessment. A person, not an automated system, makes the decision, and you may request human review.",
    standardFooter:
      "Your data is processed for this assessment under applicable data-protection rules and the responsible company's privacy policy. Scoring follows criteria defined before the assessment, without AI judging you. A person, not an automated system, decides about your application, and you may request human review of the result.",
    reviewSuccess: "The review request was registered. A person from the responsible team will analyze it.",
    requestReview: "Request human review",
    reviewReasonPlaceholder: "Optionally, explain why you are requesting a review.",
    sendingReview: "Sending...",
    sendReview: "Send request",
    reviewError: "I could not register the request right now. Please try again shortly.",
    healthConsentBeforeStart: "Before you begin",
    healthConsentTitle: "How your data is used in this activity",
    healthConsentIntroPrefix: "This activity is an",
    healthConsentIntroEmphasis: "educational decision-making exercise",
    healthConsentIntroMiddle: "It presents everyday situations so you can practice making choices.",
    healthConsentWarning:
      "It is not a consultation or diagnosis and does not replace guidance from a health professional.",
    healthConsentDataPrefix:
      "To carry out the activity, the responsible company will process answers that may reveal information about your health or habits. This data will be used",
    healthConsentOnly: "only",
    healthConsentDataSuffix:
      "to generate the educational result of this activity and for the purposes described in the responsible company's privacy policy.",
    healthConsentBullets: [
      "Scoring follows criteria defined before the activity. No AI judges you.",
      "Your data will not be used by itself in an automated decision about treatment, care, or access to services.",
      "You may request a human review of the result.",
      "You may access, correct, or delete your data and withdraw this consent at any time through the channel provided by the responsible company. Withdrawal does not affect activities already completed.",
    ],
    healthConsentAgreement:
      "I have read and agree that the responsible company may process the sensitive health data I provide in this activity for the educational purposes described above.",
    healthConsentGuardianAgreement:
      "I am agreeing as the legal guardian of the person under my responsibility.",
    healthConsentRegistering: "Registering consent...",
    healthConsentContinue: "Agree and continue",
    healthConsentError: "I could not register consent right now. Please try again shortly.",
  },
  "es-MX": {
    entryTitle: "Se requiere código de acceso",
    entryDescription:
      "Para abrir la evaluación, usa el código de acceso enviado con la invitación. Pégalo aquí o abre el enlace del correo.",
    accessCodePlaceholder: "Código de acceso",
    openAssessment: "Abrir evaluación",
    backToDashboard: "Volver al panel",
    unsupportedAudio: "Tu navegador no admite audio.",
    submitError400: "No pude registrar tu respuesta en este momento. Selecciónala de nuevo, por favor.",
    submitError409: "El tiempo de esta etapa terminó. Actualiza la página para consultar la etapa actual.",
    submitErrorFallback: "Hubo un problema al guardar tu respuesta. Inténtalo de nuevo.",
    loadError400: "No pude abrir esta evaluación. Revisa el enlace recibido e inténtalo de nuevo.",
    loadError404: "No encontré esta evaluación. Revisa el enlace recibido e inténtalo de nuevo.",
    loadError409: "Esta evaluación ya no está disponible.",
    loadErrorFallback: "Revisa el enlace recibido e inténtalo de nuevo.",
    highContrastTitle: "Alternar alto contraste",
    highContrast: "Alto contraste",
    largeTextTitle: "Aumentar el tamaño del texto",
    normalText: "normal",
    largerText: "más grande",
    dyslexiaFontTitle: "Cambiar a una fuente más fácil de leer",
    dyslexiaFont: "Fuente para dislexia",
    loadingLabel: "Cargando",
    loadingTitle: "Preparando tu evaluación.",
    unavailableLabel: "Acceso no disponible",
    loadFailedTitle: "No fue posible cargar la evaluación.",
    scenarioProgress: "Escenario {current}/{total}",
    secondsRemaining: "{seconds}s",
    scenarioAria: "Escenario de la evaluación",
    defaultParticipant: "Participante",
    defaultParticipantInitial: "P",
    defaultAssessment: "evaluación",
    scenarioAudioDescription: "Audiodescripción del escenario",
    scenarioAudio: "Audio del escenario",
    actionQuestion: "¿Cómo actuarías?",
    optionAudioDescription: "Audiodescripción de la alternativa {option}",
    optionAudio: "Audio de la alternativa {option}",
    confirmHint: "Confirma para enviar tu respuesta final.",
    recording: "Registrando...",
    confirmFinal: "Confirmar respuesta final",
    selectionNote:
      "Elige una alternativa. Puedes cambiarla antes de confirmar; después de confirmar, la respuesta es definitiva.",
    completedLabel: "Participación finalizada",
    completedTitle: "Gracias por participar.",
    redirecting: "Evaluación concluida. Redirigiéndote de vuelta a Gupy...",
    resultProcessing: "El resultado será procesado y entregado al equipo responsable.",
    humanDecisionNotice:
      "Esta evaluación mide cómo actúas en una situación de trabajo, por competencia. Sirve como apoyo para la decisión: una persona, no un sistema automatizado, decide sobre tu candidatura. Puedes solicitar una revisión humana del resultado.",
    healthFooter:
      "Esta es una actividad educativa. Tus datos se tratan con esta finalidad conforme a las normas de protección de datos aplicables y la política de privacidad de la empresa responsable. No es un diagnóstico ni sustituye una evaluación profesional. La decisión la toma una persona, no un sistema automatizado, y puedes solicitar revisión humana.",
    standardFooter:
      "Tus datos se tratan para esta evaluación conforme a las normas de protección de datos aplicables y la política de privacidad de la empresa responsable. La puntuación sigue criterios definidos antes de la evaluación, sin IA que te juzgue. La decisión sobre tu candidatura la toma una persona, no un sistema automatizado, y puedes solicitar una revisión humana del resultado.",
    reviewSuccess: "La solicitud de revisión fue registrada. Una persona del equipo responsable la analizará.",
    requestReview: "Solicitar revisión humana",
    reviewReasonPlaceholder: "Si quieres, explica el motivo de la solicitud (opcional).",
    sendingReview: "Enviando...",
    sendReview: "Enviar solicitud",
    reviewError: "No pude registrar la solicitud en este momento. Inténtalo de nuevo en unos instantes.",
    healthConsentBeforeStart: "Antes de comenzar",
    healthConsentTitle: "Uso de tus datos en esta actividad",
    healthConsentIntroPrefix: "Esta actividad es un",
    healthConsentIntroEmphasis: "ejercicio educativo de toma de decisiones",
    healthConsentIntroMiddle: "Presenta situaciones cotidianas para que practiques tus elecciones.",
    healthConsentWarning:
      "No es una consulta ni un diagnóstico y no sustituye la orientación de un profesional de la salud.",
    healthConsentDataPrefix:
      "Para realizar la actividad, la empresa responsable tratará respuestas que pueden revelar información relacionada con tu salud o tus hábitos. Estos datos se usarán",
    healthConsentOnly: "solamente",
    healthConsentDataSuffix:
      "para generar el resultado educativo de esta actividad y para las finalidades descritas en la política de privacidad de la empresa responsable.",
    healthConsentBullets: [
      "La puntuación sigue criterios definidos antes de la actividad. No hay IA que te juzgue.",
      "Tus datos no se usarán por sí solos en una decisión automatizada sobre tratamiento, atención o acceso a servicios.",
      "Puedes solicitar que una persona revise el resultado.",
      "Puedes acceder, corregir o eliminar tus datos y retirar este consentimiento en cualquier momento mediante el canal indicado por la empresa responsable. La revocación no afecta actividades ya realizadas.",
    ],
    healthConsentAgreement:
      "Leí y acepto que la empresa responsable trate los datos sensibles de salud que informe en esta actividad para las finalidades educativas descritas arriba.",
    healthConsentGuardianAgreement:
      "Estoy aceptando como representante legal de la persona bajo mi responsabilidad.",
    healthConsentRegistering: "Registrando consentimiento...",
    healthConsentContinue: "Aceptar y continuar",
    healthConsentError: "No pude registrar el consentimiento en este momento. Inténtalo de nuevo en unos instantes.",
  },
};
''',
    encoding="utf-8",
)

index_path = ROOT / "frontend/src/lib/translations/index.ts"
index_text = index_path.read_text(encoding="utf-8")
index_text = replace_once(
    index_text,
    'import { documentMetadataTranslations } from "./document-metadata";\n',
    'import { documentMetadataTranslations } from "./document-metadata";\nimport { candidateExperienceTranslations } from "./candidate-experience";\n',
    "import do catálogo da experiência",
)
index_text = replace_once(
    index_text,
    '  documentMetadata: documentMetadataTranslations[language],\n',
    '  documentMetadata: documentMetadataTranslations[language],\n  candidateExperience: candidateExperienceTranslations[language],\n',
    "registro do catálogo da experiência",
)
index_path.write_text(index_text, encoding="utf-8")

route_path = ROOT / "frontend/src/routes/candidato.tsx"
route = route_path.read_text(encoding="utf-8")
route = replace_once(
    route,
    'import { cn } from "@/lib/utils";\n',
    'import { useLanguage } from "@/lib/language-context";\nimport { cn } from "@/lib/utils";\n',
    "import useLanguage",
)
route = replace_once(
    route,
    '''const SUBMIT_ERROR_MESSAGES: Record<number, string> = {
  400: "Não consegui registrar agora. Toque na resposta de novo, por favor.",
  409: "O tempo desta etapa terminou. Atualize a página para conferir a etapa atual.",
};

const LOAD_ERROR_MESSAGES: Record<number, string> = {
  400: "Não consegui abrir esta avaliação. Verifique o link recebido e tente novamente.",
  404: "Não encontrei esta avaliação. Verifique o link recebido e tente novamente.",
  409: "Esta avaliação não está mais disponível.",
};

''',
    "",
    "remoção das mensagens fixas",
)
route = replace_once(
    route,
    '''function CandidateEntryPage() {
  const [token, setToken] = useState("");
''',
    '''function CandidateEntryPage() {
  const { t } = useLanguage();
  const copy = t.candidateExperience;
  const [token, setToken] = useState("");
''',
    "contexto no acesso por código",
)
route = replace_once(route, 'title="Código de acesso obrigatório"', 'title={copy.entryTitle}', "título do acesso")
route = replace_once(
    route,
    'description="Para abrir a avaliação, use o código de acesso enviado pelo convite. Cole aqui ou abra o link do e-mail."',
    'description={copy.entryDescription}',
    "descrição do acesso",
)
route = replace_once(route, 'placeholder="Código de acesso"', 'placeholder={copy.accessCodePlaceholder}', "placeholder do acesso")
route = replace_once(route, '              Abrir avaliação\n', '              {copy.openAssessment}\n', "ação abrir avaliação")
route = replace_once(route, '              Voltar ao painel\n', '              {copy.backToDashboard}\n', "ação voltar ao painel")
route = replace_once(
    route,
    '''  audioLabel,
}: {
  mediaUrl: string;
  mediaType: MediaType | null;
  accessibleDescription?: string | null;
  audioLabel: string;
}) {
''',
    '''  audioLabel,
  unsupportedAudio,
}: {
  mediaUrl: string;
  mediaType: MediaType | null;
  accessibleDescription?: string | null;
  audioLabel: string;
  unsupportedAudio: string;
}) {
''',
    "propriedade de fallback de áudio",
)
route = replace_all(route, '        Seu navegador não suporta áudio.\n', '        {unsupportedAudio}\n', 1, "fallback de áudio do componente")
route = replace_once(
    route,
    '''function formatTimer(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(remainder).padStart(2, "0")}`;
}

''',
    '''function formatTimer(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const remainder = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(remainder).padStart(2, "0")}`;
}

function formatCandidateMessage(
  template: string,
  values: Record<string, string | number>,
): string {
  return Object.entries(values).reduce(
    (message, [key, value]) => message.replace(`{${key}}`, String(value)),
    template,
  );
}

''',
    "helper de interpolação",
)
route = replace_once(
    route,
    '''function FocusedCandidateExperience({ token }: { token: string }) {
  const [liveAttempt, setLiveAttempt] = useState<CandidateAttemptResponse | null>(null);
''',
    '''function FocusedCandidateExperience({ token }: { token: string }) {
  const { t } = useLanguage();
  const copy = t.candidateExperience;
  const submitErrorMessages = { 400: copy.submitError400, 409: copy.submitError409 };
  const loadErrorMessages = {
    400: copy.loadError400,
    404: copy.loadError404,
    409: copy.loadError409,
  };
  const [liveAttempt, setLiveAttempt] = useState<CandidateAttemptResponse | null>(null);
''',
    "contexto da execução",
)
route = replace_all(route, "SUBMIT_ERROR_MESSAGES", "submitErrorMessages", 2, "mapa de erro de envio")
route = replace_all(route, '"Tivemos um problema ao salvar. Tente novamente."', "copy.submitErrorFallback", 2, "fallback de envio")
route = replace_once(route, "    [attempt, attemptQuery, token],", "    [attempt, attemptQuery, copy, token],", "dependência do callback")
route = replace_once(route, 'title="Alterna para alto contraste"', 'title={copy.highContrastTitle}', "título alto contraste")
route = replace_once(route, '          Alto contraste\n', '          {copy.highContrast}\n', "rótulo alto contraste")
route = replace_once(route, 'title="Aumenta o tamanho do texto"', 'title={copy.largeTextTitle}', "título texto maior")
route = replace_once(route, '          Texto {largeText ? "normal" : "maior"}\n', '          {largeText ? copy.normalText : copy.largerText}\n', "rótulo texto maior")
route = replace_once(route, 'title="Troca para uma fonte mais fácil de ler"', 'title={copy.dyslexiaFontTitle}', "título fonte dislexia")
route = replace_once(route, '          Fonte para dislexia\n', '          {copy.dyslexiaFont}\n', "rótulo fonte dislexia")
route = replace_once(route, '<div className="cs-label loading">Carregando</div>', '<div className="cs-label loading">{copy.loadingLabel}</div>', "estado carregando")
route = replace_once(route, '<h1>Preparando sua avaliação.</h1>', '<h1>{copy.loadingTitle}</h1>', "título carregando")
route = replace_once(route, '<div className="cs-label error">Acesso indisponível</div>', '<div className="cs-label error">{copy.unavailableLabel}</div>', "estado indisponível")
route = replace_once(route, '<h1>Não foi possível carregar a avaliação.</h1>', '<h1>{copy.loadFailedTitle}</h1>', "título de falha")
route = replace_once(route, "              LOAD_ERROR_MESSAGES,", "              loadErrorMessages,", "mapa de erro de carga")
route = replace_once(route, '              "Verifique o link recebido e tente novamente.",', "              copy.loadErrorFallback,", "fallback de carga")
route = replace_all(
    route,
    '''                Cenário {currentStep}/{totalSteps}
''',
    '''                {formatCandidateMessage(copy.scenarioProgress, {
                  current: currentStep,
                  total: totalSteps,
                })}
''',
    1,
    "progresso do cenário",
)
route = replace_once(
    route,
    '              <span>{remaining}s</span>',
    '              <span>{formatCandidateMessage(copy.secondsRemaining, { seconds: remaining })}</span>',
    "tempo restante",
)
route = replace_once(route, '<div className="scenario" aria-label="Cenário da avaliação">', '<div className="scenario" aria-label={copy.scenarioAria}>', "aria do cenário")
route = replace_once(route, '(currentNode.pessoa ?? "P").substring(0, 2).toUpperCase()', '(currentNode.pessoa ?? copy.defaultParticipantInitial).substring(0, 2).toUpperCase()', "inicial padrão")
route = replace_once(route, '<div className="who">{currentNode.pessoa ?? "Participante"}</div>', '<div className="who">{currentNode.pessoa ?? copy.defaultParticipant}</div>', "participante padrão")
route = replace_once(
    route,
    '''                    Cenário {currentStep}/{totalSteps} · {attempt?.avaliacaoNome ?? "avaliação"}
''',
    '''                    {formatCandidateMessage(copy.scenarioProgress, {
                      current: currentStep,
                      total: totalSteps,
                    })} · {attempt?.avaliacaoNome ?? copy.defaultAssessment}
''',
    "cabeçalho do cenário",
)
route = replace_once(route, 'aria-label="Audiodescrição do cenário"', 'aria-label={copy.scenarioAudioDescription}', "audiodescrição do cenário")
route = replace_once(route, '                    Seu navegador não suporta áudio.\n', '                    {copy.unsupportedAudio}\n', "fallback de audiodescrição")
route = replace_once(route, '                    audioLabel="Áudio do cenário"\n', '                    audioLabel={copy.scenarioAudio}\n                    unsupportedAudio={copy.unsupportedAudio}\n', "áudio do cenário")
route = replace_once(route, '<div className="sc-opts" role="group" aria-label="Como você agiria?">', '<div className="sc-opts" role="group" aria-label={copy.actionQuestion}>', "pergunta de ação")
route = replace_once(
    route,
    '''                          aria-label={`Audiodescrição da alternativa ${
                            OPTION_LETTERS[idx] ?? idx + 1
                          }`}
''',
    '''                          aria-label={formatCandidateMessage(copy.optionAudioDescription, {
                            option: OPTION_LETTERS[idx] ?? idx + 1,
                          })}
''',
    "audiodescrição da alternativa",
)
route = replace_once(route, '                          Seu navegador não suporta áudio.\n', '                          {copy.unsupportedAudio}\n', "fallback da alternativa")
route = replace_once(
    route,
    '                          audioLabel={`Áudio da alternativa ${OPTION_LETTERS[idx] ?? idx + 1}`}\n',
    '''                          audioLabel={formatCandidateMessage(copy.optionAudio, {
                            option: OPTION_LETTERS[idx] ?? idx + 1,
                          })}
                          unsupportedAudio={copy.unsupportedAudio}
''',
    "áudio da alternativa",
)
route = replace_once(route, '<span className="confirm-hint">Confirme para enviar sua resposta final.</span>', '<span className="confirm-hint">{copy.confirmHint}</span>', "dica de confirmação")
route = replace_once(route, '{submittingAnswer ? "Registrando..." : "Confirmar resposta final"}', '{submittingAnswer ? copy.recording : copy.confirmFinal}', "botão confirmar")
route = replace_once(
    route,
    '''                  Escolha uma alternativa. Você pode trocar antes de confirmar; depois de confirmar,
                  a resposta é final.
''',
    '''                  {copy.selectionNote}
''',
    "instrução da alternativa",
)
route = replace_once(route, '<div className="cs-label done">Participação finalizada</div>', '<div className="cs-label done">{copy.completedLabel}</div>', "estado finalizado")
route = replace_once(route, '<h1>Obrigado por participar.</h1>', '<h1>{copy.completedTitle}</h1>', "título finalizado")
route = replace_once(
    route,
    '''              ? "Avaliação concluída. Redirecionando você de volta para a Gupy..."
              : "O resultado será processado e entregue para a equipe responsável."}
''',
    '''              ? copy.redirecting
              : copy.resultProcessing}
''',
    "mensagem de conclusão",
)
route = replace_once(
    route,
    '''            Esta avaliação mede como você age em uma situação de trabalho, por competência. Ela é
            apoio à decisão: quem decide sobre a sua candidatura é uma pessoa, não um sistema
            automático. Você pode pedir que uma pessoa revise o resultado.
''',
    '''            {copy.humanDecisionNotice}
''',
    "aviso de decisão humana",
)
route = replace_once(
    route,
    '''          ? "Esta é uma atividade educativa. Seus dados são tratados para esta finalidade, conforme a LGPD e a política de privacidade da empresa responsável. Não é diagnóstico nem substitui avaliação profissional. A decisão é de uma pessoa, não de um sistema automático, e você pode pedir revisão humana."
          : "Seus dados são tratados para fins desta avaliação, conforme a LGPD e a política de privacidade da empresa responsável. A pontuação segue critérios definidos antes da avaliação, sem IA julgando você. A decisão sobre a sua candidatura é tomada por uma pessoa, não por um sistema automático, e você pode solicitar revisão humana do resultado."}
''',
    '''          ? copy.healthFooter
          : copy.standardFooter}
''',
    "rodapé de privacidade",
)
route = replace_once(
    route,
    '''function HumanReviewRequest({ attemptId }: { attemptId: string }) {
  const [open, setOpen] = useState(false);
''',
    '''function HumanReviewRequest({ attemptId }: { attemptId: string }) {
  const { t } = useLanguage();
  const copy = t.candidateExperience;
  const [open, setOpen] = useState(false);
''',
    "contexto da revisão",
)
route = replace_once(
    route,
    '''          Pedido de revisão registrado. Uma pessoa da equipe responsável vai analisar.
''',
    '''          {copy.reviewSuccess}
''',
    "sucesso da revisão",
)
route = replace_once(route, '          Solicitar revisão humana\n', '          {copy.requestReview}\n', "ação de revisão")
route = replace_once(route, '            placeholder="Se quiser, conte por que (opcional)."', '            placeholder={copy.reviewReasonPlaceholder}', "placeholder de revisão")
route = replace_once(route, '{mutation.isPending ? "Enviando..." : "Enviar pedido"}', '{mutation.isPending ? copy.sendingReview : copy.sendReview}', "envio de revisão")
route = replace_once(
    route,
    '''              Não consegui registrar agora. Tente novamente em instantes.
''',
    '''              {copy.reviewError}
''',
    "erro de revisão",
)
route = replace_once(
    route,
    '''}) {
  const [agreed, setAgreed] = useState(false);
  const [onBehalfOfMinor, setOnBehalfOfMinor] = useState(false);
  const mutation = useMutation({
    mutationFn: () => recordHealthConsent(token, onBehalfOfMinor, noticeVersion),
    onSuccess: onConsented,
  });

  return (
    <div className="cand-consent">
      <div className="cc-label">Antes de começar</div>
      <h1>Uso dos seus dados nesta atividade</h1>
      <div className="cc-body">
        <p>
          Esta atividade é um <strong>exercício educativo de tomada de decisão</strong>. Ela
          apresenta situações do dia a dia para você praticar escolhas. {" "}
          <strong>
            Não é uma consulta, não é diagnóstico e não substitui a orientação de um profissional de
            saúde.
          </strong>
        </p>
        <p>
          Para realizar a atividade, a empresa responsável vai tratar respostas suas que podem
          revelar informações relacionadas à sua saúde ou aos seus hábitos. Esses dados serão usados {" "}
          <strong>somente</strong> para gerar o resultado educativo desta atividade e para as
          finalidades descritas na política de privacidade da empresa responsável.
        </p>
        <ul>
          <li>A pontuação segue critérios definidos antes da atividade. Não há IA julgando você.</li>
          <li>
            Seus dados não serão usados para decidir, sozinhos e de forma automatizada, sobre
            tratamento, atendimento ou acesso a serviços.
          </li>
          <li>Você pode pedir que uma pessoa revise o resultado.</li>
          <li>
            Você pode acessar, corrigir ou excluir seus dados e revogar este consentimento a qualquer
            momento, pelo canal indicado pela empresa responsável. A revogação não afeta atividades
            já realizadas.
          </li>
        </ul>
      </div>
''',
    '''}) {
  const { t } = useLanguage();
  const copy = t.candidateExperience;
  const [agreed, setAgreed] = useState(false);
  const [onBehalfOfMinor, setOnBehalfOfMinor] = useState(false);
  const mutation = useMutation({
    mutationFn: () => recordHealthConsent(token, onBehalfOfMinor, noticeVersion),
    onSuccess: onConsented,
  });

  return (
    <div className="cand-consent">
      <div className="cc-label">{copy.healthConsentBeforeStart}</div>
      <h1>{copy.healthConsentTitle}</h1>
      <div className="cc-body">
        <p>
          {copy.healthConsentIntroPrefix} {" "}
          <strong>{copy.healthConsentIntroEmphasis}</strong>. {" "}
          {copy.healthConsentIntroMiddle} {" "}
          <strong>{copy.healthConsentWarning}</strong>
        </p>
        <p>
          {copy.healthConsentDataPrefix} {" "}
          <strong>{copy.healthConsentOnly}</strong> {copy.healthConsentDataSuffix}
        </p>
        <ul>
          {copy.healthConsentBullets.map((item) => (
            <li key={item}>{item}</li>
          ))}
        </ul>
      </div>
''',
    "corpo do consentimento de saúde",
)
route = replace_once(
    route,
    '''            Li e concordo que a empresa responsável trate os dados sensíveis de saúde informados por
            mim nesta atividade, para as finalidades educativas descritas acima.
''',
    '''            {copy.healthConsentAgreement}
''',
    "aceite de saúde",
)
route = replace_once(
    route,
    '          <span>Estou concordando como responsável legal pela pessoa sob minha responsabilidade.</span>',
    '          <span>{copy.healthConsentGuardianAgreement}</span>',
    "aceite do responsável",
)
route = replace_once(
    route,
    '{mutation.isPending ? "Registrando consentimento..." : "Concordar e continuar"}',
    '{mutation.isPending ? copy.healthConsentRegistering : copy.healthConsentContinue}',
    "botão do consentimento",
)
route = replace_once(
    route,
    '''            Não consegui registrar o consentimento agora. Tente novamente em instantes.
''',
    '''            {copy.healthConsentError}
''',
    "erro do consentimento",
)

for forbidden in [
    "Alto contraste",
    "Preparando sua avaliação.",
    "Cenário da avaliação",
    "Confirmar resposta final",
    "Participação finalizada",
    "Solicitar revisão humana",
    "Uso dos seus dados nesta atividade",
]:
    if f'"{forbidden}"' in route:
        raise RuntimeError(f"texto operacional ainda fixo no componente: {forbidden}")

if "{currentNode.descricao}" not in route or "{option.texto}" not in route:
    raise RuntimeError("o conteúdo cadastrado do cenário deve continuar sendo renderizado sem tradução automática")

route_path.write_text(route, encoding="utf-8")

test_path = ROOT / "frontend/scripts/test-candidate-experience-i18n.mjs"
test_path.write_text(
    '''import assert from "node:assert/strict";
import { readFileSync } from "node:fs";

const route = readFileSync(new URL("../src/routes/candidato.tsx", import.meta.url), "utf8");
const catalog = readFileSync(
  new URL("../src/lib/translations/candidate-experience.ts", import.meta.url),
  "utf8",
);
const index = readFileSync(new URL("../src/lib/translations/index.ts", import.meta.url), "utf8");

for (const localeMarker of ['"pt-BR": {', "  en: {", '"es-MX": {']) {
  assert.ok(catalog.includes(localeMarker), `catálogo ausente para ${localeMarker}`);
}

for (const key of [
  "loadingTitle",
  "submitError409",
  "highContrast",
  "scenarioProgress",
  "confirmFinal",
  "completedTitle",
  "reviewSuccess",
  "healthConsentTitle",
  "healthConsentBullets",
]) {
  assert.equal((catalog.match(new RegExp(`\\\\b${key}:`, "g")) ?? []).length, 3, `${key} deve existir nos três idiomas`);
}

assert.ok(index.includes("candidateExperience: candidateExperienceTranslations[language]"));
assert.ok(route.includes('import { useLanguage } from "@/lib/language-context"'));
assert.ok(route.includes("const copy = t.candidateExperience"));
assert.ok(route.includes("copy.submitError409"));
assert.ok(route.includes("copy.healthConsentTitle"));
assert.ok(route.includes("{currentNode.descricao}"));
assert.ok(route.includes("{option.texto}"));

for (const fixedText of [
  'title="Alterna para alto contraste"',
  ">Alto contraste<",
  ">Preparando sua avaliação.<",
  'aria-label="Cenário da avaliação"',
  ">Confirmar resposta final<",
  ">Participação finalizada<",
  ">Solicitar revisão humana<",
  ">Uso dos seus dados nesta atividade<",
]) {
  assert.ok(!route.includes(fixedText), `texto fixo encontrado: ${fixedText}`);
}

console.log("Candidate experience i18n contract: OK");
''',
    encoding="utf-8",
)

package_path = ROOT / "frontend/package.json"
package_data = json.loads(package_path.read_text(encoding="utf-8"))
package_data["scripts"]["test:candidate-i18n"] = "node scripts/test-candidate-experience-i18n.mjs"
package_path.write_text(json.dumps(package_data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

ci_path = ROOT / ".github/workflows/ci.yml"
ci = ci_path.read_text(encoding="utf-8")
ci = replace_once(
    ci,
    '''      - name: Test health consent contract
        shell: bash
        run: |
          set -o pipefail
          npm run test:health-consent 2>&1 | tee -a frontend.log

''',
    '''      - name: Test health consent contract
        shell: bash
        run: |
          set -o pipefail
          npm run test:health-consent 2>&1 | tee -a frontend.log

      - name: Test candidate experience translations
        shell: bash
        run: |
          set -o pipefail
          npm run test:candidate-i18n 2>&1 | tee -a frontend.log

''',
    "etapa de CI para i18n",
)
ci_path.write_text(ci, encoding="utf-8")

print("Issue #489 patch applied successfully")
