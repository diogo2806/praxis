import { Link, createFileRoute } from "@tanstack/react-router";
import {
  ArrowLeft,
  BookOpen,
  ExternalLink,
  KeyRound,
  ListChecks,
  Send,
  ShieldCheck,
  Webhook,
} from "lucide-react";
import type { ReactNode } from "react";

import { getApiBaseUrl } from "@/lib/runtime-config";

export const Route = createFileRoute("/docs/integracao-api-propria")({
  head: () => ({
    meta: [
      { title: "Integração via API própria - Práxis" },
      {
        name: "description",
        content:
          "Documentação da integração por API própria: token de API, webhook assinado por HMAC e formato dos eventos de resultado.",
      },
    ],
  }),
  component: CustomApiDocsPage,
});

const TOKEN_CURL_EXAMPLE = `curl -H "Authorization: Bearer prx_live_SEU_TOKEN" \\
  https://api.praxis.iforce.com.br/api/...`;

const RESULT_READY_PAYLOAD = `{
  "event": "RESULT_READY",
  "tenantId": "empresa-1",
  "attemptId": "att_8f2c1a",
  "simulationId": "sim_atendimento",
  "score": 78,
  "decision": "RECOMMEND_INTERVIEW",
  "competencies": [
    { "name": "Resolução de Conflitos", "score": 82 }
  ],
  "resultUrl": "https://praxis.iforce.com.br/results/att_8f2c1a"
}`;

const SIGNATURE_EXAMPLE = `POST /webhooks/praxis HTTP/1.1
Content-Type: application/json
X-Praxis-Signature: sha256=4f6b1a2c...`;

const NODE_VERIFY_EXAMPLE = `const crypto = require("crypto");

function verifyPraxisSignature(rawBody, signatureHeader, secret) {
  // rawBody: corpo da requisição EXATAMENTE como recebido (bytes crus)
  const expected =
    "sha256=" +
    crypto.createHmac("sha256", secret).update(rawBody, "utf8").digest("hex");

  return (
    signatureHeader != null &&
    expected.length === signatureHeader.length &&
    crypto.timingSafeEqual(Buffer.from(expected), Buffer.from(signatureHeader))
  );
}`;

function CustomApiDocsPage() {
  const swaggerUrl = `${getApiBaseUrl()}/swagger-ui/index.html`;

  return (
    <div className="min-h-screen bg-background text-foreground">
      <header className="border-b border-border bg-card">
        <div className="mx-auto flex max-w-4xl flex-wrap items-center justify-between gap-3 px-4 py-4">
          <div className="flex items-center gap-2">
            <BookOpen className="h-5 w-5 text-primary" />
            <span className="text-sm font-semibold uppercase tracking-wide text-primary">
              Práxis · Documentação
            </span>
          </div>
          <Link
            to="/integrations"
            className="inline-flex items-center gap-1.5 rounded-md border border-border bg-background px-3 py-1.5 text-sm font-medium hover:bg-accent"
          >
            <ArrowLeft className="h-4 w-4" />
            Central de integrações
          </Link>
        </div>
      </header>

      <main className="mx-auto max-w-4xl px-4 py-10">
        <h1 className="text-3xl font-semibold">Integração via API própria</h1>
        <p className="mt-3 max-w-3xl text-sm text-muted-foreground">
          Conecte qualquer sistema interno ou ATS ao Práxis sem depender de um provedor específico:
          seu sistema consulta a API do Práxis com um token de acesso e recebe os resultados das
          avaliações em um webhook assinado por HMAC.
        </p>

        <DocSection icon={ListChecks} title="Como funciona">
          <ol className="grid gap-3 text-sm">
            <FlowStep number={1}>
              Sua equipe configura a integração no painel do Práxis: gera o token de API e cadastra
              a URL do webhook do seu sistema.
            </FlowStep>
            <FlowStep number={2}>
              As pessoas participantes respondem às avaliações do Práxis (por exemplo, por link de
              convite) e o Práxis calcula o resultado.
            </FlowStep>
            <FlowStep number={3}>
              Quando o resultado fica pronto, o Práxis envia o evento{" "}
              <InlineCode>RESULT_READY</InlineCode> ao seu webhook, com a assinatura no cabeçalho{" "}
              <InlineCode>X-Praxis-Signature</InlineCode>. Seu sistema também pode consultar a API
              do Práxis usando o token de acesso.
            </FlowStep>
          </ol>
        </DocSection>

        <DocSection icon={ListChecks} title="Configuração no painel">
          <p>
            Tudo fica em um só lugar: a Central de{" "}
            <Link to="/integrations" className="font-medium text-primary hover:underline">
              Integrações
            </Link>
            , no card <strong>API própria</strong>. Sem contato com o suporte:
          </p>
          <ul className="mt-3 list-disc space-y-2 pl-5">
            <li>
              No card <strong>API própria</strong>, informe a URL base do seu sistema e gere o token
              de integração.
            </li>
            <li>
              Em <strong>Ver configuração</strong>, cadastre a URL do webhook, escolha os eventos,
              gere o segredo HMAC e dispare um evento de teste. O token de API pública também fica
              ali.
            </li>
          </ul>
          <p className="mt-3">
            Por segurança, token e segredo são exibidos <strong>uma única vez</strong> no momento da
            geração — o Práxis guarda apenas o hash. Se perder o valor, basta rotacionar para gerar
            um novo.
          </p>
        </DocSection>

        <DocSection icon={KeyRound} title="Autenticação (token de API)">
          <p>
            O token de API pública tem o prefixo <InlineCode>prx_live_</InlineCode> e identifica a
            sua empresa. Envie-o no cabeçalho <InlineCode>Authorization</InlineCode> em todas as
            chamadas do seu sistema à API do Práxis:
          </p>
          <CodeBlock>{TOKEN_CURL_EXAMPLE}</CodeBlock>
          <ul className="mt-3 list-disc space-y-2 pl-5">
            <li>
              Guarde o token em um cofre de segredos; nunca o exponha em código-fonte ou logs.
            </li>
            <li>
              Rotacione o token periodicamente (gerar um novo invalida o anterior de imediato) e
              revogue-o se houver suspeita de vazamento.
            </li>
            <li>
              A referência completa de endpoints disponíveis está no{" "}
              <a
                href={swaggerUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center gap-1 font-medium text-primary hover:underline"
              >
                Swagger da API
                <ExternalLink className="h-3.5 w-3.5" />
              </a>
              .
            </li>
          </ul>
        </DocSection>

        <DocSection icon={Webhook} title="Webhook de resultados">
          <p>
            O Práxis envia eventos por <InlineCode>POST</InlineCode> à URL cadastrada, com corpo
            JSON (<InlineCode>Content-Type: application/json</InlineCode>) e assinatura HMAC no
            cabeçalho:
          </p>
          <CodeBlock>{SIGNATURE_EXAMPLE}</CodeBlock>
          <div className="mt-4 overflow-x-auto">
            <table className="w-full min-w-[28rem] border-collapse text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs uppercase text-muted-foreground">
                  <th className="py-2 pr-4">Evento</th>
                  <th className="py-2">Quando é enviado</th>
                </tr>
              </thead>
              <tbody>
                <tr className="border-b border-border">
                  <td className="py-2 pr-4 align-top">
                    <InlineCode>RESULT_READY</InlineCode>
                  </td>
                  <td className="py-2">
                    Resultado da avaliação concluído e disponível. É o evento principal da
                    integração.
                  </td>
                </tr>
                <tr>
                  <td className="py-2 pr-4 align-top">
                    <InlineCode>ATTEMPT_STARTED</InlineCode>
                  </td>
                  <td className="py-2">
                    Participação iniciada. Disponível para assinatura no painel; a entrega ativa
                    hoje cobre <InlineCode>RESULT_READY</InlineCode>.
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <ul className="mt-4 list-disc space-y-2 pl-5">
            <li>
              Responda com um status <InlineCode>2xx</InlineCode> rapidamente (processe o evento de
              forma assíncrona se necessário). Qualquer outro status é registrado como falha de
              entrega e aparece no painel.
            </li>
            <li>
              Trate entregas repetidas de forma idempotente usando{" "}
              <InlineCode>attemptId</InlineCode> como chave.
            </li>
            <li>
              Eventos disparados pelo botão <em>Enviar evento de teste</em> incluem o campo{" "}
              <InlineCode>&quot;test&quot;: true</InlineCode>.
            </li>
          </ul>
        </DocSection>

        <DocSection icon={ShieldCheck} title="Validação da assinatura HMAC">
          <p>
            Cada entrega é assinada com <strong>HMAC-SHA256</strong> usando o segredo do webhook
            (prefixo <InlineCode>whsec_</InlineCode>). O valor do cabeçalho{" "}
            <InlineCode>X-Praxis-Signature</InlineCode> tem o formato{" "}
            <InlineCode>sha256=&lt;hex&gt;</InlineCode>, calculado sobre o corpo bruto da
            requisição. Recalcule o HMAC no seu servidor e rejeite a requisição se não coincidir:
          </p>
          <CodeBlock>{NODE_VERIFY_EXAMPLE}</CodeBlock>
          <ul className="mt-3 list-disc space-y-2 pl-5">
            <li>
              Valide sobre os <strong>bytes crus</strong> do corpo, antes de qualquer parse ou
              reserialização do JSON.
            </li>
            <li>Use comparação em tempo constante, como no exemplo acima.</li>
            <li>
              Ao rotacionar o segredo no painel, o anterior é invalidado imediatamente — atualize o
              seu sistema no mesmo momento.
            </li>
          </ul>
        </DocSection>

        <DocSection icon={Send} title={'Payload do evento "RESULT_READY"'}>
          <CodeBlock>{RESULT_READY_PAYLOAD}</CodeBlock>
          <div className="mt-4 overflow-x-auto">
            <table className="w-full min-w-[32rem] border-collapse text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs uppercase text-muted-foreground">
                  <th className="py-2 pr-4">Campo</th>
                  <th className="py-2">Descrição</th>
                </tr>
              </thead>
              <tbody>
                <PayloadField name="event">
                  Tipo do evento. Hoje, <InlineCode>RESULT_READY</InlineCode>.
                </PayloadField>
                <PayloadField name="tenantId">Identificador da sua empresa no Práxis.</PayloadField>
                <PayloadField name="attemptId">
                  Identificador único da participação avaliada. Use como chave de idempotência.
                </PayloadField>
                <PayloadField name="simulationId">
                  Identificador da avaliação (simulação) respondida.
                </PayloadField>
                <PayloadField name="score">Pontuação geral (0 a 100).</PayloadField>
                <PayloadField name="decision">
                  Indicação calculada: <InlineCode>RECOMMEND_INTERVIEW</InlineCode>,{" "}
                  <InlineCode>REVIEW_REQUIRED</InlineCode>, <InlineCode>IN_PROGRESS</InlineCode>,{" "}
                  <InlineCode>NO_RECOMMENDATION</InlineCode> ou <InlineCode>null</InlineCode>.
                </PayloadField>
                <PayloadField name="competencies">
                  Lista de competências avaliadas, cada uma com <InlineCode>name</InlineCode> e{" "}
                  <InlineCode>score</InlineCode>.
                </PayloadField>
                <PayloadField name="resultUrl">
                  Link direto para o resultado completo no Práxis.
                </PayloadField>
                <PayloadField name="test">
                  Presente com valor <InlineCode>true</InlineCode> apenas em eventos de teste.
                </PayloadField>
              </tbody>
            </table>
          </div>
        </DocSection>

        <DocSection icon={ListChecks} title="Endpoints de gerenciamento">
          <p>
            Os endpoints abaixo gerenciam a integração e exigem uma sessão autenticada no Práxis
            (são os mesmos usados pelas telas do painel):
          </p>
          <div className="mt-4 overflow-x-auto">
            <table className="w-full min-w-[36rem] border-collapse text-sm">
              <thead>
                <tr className="border-b border-border text-left text-xs uppercase text-muted-foreground">
                  <th className="py-2 pr-4">Método e caminho</th>
                  <th className="py-2">O que faz</th>
                </tr>
              </thead>
              <tbody>
                <EndpointRow method="POST" path="/api/v1/integrations/custom-api/api-token">
                  Gera (ou rotaciona) o token de API pública.
                </EndpointRow>
                <EndpointRow method="DELETE" path="/api/v1/integrations/custom-api/api-token">
                  Revoga o token de API pública.
                </EndpointRow>
                <EndpointRow method="GET" path="/api/v1/integrations/custom-api/webhook">
                  Lê a configuração atual do webhook.
                </EndpointRow>
                <EndpointRow method="POST" path="/api/v1/integrations/custom-api/webhook">
                  Configura URL e eventos do webhook.
                </EndpointRow>
                <EndpointRow
                  method="POST"
                  path="/api/v1/integrations/custom-api/webhook/secret/rotate"
                >
                  Rotaciona o segredo HMAC (o valor completo é exibido uma única vez).
                </EndpointRow>
                <EndpointRow method="POST" path="/api/v1/integrations/custom-api/webhook/test">
                  Envia um evento de teste assinado à URL configurada.
                </EndpointRow>
              </tbody>
            </table>
          </div>
        </DocSection>

        <DocSection icon={ShieldCheck} title="Boas práticas">
          <ul className="list-disc space-y-2 pl-5">
            <li>Valide a assinatura HMAC de todas as entregas antes de processá-las.</li>
            <li>
              Exponha o webhook apenas por HTTPS e restrinja o endpoint a receber somente{" "}
              <InlineCode>POST</InlineCode>.
            </li>
            <li>
              Monitore o painel de integrações: falhas de entrega ficam registradas com o último
              erro e a data da última atividade.
            </li>
            <li>
              Após configurar, use <em>Enviar evento de teste</em> para validar o fluxo de ponta a
              ponta antes de ativar em produção.
            </li>
          </ul>
        </DocSection>

        <footer className="mt-12 border-t border-border pt-6 text-sm text-muted-foreground">
          Dúvidas? Acesse a{" "}
          <Link to="/integrations" className="font-medium text-primary hover:underline">
            Central de integrações
          </Link>{" "}
          ou consulte a{" "}
          <a
            href={swaggerUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center gap-1 font-medium text-primary hover:underline"
          >
            referência completa da API
            <ExternalLink className="h-3.5 w-3.5" />
          </a>
          .
        </footer>
      </main>
    </div>
  );
}

function DocSection({
  icon: Icon,
  title,
  children,
}: {
  icon: typeof BookOpen;
  title: string;
  children: ReactNode;
}) {
  return (
    <section className="mt-8 rounded-md border border-border bg-card p-6">
      <h2 className="flex items-center gap-2 text-lg font-semibold">
        <Icon className="h-5 w-5 text-primary" />
        {title}
      </h2>
      <div className="mt-3 text-sm leading-relaxed text-foreground">{children}</div>
    </section>
  );
}

function FlowStep({ number, children }: { number: number; children: ReactNode }) {
  return (
    <li className="flex items-start gap-3">
      <span className="flex h-6 w-6 shrink-0 items-center justify-center rounded-full bg-primary/10 text-xs font-semibold text-primary">
        {number}
      </span>
      <span className="pt-0.5">{children}</span>
    </li>
  );
}

function InlineCode({ children }: { children: ReactNode }) {
  return (
    <code className="rounded bg-muted px-1.5 py-0.5 font-mono text-[0.8125rem]">{children}</code>
  );
}

function CodeBlock({ children }: { children: string }) {
  return (
    <pre className="mt-3 overflow-x-auto rounded-md border border-border bg-muted p-4 font-mono text-xs leading-relaxed">
      <code>{children}</code>
    </pre>
  );
}

function PayloadField({ name, children }: { name: string; children: ReactNode }) {
  return (
    <tr className="border-b border-border last:border-b-0">
      <td className="py-2 pr-4 align-top">
        <InlineCode>{name}</InlineCode>
      </td>
      <td className="py-2">{children}</td>
    </tr>
  );
}

function EndpointRow({
  method,
  path,
  children,
}: {
  method: string;
  path: string;
  children: ReactNode;
}) {
  return (
    <tr className="border-b border-border last:border-b-0">
      <td className="py-2 pr-4 align-top">
        <div className="flex flex-wrap items-center gap-2">
          <span className="rounded bg-primary/10 px-1.5 py-0.5 font-mono text-xs font-semibold text-primary">
            {method}
          </span>
          <code className="font-mono text-xs">{path}</code>
        </div>
      </td>
      <td className="py-2">{children}</td>
    </tr>
  );
}
