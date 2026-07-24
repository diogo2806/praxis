import type { ScreenManualDefinition } from "@/lib/screen-manuals";

export const ENTERPRISE_AUTH_MANUALS: ScreenManualDefinition[] = [
  {
    id: "sso-corporativo-mfa",
    title: "SSO corporativo e MFA",
    purpose:
      "Configurar autenticação corporativa OpenID Connect por empresa, exigir MFA no provedor e impedir senha local quando o domínio estiver sob política obrigatória.",
    flow: [
      "Cadastre nome, issuer, client ID, redirect URI e a variável de ambiente que contém o client secret.",
      "Informe os domínios corporativos, escopos e perfil padrão de provisionamento.",
      "Defina se o primeiro acesso pode criar a identidade automaticamente.",
      "Ative a exigência de MFA e informe os valores AMR aceitos pelo provedor.",
      "Teste discovery OIDC e presença da variável de segredo antes de ativar.",
      "Ative o provedor e, somente após homologar, bloqueie a senha local para os domínios.",
      "Acompanhe inícios, conclusões, recusas e erros na auditoria da tela.",
    ],
    fields: [
      { name: "Issuer", description: "URL HTTPS do emissor OIDC usada para obter discovery, endpoints e chaves públicas." },
      { name: "Client ID", description: "Identificador público da aplicação Práxis no provedor corporativo." },
      { name: "Variável do client secret", description: "Nome da variável de ambiente que guarda o segredo; o valor não é persistido no banco." },
      { name: "Redirect URI", description: "Callback cadastrado no provedor, preferencialmente /api/v1/enterprise-auth/callback/redirect." },
      { name: "Destino após autenticação", description: "Tela segura do frontend que conclui a sessão e encaminha a pessoa usuária." },
      { name: "Domínios autorizados", description: "Domínios de e-mail que podem usar o provedor e, quando exigido, não podem usar senha local." },
      { name: "Provisionamento automático", description: "Cria a identidade corporativa no primeiro acesso com o perfil padrão configurado." },
      { name: "Valores AMR", description: "Evidências de MFA aceitas, como mfa, otp, hwk ou sms, conforme claims do IdP." },
      { name: "SSO obrigatório", description: "Bloqueia o endpoint de senha para os domínios configurados e direciona ao acesso corporativo." },
    ],
    permissions: [
      "Somente gestores da empresa e administradores podem criar, alterar, testar e ativar provedores.",
      "Descoberta, início e callback são públicos, mas protegidos por domínio, state, nonce, PKCE e assinatura do IdP.",
      "A sessão recebe apenas o perfil padrão permitido e continua sujeita às regras de autorização do backend.",
    ],
    states: [
      "Rascunho inativo",
      "Teste com sucesso",
      "Teste com erro",
      "Ativo com SSO opcional",
      "Ativo com SSO obrigatório",
      "MFA confirmada",
      "MFA ausente ou recusada",
      "Identidade desativada",
    ],
    blocks: [
      "Issuer, redirect URI ou destino sem HTTPS.",
      "Variável de client secret ausente no ambiente.",
      "Domínio não autorizado.",
      "State, nonce, PKCE, issuer, audience ou assinatura divergente.",
      "Claim AMR não comprova MFA quando a política exige segundo fator.",
      "Identidade não provisionada com JIT desativado.",
      "Senha local usada por domínio com SSO obrigatório.",
    ],
    examples: [
      "Configurar Microsoft Entra ID com issuer do tenant, client ID e segredo em AZURE_PRAXIS_CLIENT_SECRET.",
      "Permitir SSO opcional durante homologação e ativar bloqueio de senha somente depois do teste completo.",
      "Exigir claim AMR mfa ou hwk para gestores da empresa.",
    ],
    shortcuts: [
      "Use Testar discovery e segredo antes de marcar o provedor como ativo.",
      "Mantenha ao menos uma conta administrativa de contingência fora do domínio bloqueado.",
      "Nunca coloque o client secret na tela, banco, commit ou log.",
      "Consulte a auditoria após mudanças de issuer, domínio, MFA ou perfil padrão.",
      "O processo completo está disponível na Central de manuais.",
    ],
    matches: (pathname) => pathname === "/configuracoes/sso",
  },
];

export function resolveEnterpriseAuthManual(pathname: string) {
  return ENTERPRISE_AUTH_MANUALS.find((manual) => manual.matches(pathname));
}
