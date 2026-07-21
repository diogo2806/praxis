import { createFileRoute } from "@tanstack/react-router";
import {
  ArrowLeft,
  Baby,
  Clock3,
  Cookie,
  Database,
  Globe2,
  LockKeyhole,
  Mail,
  MousePointerClick,
  RefreshCw,
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
    title: "Política de Privacidade do Práxis",
    intro:
      "Esta Política explica como o Práxis trata dados pessoais de visitantes, usuários de empresas clientes e pessoas candidatas. Antes de uma avaliação, também são exibidas as informações específicas da participação, como empresa controladora, base legal, retenção e canal de atendimento.",
    updated: "Vigente desde 21 de julho de 2026 · versão 1.0",
    back: "Voltar para a página anterior",
    sections: [
      {
        icon: Users,
        title: "Abrangência e papéis",
        paragraphs: [
          "A iForce fornece e opera tecnicamente o Práxis. Esta Política se aplica ao site, à plataforma, às contas empresariais, aos canais de suporte e às avaliações realizadas pelo sistema.",
          "Nos processos seletivos, a empresa responsável pela vaga normalmente atua como controladora, pois define finalidade, critérios, base legal, retenção e pessoas autorizadas. O Práxis atua como operador seguindo suas instruções. A iForce pode atuar como controladora em atividades próprias, como segurança, suporte, faturamento e cumprimento de obrigações legais.",
        ],
      },
      {
        icon: Database,
        title: "Dados que podem ser tratados",
        items: [
          "dados cadastrais e profissionais, como nome, e-mail, telefone, cargo, empresa e identificadores de conta;",
          "dados da candidatura e participação, como vaga, convite, respostas, percurso, tempos, status e pontuações por competência;",
          "eventos técnicos, como sessão, endereço IP, navegador, dispositivo, visibilidade da aba, carregamento de mídia, modo de entrada e registros de segurança;",
          "dados de suporte, relacionamento, contratos, faturamento e processamento de pagamentos;",
          "preferências de idioma e acessibilidade;",
          "dados sensíveis somente quando houver necessidade, finalidade específica, base legal adequada e controles compatíveis.",
        ],
      },
      {
        icon: MousePointerClick,
        title: "Como os dados são obtidos",
        paragraphs: [
          "Os dados podem ser fornecidos diretamente pela pessoa titular, pela empresa responsável pelo processo seletivo, por plataformas de recrutamento e integrações autorizadas, ou coletados automaticamente durante o uso da plataforma.",
          "Também podemos receber informações de prestadores necessários à operação, como serviços de infraestrutura, autenticação, comunicação, suporte e pagamento.",
        ],
      },
      {
        icon: Scale,
        title: "Finalidades e bases legais",
        items: [
          "criar e administrar contas e empresas clientes;",
          "disponibilizar avaliações, convites e integrações;",
          "registrar respostas, percurso e evidências técnicas da execução;",
          "calcular resultados conforme critérios previamente configurados;",
          "permitir revisão humana e atendimento de solicitações;",
          "proteger contas, prevenir fraude, abuso e acesso indevido;",
          "prestar suporte, processar pagamentos e manter trilhas de auditoria;",
          "cumprir contratos, obrigações legais e exercer direitos;",
          "melhorar segurança, confiabilidade e desempenho, preferencialmente com dados agregados ou anonimizados.",
        ],
        footer:
          "A base legal depende da atividade e do papel de cada agente. O aceite desta Política confirma ciência sobre o tratamento, mas não transforma automaticamente o consentimento na base legal de todas as operações.",
      },
      {
        icon: UserRoundCheck,
        title: "Avaliações, pontuação e decisão humana",
        paragraphs: [
          "As avaliações usam alternativas, pesos e critérios definidos previamente. A pontuação é calculada de forma determinística. O Práxis não utiliza IA generativa para interpretar respostas livres ou decidir sozinho sobre contratação, eliminação ou aprovação.",
          "Eventos técnicos de integridade são mantidos separados da pontuação, não alteram automaticamente o resultado e qualquer consequência relevante deve ser submetida à revisão humana da empresa responsável.",
        ],
      },
      {
        icon: Cookie,
        title: "Cookies e tecnologias semelhantes",
        paragraphs: [
          "O Práxis pode utilizar cookies e armazenamento local estritamente necessários para autenticação, segurança, continuidade da sessão, idioma, acessibilidade e funcionamento da plataforma.",
          "Tecnologias não essenciais somente devem ser usadas quando houver fundamento adequado e, quando exigido, mecanismo de escolha. O bloqueio de recursos essenciais pode impedir o funcionamento de algumas áreas.",
        ],
      },
      {
        icon: Share2,
        title: "Compartilhamento",
        paragraphs: [
          "Os dados podem ser compartilhados com a empresa controladora, pessoas autorizadas no processo seletivo, sistemas integrados, fornecedores de infraestrutura, hospedagem, banco de dados, monitoramento, comunicação, autenticação, suporte e pagamento, além de autoridades quando houver obrigação legal ou ordem válida.",
          "Cada destinatário deve receber somente os dados necessários à sua atividade e observar obrigações de segurança e confidencialidade. O Práxis não vende dados pessoais de pessoas candidatas ou usuários.",
        ],
      },
      {
        icon: Globe2,
        title: "Transferências internacionais",
        paragraphs: [
          "Alguns fornecedores de tecnologia podem processar ou armazenar dados fora do Brasil. Quando houver transferência internacional, serão utilizados mecanismos admitidos pela LGPD e pela regulamentação aplicável, além de medidas contratuais e de segurança compatíveis com o risco.",
        ],
      },
      {
        icon: Clock3,
        title: "Retenção, anonimização e descarte",
        paragraphs: [
          "Nos processos seletivos, o período de retenção é definido pela empresa controladora e apresentado na participação quando aplicável. A iForce mantém dados pelo tempo necessário às finalidades, contratos e obrigações legais.",
          "Depois desse período, os dados serão eliminados ou anonimizados, salvo quando a conservação for necessária para obrigação legal, auditoria, prevenção a fraude ou exercício regular de direitos. Cópias de segurança podem permanecer por prazo adicional controlado até sua substituição segura.",
        ],
      },
      {
        icon: LockKeyhole,
        title: "Segurança e incidentes",
        paragraphs: [
          "O Práxis adota controles de acesso por perfil e empresa, segregação lógica, proteção de credenciais, comunicação segura, trilhas de auditoria, monitoramento, cópias de segurança e procedimentos de resposta a incidentes.",
          "Nenhum ambiente é totalmente isento de riscos. Incidentes confirmados que possam causar risco ou dano relevante serão avaliados e comunicados aos agentes e pessoas cabíveis quando a legislação e a regulamentação exigirem.",
        ],
      },
      {
        icon: ShieldCheck,
        title: "Direitos da pessoa titular",
        items: [
          "confirmar a existência do tratamento e acessar os dados;",
          "corrigir dados incompletos, inexatos ou desatualizados;",
          "solicitar anonimização, bloqueio ou eliminação de dados desnecessários, excessivos ou irregulares;",
          "solicitar portabilidade, quando aplicável e regulamentada;",
          "obter informação sobre compartilhamentos;",
          "eliminar dados tratados com base em consentimento, quando aplicável;",
          "revogar consentimento ou se opor a tratamento irregular;",
          "solicitar revisão de decisão tomada unicamente com base em tratamento automatizado, quando aplicável.",
        ],
        footer:
          "Pessoas candidatas devem usar preferencialmente a seção “Exercer um direito sobre meus dados” no link da participação. A identidade poderá ser verificada antes do atendimento para evitar acesso indevido.",
      },
      {
        icon: Baby,
        title: "Crianças e adolescentes",
        paragraphs: [
          "O Práxis não é direcionado ao uso autônomo por crianças. Processos que envolvam adolescentes devem ser organizados pela empresa controladora de forma compatível com a legislação, considerando o melhor interesse da pessoa menor de idade e as autorizações exigidas no caso concreto.",
        ],
      },
      {
        icon: RefreshCw,
        title: "Alterações desta Política",
        paragraphs: [
          "Esta Política pode ser atualizada para refletir mudanças legais, regulatórias, contratuais ou técnicas. A versão vigente e a data de atualização serão publicadas nesta página.",
          "Quando uma alteração modificar de forma relevante o tratamento aplicável a uma participação, a nova versão deverá ser apresentada antes da continuidade da avaliação e poderá exigir nova ciência.",
        ],
      },
    ],
    finalTitle: "Canal de atendimento",
    finalText:
      "Para assuntos gerais sobre privacidade no Práxis, use o e-mail abaixo. Em assuntos relacionados a uma candidatura, procure primeiro a empresa responsável pelo processo seletivo ou utilize o formulário disponível no link da participação.",
    emailLabel: "contato@iforce.com.br",
  },
  en: {
    badge: "Privacy and data protection",
    title: "Praxis Privacy Policy",
    intro:
      "This Policy explains how Praxis processes personal data from website visitors, customer-company users, and candidates. Before an assessment, participation-specific information is also shown, including the controller, legal ground, retention period, and support channel.",
    updated: "Effective July 21, 2026 · version 1.0",
    back: "Go back",
    sections: [
      {
        icon: Users,
        title: "Scope and roles",
        paragraphs: [
          "iForce provides and technically operates Praxis. This Policy applies to the website, platform, company accounts, support channels, and assessments delivered through the system.",
          "For recruitment assessments, the company responsible for the role normally acts as controller and defines purpose, criteria, legal ground, retention, and authorized access. Praxis acts as processor under its instructions. iForce may act as controller for its own security, support, billing, commercial, and legal-compliance activities.",
        ],
      },
      {
        icon: Database,
        title: "Data that may be processed",
        items: [
          "account and professional data, such as name, email, phone number, job title, company, and account identifiers;",
          "application and participation data, such as role, invitation, answers, journey, timing, status, and competency scores;",
          "technical events, including session, IP address, browser, device, tab visibility, media loading, input mode, and security records;",
          "support, commercial, contractual, billing, and payment-processing data;",
          "language and accessibility preferences;",
          "sensitive data only when necessary, purpose-specific, supported by an appropriate legal ground, and protected by suitable controls.",
        ],
      },
      {
        icon: MousePointerClick,
        title: "How data is collected",
        paragraphs: [
          "Data may be provided by the individual, the company responsible for the recruitment process, authorized recruiting platforms and integrations, or collected automatically while the platform is used.",
          "We may also receive information from providers required for infrastructure, authentication, communications, support, and payments.",
        ],
      },
      {
        icon: Scale,
        title: "Purposes and legal grounds",
        items: [
          "create and manage customer accounts and companies;",
          "provide assessments, invitations, and integrations;",
          "record answers, journeys, and technical execution evidence;",
          "calculate results under predefined criteria;",
          "enable human review and rights requests;",
          "protect accounts and prevent fraud, abuse, and unauthorized access;",
          "provide support, process payments, and maintain audit trails;",
          "perform contracts, comply with legal duties, and exercise legal rights;",
          "improve security, reliability, and performance, preferably with aggregated or anonymized data.",
        ],
        footer:
          "The applicable legal ground depends on the activity and each party's role. Acknowledging this Policy confirms awareness of the processing and does not automatically make consent the legal ground for every operation.",
      },
      {
        icon: UserRoundCheck,
        title: "Assessments, scoring, and human decisions",
        paragraphs: [
          "Assessments use alternatives, weights, and criteria defined in advance. Scoring is deterministic. Praxis does not use generative AI to interpret free-text answers or independently decide hiring, rejection, or approval.",
          "Technical integrity events are separated from scoring, do not automatically change the result, and any relevant consequence must be reviewed by the responsible company.",
        ],
      },
      {
        icon: Cookie,
        title: "Cookies and similar technologies",
        paragraphs: [
          "Praxis may use cookies and local storage strictly required for authentication, security, session continuity, language, accessibility, and platform operation.",
          "Non-essential technologies should only be used with an appropriate basis and, where required, user choice. Blocking essential browser features may prevent parts of the service from working.",
        ],
      },
      {
        icon: Share2,
        title: "Sharing",
        paragraphs: [
          "Data may be shared with the controller, authorized recruitment users, integrated systems, infrastructure and hosting providers, databases, monitoring, communications, authentication, support, payment providers, and public authorities when legally required.",
          "Recipients must receive only the data necessary for their role and follow security and confidentiality obligations. Praxis does not sell candidate or user personal data.",
        ],
      },
      {
        icon: Globe2,
        title: "International transfers",
        paragraphs: [
          "Some technology providers may process or store data outside Brazil. International transfers will rely on mechanisms accepted by applicable data-protection law and include suitable contractual and security safeguards.",
        ],
      },
      {
        icon: Clock3,
        title: "Retention and deletion",
        paragraphs: [
          "For recruitment processes, retention is set by the controller and shown in the participation flow when applicable. iForce keeps data only as long as needed for the stated purposes, contracts, and legal duties.",
          "Data is then deleted or anonymized unless retention is required for legal obligations, audits, fraud prevention, or legal claims. Backups may remain for an additional controlled period until securely replaced.",
        ],
      },
      {
        icon: LockKeyhole,
        title: "Security and incidents",
        paragraphs: [
          "Praxis applies role- and company-based access control, logical segregation, credential protection, secure communications, audit trails, monitoring, backups, and incident-response procedures.",
          "No environment is risk-free. Confirmed incidents that may cause relevant risk or harm will be assessed and communicated to the appropriate parties when required by law.",
        ],
      },
      {
        icon: ShieldCheck,
        title: "Your rights",
        items: [
          "confirm processing and access your data;",
          "correct incomplete, inaccurate, or outdated data;",
          "request anonymization, blocking, or deletion of unnecessary, excessive, or unlawful data;",
          "request portability where applicable and regulated;",
          "obtain information about sharing;",
          "request deletion of data processed on consent where applicable;",
          "withdraw consent or object to unlawful processing;",
          "request review of a decision based solely on automated processing where applicable.",
        ],
        footer:
          "Candidates should preferably use the “Exercise a right over my data” section in their participation link. Identity may be verified before a request is fulfilled to prevent unauthorized access.",
      },
      {
        icon: Baby,
        title: "Children and teenagers",
        paragraphs: [
          "Praxis is not designed for autonomous use by children. Processes involving teenagers must be organized by the controller under applicable law, considering the minor's best interests and any required authorizations.",
        ],
      },
      {
        icon: RefreshCw,
        title: "Policy updates",
        paragraphs: [
          "This Policy may be updated for legal, regulatory, contractual, or technical changes. The current version and update date will be published on this page.",
          "If a change materially affects an assessment, the new version must be presented before the assessment continues and may require a new acknowledgement.",
        ],
      },
    ],
    finalTitle: "Contact channel",
    finalText:
      "For general privacy matters related to Praxis, use the email below. For application-specific matters, first contact the company responsible for the recruitment process or use the form in your participation link.",
    emailLabel: "contato@iforce.com.br",
  },
  "es-MX": {
    badge: "Privacidad y protección de datos",
    title: "Política de Privacidad de Práxis",
    intro:
      "Esta Política explica cómo Práxis trata datos personales de visitantes, usuarios de empresas clientes y personas candidatas. Antes de una evaluación también se muestran datos específicos de la participación, como la empresa responsable, la base legal, la conservación y el canal de atención.",
    updated: "Vigente desde el 21 de julio de 2026 · versión 1.0",
    back: "Volver",
    sections: [
      {
        icon: Users,
        title: "Alcance y funciones",
        paragraphs: [
          "iForce proporciona y opera técnicamente Práxis. Esta Política se aplica al sitio, la plataforma, las cuentas empresariales, los canales de soporte y las evaluaciones realizadas por el sistema.",
          "En procesos de selección, la empresa responsable de la vacante normalmente actúa como responsable del tratamiento y define finalidad, criterios, base legal, conservación y acceso. Práxis actúa como encargado siguiendo sus instrucciones. iForce puede actuar como responsable en actividades propias de seguridad, soporte, facturación y cumplimiento legal.",
        ],
      },
      {
        icon: Database,
        title: "Datos que pueden tratarse",
        items: [
          "datos de registro y profesionales, como nombre, correo, teléfono, cargo, empresa e identificadores de cuenta;",
          "datos de candidatura y participación, como vacante, invitación, respuestas, recorrido, tiempos, estado y puntuaciones por competencia;",
          "eventos técnicos, como sesión, dirección IP, navegador, dispositivo, visibilidad de pestaña, carga de medios, modo de entrada y registros de seguridad;",
          "datos de soporte, relación comercial, contratos, facturación y pagos;",
          "preferencias de idioma y accesibilidad;",
          "datos sensibles solo cuando sean necesarios, tengan una finalidad específica, una base legal adecuada y controles compatibles.",
        ],
      },
      {
        icon: MousePointerClick,
        title: "Cómo se obtienen los datos",
        paragraphs: [
          "Los datos pueden ser proporcionados por la persona titular, la empresa responsable del proceso, plataformas de reclutamiento e integraciones autorizadas, o recopilarse automáticamente durante el uso.",
          "También pueden provenir de proveedores necesarios para infraestructura, autenticación, comunicación, soporte y pagos.",
        ],
      },
      {
        icon: Scale,
        title: "Finalidades y bases legales",
        items: [
          "crear y administrar cuentas y empresas clientes;",
          "ofrecer evaluaciones, invitaciones e integraciones;",
          "registrar respuestas, recorrido y evidencias técnicas;",
          "calcular resultados según criterios predefinidos;",
          "permitir revisión humana y solicitudes de derechos;",
          "proteger cuentas y prevenir fraude, abuso y acceso indebido;",
          "prestar soporte, procesar pagos y mantener auditorías;",
          "cumplir contratos, obligaciones legales y ejercer derechos;",
          "mejorar seguridad, confiabilidad y rendimiento, preferentemente con datos agregados o anonimizados.",
        ],
        footer:
          "La base legal depende de la actividad y del rol de cada parte. Conocer esta Política no convierte automáticamente el consentimiento en la base legal de todas las operaciones.",
      },
      {
        icon: UserRoundCheck,
        title: "Evaluaciones, puntuación y decisión humana",
        paragraphs: [
          "Las evaluaciones usan alternativas, pesos y criterios definidos previamente. La puntuación es determinista. Práxis no usa IA generativa para interpretar respuestas libres ni decidir por sí solo una contratación, eliminación o aprobación.",
          "Los eventos técnicos de integridad se mantienen separados de la puntuación, no cambian automáticamente el resultado y cualquier consecuencia relevante requiere revisión humana de la empresa responsable.",
        ],
      },
      {
        icon: Cookie,
        title: "Cookies y tecnologías similares",
        paragraphs: [
          "Práxis puede usar cookies y almacenamiento local estrictamente necesarios para autenticación, seguridad, continuidad de sesión, idioma, accesibilidad y funcionamiento.",
          "Las tecnologías no esenciales solo deben usarse con una base adecuada y, cuando corresponda, con elección de la persona usuaria. Bloquear funciones esenciales puede impedir el uso de algunas áreas.",
        ],
      },
      {
        icon: Share2,
        title: "Compartición",
        paragraphs: [
          "Los datos pueden compartirse con la empresa responsable, personas autorizadas, sistemas integrados, proveedores de infraestructura, alojamiento, bases de datos, monitoreo, comunicación, autenticación, soporte y pagos, y autoridades cuando exista obligación legal.",
          "Cada destinatario debe recibir solo los datos necesarios y cumplir obligaciones de seguridad y confidencialidad. Práxis no vende datos personales de personas candidatas o usuarias.",
        ],
      },
      {
        icon: Globe2,
        title: "Transferencias internacionales",
        paragraphs: [
          "Algunos proveedores tecnológicos pueden procesar o almacenar datos fuera de Brasil. Las transferencias internacionales utilizarán mecanismos admitidos por la legislación aplicable y medidas contractuales y de seguridad adecuadas.",
        ],
      },
      {
        icon: Clock3,
        title: "Conservación y eliminación",
        paragraphs: [
          "En procesos de selección, el plazo de conservación es definido por la empresa responsable y se muestra en la participación cuando corresponde. iForce conserva datos solo durante el tiempo necesario para las finalidades, contratos y obligaciones legales.",
          "Después, los datos se eliminan o anonimizan, salvo que deban conservarse por obligación legal, auditoría, prevención de fraude o ejercicio de derechos. Las copias de seguridad pueden permanecer por un plazo adicional controlado.",
        ],
      },
      {
        icon: LockKeyhole,
        title: "Seguridad e incidentes",
        paragraphs: [
          "Práxis aplica control de acceso por perfil y empresa, segregación lógica, protección de credenciales, comunicaciones seguras, auditorías, monitoreo, copias de seguridad y procedimientos de respuesta a incidentes.",
          "Ningún entorno está libre de riesgos. Los incidentes confirmados que puedan causar riesgo o daño relevante serán evaluados y comunicados cuando la ley lo exija.",
        ],
      },
      {
        icon: ShieldCheck,
        title: "Tus derechos",
        items: [
          "confirmar el tratamiento y acceder a tus datos;",
          "corregir datos incompletos, inexactos o desactualizados;",
          "solicitar anonimización, bloqueo o eliminación de datos innecesarios, excesivos o irregulares;",
          "solicitar portabilidad cuando corresponda;",
          "obtener información sobre comparticiones;",
          "eliminar datos tratados con consentimiento cuando corresponda;",
          "revocar el consentimiento u oponerte a un tratamiento irregular;",
          "solicitar revisión de una decisión basada únicamente en tratamiento automatizado cuando corresponda.",
        ],
        footer:
          "Las personas candidatas deben usar preferentemente la sección “Ejercer un derecho sobre mis datos” en el enlace de participación. La identidad puede verificarse antes de atender la solicitud.",
      },
      {
        icon: Baby,
        title: "Niñas, niños y adolescentes",
        paragraphs: [
          "Práxis no está dirigido al uso autónomo por niñas o niños. Los procesos que involucren adolescentes deben ser organizados por la empresa responsable conforme a la legislación, considerando el interés superior de la persona menor de edad y las autorizaciones necesarias.",
        ],
      },
      {
        icon: RefreshCw,
        title: "Cambios de esta Política",
        paragraphs: [
          "Esta Política puede actualizarse por cambios legales, regulatorios, contractuales o técnicos. La versión vigente y la fecha se publicarán en esta página.",
          "Si un cambio afecta de forma relevante una evaluación, la nueva versión deberá presentarse antes de continuar y podrá requerir un nuevo registro de conocimiento.",
        ],
      },
    ],
    finalTitle: "Canal de atención",
    finalText:
      "Para asuntos generales de privacidad de Práxis, usa el correo siguiente. Para temas de una candidatura, comunícate primero con la empresa responsable o usa el formulario del enlace de participación.",
    emailLabel: "contato@iforce.com.br",
  },
} as const;

export const Route = createFileRoute("/privacidade")({
  head: () => ({
    meta: [
      { title: "Política de Privacidade do Práxis" },
      {
        name: "description",
        content:
          "Política de Privacidade do Práxis: tratamento de dados, avaliações, segurança, retenção e direitos das pessoas titulares.",
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
          <div className="flex items-start gap-3">
            <Mail className="mt-0.5 h-5 w-5 shrink-0 text-primary" aria-hidden />
            <div>
              <h2 className="text-lg font-semibold">{copy.finalTitle}</h2>
              <p className="mt-2 text-sm leading-7 text-muted-foreground">{copy.finalText}</p>
              <a
                href={`mailto:${copy.emailLabel}`}
                className="mt-3 inline-flex text-sm font-semibold text-primary underline-offset-4 hover:underline"
              >
                {copy.emailLabel}
              </a>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
