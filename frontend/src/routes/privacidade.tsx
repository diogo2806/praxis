import { createFileRoute } from "@tanstack/react-router";
import {
  ArrowLeft,
  Clock3,
  Database,
  LockKeyhole,
  Scale,
  Share2,
  ShieldCheck,
  UserRoundCheck,
  Users,
} from "lucide-react";
import { LanguageSelector } from "@/components/language-selector";
import { useLanguage } from "@/lib/language-context";

const privacyCopy = {
  "pt-BR": {
    badge: "Privacidade e proteção de dados",
    title: "Aviso de Privacidade da pessoa candidata",
    intro:
      "Este aviso explica como os dados são tratados quando você participa de uma avaliação no Práxis. Ele complementa as informações fornecidas pela empresa responsável pelo processo seletivo.",
    updated: "Atualizado em 18 de julho de 2026 · versão 1.0",
    back: "Voltar para a página anterior",
    sections: [
      {
        icon: Users,
        title: "Quem decide sobre o tratamento",
        paragraphs: [
          "A empresa responsável pela vaga ou pelo processo seletivo atua como controladora: ela define a finalidade da avaliação, os critérios, o período de retenção e quem pode acessar o resultado.",
          "O Práxis atua como operador no fluxo da avaliação, tratando os dados conforme as instruções da controladora. A fornecedora do Práxis pode atuar como controladora apenas em atividades próprias, como segurança, prevenção a fraudes, suporte, faturamento e cumprimento de obrigações legais.",
        ],
      },
      {
        icon: Database,
        title: "Quais dados podem ser tratados",
        items: [
          "nome, e-mail e identificadores da candidatura, vaga, empresa e participação;",
          "respostas, percurso realizado, tempos, status e pontuações por competência;",
          "registros de auditoria, informações técnicas de acesso e eventos de segurança;",
          "preferências de acessibilidade e, quando aplicável, dados sensíveis informados em fluxo específico e autorizado.",
        ],
      },
      {
        icon: Scale,
        title: "Finalidades e bases legais",
        paragraphs: [
          "Os dados são usados para disponibilizar a avaliação, calcular o resultado conforme critérios previamente definidos, manter a rastreabilidade, permitir revisão humana, integrar o resultado ao sistema da empresa e atender obrigações legais e de segurança.",
          "A base legal é definida pela controladora conforme o caso, podendo envolver procedimentos preliminares relacionados ao contrato de trabalho, legítimo interesse devidamente avaliado, cumprimento de obrigação legal ou regulatória e consentimento específico quando realmente necessário, especialmente para dados sensíveis.",
        ],
      },
      {
        icon: UserRoundCheck,
        title: "Pontuação e decisão humana",
        paragraphs: [
          "O Práxis calcula pontuações de forma determinística a partir de alternativas, pesos e critérios definidos antes da aplicação. O sistema não usa IA generativa para interpretar texto livre ou decidir sobre a candidatura.",
          "A pontuação é um apoio à análise. A decisão final deve permanecer sob responsabilidade humana. Você pode solicitar revisão humana do resultado pelo próprio link da participação.",
        ],
      },
      {
        icon: Share2,
        title: "Compartilhamento",
        paragraphs: [
          "Os dados podem ser disponibilizados à empresa controladora, às pessoas autorizadas no processo seletivo e aos sistemas integrados necessários ao fluxo, como plataformas de recrutamento e serviços de infraestrutura contratados.",
          "O acesso deve respeitar finalidade, necessidade, contrato, confidencialidade e controles de permissão. O Práxis não comercializa dados de pessoas candidatas.",
        ],
      },
      {
        icon: Clock3,
        title: "Retenção e descarte",
        paragraphs: [
          "O período de retenção é configurado conforme as instruções da controladora e as obrigações aplicáveis. Ao fim do período, o Práxis executa rotinas de anonimização dos dados pessoais, preservando somente registros que precisem ser mantidos por obrigação legal, auditoria ou defesa de direitos.",
        ],
      },
      {
        icon: LockKeyhole,
        title: "Segurança",
        paragraphs: [
          "São adotados controles de acesso por perfil e empresa, trilhas de auditoria, proteção de credenciais, comunicação segura, segregação lógica, monitoramento e procedimentos de resposta a incidentes. Nenhuma medida elimina totalmente os riscos, mas os controles são revisados conforme a natureza do tratamento.",
        ],
      },
      {
        icon: ShieldCheck,
        title: "Seus direitos",
        items: [
          "confirmar a existência do tratamento e acessar seus dados;",
          "corrigir dados incompletos, inexatos ou desatualizados;",
          "solicitar anonimização, bloqueio ou eliminação de dados desnecessários ou excessivos;",
          "solicitar portabilidade, quando aplicável e regulamentada;",
          "obter informação sobre compartilhamentos;",
          "revogar consentimento ou solicitar eliminação de dados tratados com essa base, quando aplicável;",
          "solicitar revisão humana de resultado ou decisão baseada em tratamento automatizado.",
        ],
        footer:
          "Use a seção “Exercer um direito sobre meus dados” no link da sua participação. O pedido fica vinculado à tentativa e registrado na trilha de auditoria para atendimento pela empresa responsável.",
      },
    ],
    finalTitle: "Canal de atendimento",
    finalText:
      "A empresa responsável pelo processo seletivo é o primeiro canal para dúvidas sobre finalidade, base legal, retenção e decisão. Para solicitações vinculadas à avaliação, use o formulário disponível na página da sua participação.",
  },
  en: {
    badge: "Privacy and data protection",
    title: "Candidate Privacy Notice",
    intro:
      "This notice explains how data is processed when you take an assessment in Praxis. It supplements the information provided by the company responsible for the recruitment process.",
    updated: "Updated July 18, 2026 · version 1.0",
    back: "Go back",
    sections: [
      {
        icon: Users,
        title: "Who decides how data is used",
        paragraphs: [
          "The company responsible for the role or recruitment process is the controller. It defines the assessment purpose, criteria, retention period, and who may access the result.",
          "Praxis acts as a processor for the assessment flow and follows the controller's instructions. The Praxis provider may act as a controller only for its own activities, such as security, fraud prevention, support, billing, and legal compliance.",
        ],
      },
      {
        icon: Database,
        title: "Data that may be processed",
        items: [
          "name, email, and identifiers for the application, role, company, and participation;",
          "answers, journey, timing, status, and competency scores;",
          "audit records, technical access information, and security events;",
          "accessibility preferences and, when applicable, sensitive data provided through a specific authorized flow.",
        ],
      },
      {
        icon: Scale,
        title: "Purposes and legal grounds",
        paragraphs: [
          "Data is used to provide the assessment, calculate results under predefined criteria, preserve traceability, enable human review, integrate results with the company's systems, and meet legal and security obligations.",
          "The controller determines the applicable legal ground, which may include pre-contractual procedures, a properly assessed legitimate interest, legal or regulatory obligations, and specific consent when genuinely required, especially for sensitive data.",
        ],
      },
      {
        icon: UserRoundCheck,
        title: "Scoring and human decision",
        paragraphs: [
          "Praxis calculates scores deterministically from alternatives, weights, and criteria defined before the assessment. It does not use generative AI to interpret free text or decide on an application.",
          "The score supports the analysis. The final decision must remain human. You may request a human review through your participation link.",
        ],
      },
      {
        icon: Share2,
        title: "Sharing",
        paragraphs: [
          "Data may be made available to the controller, authorized people in the recruitment process, and systems required for the flow, such as applicant tracking platforms and contracted infrastructure providers.",
          "Access must follow purpose, necessity, contract, confidentiality, and permission controls. Praxis does not sell candidate data.",
        ],
      },
      {
        icon: Clock3,
        title: "Retention and disposal",
        paragraphs: [
          "The retention period follows the controller's instructions and applicable obligations. At the end of that period, Praxis runs routines to anonymize personal data while preserving only records that must remain for legal obligations, audits, or the defense of rights.",
        ],
      },
      {
        icon: LockKeyhole,
        title: "Security",
        paragraphs: [
          "Praxis applies access controls by role and company, audit trails, credential protection, secure communications, logical segregation, monitoring, and incident response procedures. No measure removes all risk, but controls are reviewed according to the nature of the processing.",
        ],
      },
      {
        icon: ShieldCheck,
        title: "Your rights",
        items: [
          "confirm processing and access your data;",
          "correct incomplete, inaccurate, or outdated data;",
          "request anonymization, blocking, or deletion of unnecessary or excessive data;",
          "request portability when applicable and regulated;",
          "obtain information about data sharing;",
          "withdraw consent or request deletion of data processed under consent, when applicable;",
          "request human review of a result or decision based on automated processing.",
        ],
        footer:
          "Use the “Exercise a right over my data” section in your participation link. The request is linked to the assessment attempt and added to the audit trail for handling by the responsible company.",
      },
    ],
    finalTitle: "Support channel",
    finalText:
      "The company responsible for the recruitment process is the first point of contact for questions about purpose, legal grounds, retention, and decisions. For requests related to the assessment, use the form available on your participation page.",
  },
  "es-MX": {
    badge: "Privacidad y protección de datos",
    title: "Aviso de Privacidad para la persona candidata",
    intro:
      "Este aviso explica cómo se tratan los datos cuando participas en una evaluación en Práxis. Complementa la información proporcionada por la empresa responsable del proceso de selección.",
    updated: "Actualizado el 18 de julio de 2026 · versión 1.0",
    back: "Volver a la página anterior",
    sections: [
      {
        icon: Users,
        title: "Quién decide sobre el tratamiento",
        paragraphs: [
          "La empresa responsable de la vacante o del proceso de selección actúa como responsable del tratamiento. Define la finalidad, los criterios, el periodo de conservación y quién puede acceder al resultado.",
          "Práxis actúa como encargado en el flujo de evaluación y sigue las instrucciones de la empresa. El proveedor de Práxis puede actuar como responsable solo en actividades propias, como seguridad, prevención de fraude, soporte, facturación y cumplimiento legal.",
        ],
      },
      {
        icon: Database,
        title: "Datos que pueden tratarse",
        items: [
          "nombre, correo e identificadores de la candidatura, vacante, empresa y participación;",
          "respuestas, recorrido, tiempos, estado y puntuaciones por competencia;",
          "registros de auditoría, información técnica de acceso y eventos de seguridad;",
          "preferencias de accesibilidad y, cuando corresponda, datos sensibles informados en un flujo específico y autorizado.",
        ],
      },
      {
        icon: Scale,
        title: "Finalidades y bases legales",
        paragraphs: [
          "Los datos se usan para ofrecer la evaluación, calcular el resultado según criterios predefinidos, mantener la trazabilidad, permitir revisión humana, integrar el resultado con los sistemas de la empresa y cumplir obligaciones legales y de seguridad.",
          "La empresa responsable define la base legal aplicable, que puede incluir medidas precontractuales, interés legítimo debidamente evaluado, obligaciones legales o reglamentarias y consentimiento específico cuando sea realmente necesario, especialmente para datos sensibles.",
        ],
      },
      {
        icon: UserRoundCheck,
        title: "Puntuación y decisión humana",
        paragraphs: [
          "Práxis calcula puntuaciones de forma determinista a partir de alternativas, pesos y criterios definidos antes de la aplicación. No utiliza IA generativa para interpretar texto libre ni decidir sobre la candidatura.",
          "La puntuación apoya el análisis. La decisión final debe seguir siendo humana. Puedes solicitar revisión humana desde el enlace de tu participación.",
        ],
      },
      {
        icon: Share2,
        title: "Compartición",
        paragraphs: [
          "Los datos pueden ponerse a disposición de la empresa responsable, de las personas autorizadas en el proceso y de los sistemas necesarios, como plataformas de reclutamiento y proveedores de infraestructura contratados.",
          "El acceso debe respetar finalidad, necesidad, contrato, confidencialidad y controles de permiso. Práxis no vende datos de personas candidatas.",
        ],
      },
      {
        icon: Clock3,
        title: "Conservación y eliminación",
        paragraphs: [
          "El periodo de conservación sigue las instrucciones de la empresa responsable y las obligaciones aplicables. Al finalizar, Práxis ejecuta rutinas de anonimización y conserva solo los registros necesarios por obligación legal, auditoría o defensa de derechos.",
        ],
      },
      {
        icon: LockKeyhole,
        title: "Seguridad",
        paragraphs: [
          "Se aplican controles de acceso por perfil y empresa, pistas de auditoría, protección de credenciales, comunicaciones seguras, segregación lógica, monitoreo y procedimientos de respuesta a incidentes. Ninguna medida elimina todo riesgo, pero los controles se revisan según la naturaleza del tratamiento.",
        ],
      },
      {
        icon: ShieldCheck,
        title: "Tus derechos",
        items: [
          "confirmar el tratamiento y acceder a tus datos;",
          "corregir datos incompletos, inexactos o desactualizados;",
          "solicitar anonimización, bloqueo o eliminación de datos innecesarios o excesivos;",
          "solicitar portabilidad cuando corresponda y esté regulada;",
          "obtener información sobre comparticiones;",
          "revocar el consentimiento o solicitar la eliminación de datos tratados con esa base, cuando corresponda;",
          "solicitar revisión humana de un resultado o decisión basada en tratamiento automatizado.",
        ],
        footer:
          "Usa la sección “Ejercer un derecho sobre mis datos” en el enlace de tu participación. La solicitud se vincula al intento y queda registrada en la pista de auditoría para atención por la empresa responsable.",
      },
    ],
    finalTitle: "Canal de atención",
    finalText:
      "La empresa responsable del proceso de selección es el primer canal para dudas sobre finalidad, base legal, conservación y decisión. Para solicitudes relacionadas con la evaluación, usa el formulario disponible en la página de tu participación.",
  },
} as const;

export const Route = createFileRoute("/privacidade")({
  head: () => ({
    meta: [
      { title: "Aviso de Privacidade da pessoa candidata - Práxis" },
      {
        name: "description",
        content:
          "Informações sobre tratamento de dados, pontuação, retenção e direitos da pessoa candidata no Práxis.",
      },
    ],
  }),
  component: PrivacyPage,
});

function PrivacyPage() {
  const { language } = useLanguage();
  const copy = privacyCopy[language];

  return (
    <main className="min-h-screen bg-background px-4 py-8 text-foreground sm:px-6 sm:py-12">
      <div className="mx-auto max-w-4xl">
        <div className="flex items-center justify-between gap-4">
          <button
            type="button"
            onClick={() => window.history.back()}
            className="inline-flex min-h-10 items-center gap-2 rounded-lg border border-border bg-card px-3 py-2 text-sm font-medium hover:bg-accent"
          >
            <ArrowLeft className="h-4 w-4" aria-hidden />
            {copy.back}
          </button>
          <LanguageSelector />
        </div>

        <header className="mt-8 rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-8">
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-primary">
            {copy.badge}
          </p>
          <h1 className="mt-3 font-serif text-3xl leading-tight sm:text-4xl">{copy.title}</h1>
          <p className="mt-4 max-w-3xl text-sm leading-7 text-muted-foreground">{copy.intro}</p>
          <p className="mt-4 text-xs text-muted-foreground">{copy.updated}</p>
        </header>

        <div className="mt-6 space-y-5">
          {copy.sections.map((section) => (
            <section key={section.title} className="rounded-2xl border border-border bg-card p-5 sm:p-6">
              <div className="flex items-start gap-3">
                <section.icon className="mt-0.5 h-5 w-5 shrink-0 text-primary" aria-hidden />
                <div className="min-w-0 flex-1">
                  <h2 className="text-lg font-semibold">{section.title}</h2>
                  {"paragraphs" in section
                    ? section.paragraphs?.map((paragraph) => (
                        <p key={paragraph} className="mt-3 text-sm leading-7 text-muted-foreground">
                          {paragraph}
                        </p>
                      ))
                    : null}
                  {"items" in section && section.items ? (
                    <ul className="mt-3 list-disc space-y-2 pl-5 text-sm leading-7 text-muted-foreground">
                      {section.items.map((item) => (
                        <li key={item}>{item}</li>
                      ))}
                    </ul>
                  ) : null}
                  {"footer" in section && section.footer ? (
                    <p className="mt-4 rounded-xl border border-primary/20 bg-primary/5 p-4 text-sm leading-7 text-foreground">
                      {section.footer}
                    </p>
                  ) : null}
                </div>
              </div>
            </section>
          ))}
        </div>

        <section className="mt-6 rounded-2xl border border-primary/20 bg-primary/5 p-5 sm:p-6">
          <h2 className="text-lg font-semibold">{copy.finalTitle}</h2>
          <p className="mt-2 text-sm leading-7 text-muted-foreground">{copy.finalText}</p>
        </section>
      </div>
    </main>
  );
}
