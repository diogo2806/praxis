import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Práxis: Avaliações por cenários estruturadas e rastreáveis" },
      {
        name: "description",
        content:
          "Crie avaliações por cenários, configure critérios e pesos, compartilhe por link e acompanhe respostas, indicadores e registros do percurso.",
      },
    ],
  }),
  component: LandingPage,
});

const landingStyles = `
  :root {
    --bg: #f8f6f0;
    --surface: #ffffff;
    --surface-alt: #f0f4f8;
    --ink: #172128;
    --muted: #5d6872;
    --line: #dde3ea;
    --primary: #1b6c8c;
    --primary-deep: #0e5570;
    --gold: #a87512;
    --success: #177245;
    --danger: #a83d33;
    --radius: 18px;
    font-family: Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  }

  * { box-sizing: border-box; }
  body { margin: 0; background: var(--bg); color: var(--ink); }
  a { color: inherit; text-decoration: none; }
  .page { min-height: 100vh; }
  .wrap { width: min(1120px, calc(100% - 32px)); margin: 0 auto; }
  .nav { position: sticky; top: 0; z-index: 10; border-bottom: 1px solid var(--line); background: rgba(248, 246, 240, 0.9); backdrop-filter: blur(14px); }
  .nav-inner { height: 72px; display: flex; align-items: center; justify-content: space-between; gap: 24px; }
  .brand { font-size: 1.35rem; font-weight: 750; letter-spacing: -0.03em; }
  .brand small { margin-left: 8px; color: var(--muted); font-size: 0.68rem; font-weight: 600; letter-spacing: 0.12em; text-transform: uppercase; }
  .nav-links { display: flex; align-items: center; gap: 16px; color: var(--muted); font-size: 0.94rem; }
  .nav-links a:hover { color: var(--primary); }
  .btn { display: inline-flex; align-items: center; justify-content: center; min-height: 44px; padding: 0 18px; border-radius: 999px; border: 1px solid var(--line); font-weight: 700; }
  .btn-primary { color: #fff; background: var(--primary); border-color: var(--primary); }
  .btn-primary:hover { background: var(--primary-deep); }
  .btn-ghost { background: var(--surface); color: var(--ink); }
  .hero { padding: 88px 0 72px; }
  .hero-grid { display: grid; grid-template-columns: 1.05fr 0.95fr; gap: 48px; align-items: center; }
  .eyebrow { color: var(--primary); font-weight: 800; letter-spacing: 0.14em; text-transform: uppercase; font-size: 0.78rem; }
  h1 { margin: 18px 0 0; font-size: clamp(2.4rem, 6vw, 4.7rem); line-height: 0.98; letter-spacing: -0.055em; }
  h2 { margin: 0; font-size: clamp(2rem, 4vw, 3.1rem); line-height: 1.04; letter-spacing: -0.04em; }
  h3 { margin: 0; font-size: 1.08rem; }
  p { margin: 0; }
  .lead { margin-top: 24px; color: var(--muted); font-size: clamp(1.06rem, 2vw, 1.25rem); line-height: 1.65; max-width: 58ch; }
  .hero-actions { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 32px; }
  .truth-note { margin-top: 28px; display: flex; gap: 10px; align-items: flex-start; color: var(--muted); font-size: 0.96rem; line-height: 1.55; }
  .truth-note span:first-child { width: 10px; height: 10px; margin-top: 7px; border-radius: 50%; background: var(--success); box-shadow: 0 0 0 5px rgba(23, 114, 69, 0.12); flex: none; }
  .scenario { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); box-shadow: 0 30px 80px rgba(20, 34, 48, 0.12); overflow: hidden; }
  .scenario-head { display: flex; justify-content: space-between; align-items: center; padding: 18px 20px; border-bottom: 1px solid var(--line); background: var(--surface-alt); }
  .scenario-head strong { display: block; }
  .scenario-head span { color: var(--muted); font-size: 0.86rem; }
  .timer { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; color: var(--gold); font-weight: 700; }
  .scenario-body { padding: 22px; }
  .tag { display: inline-flex; border-radius: 999px; background: rgba(168, 61, 51, 0.1); color: var(--danger); padding: 5px 10px; font-size: 0.74rem; font-weight: 800; letter-spacing: 0.08em; text-transform: uppercase; }
  .message { margin-top: 14px; font-size: 1.2rem; line-height: 1.45; }
  .options { display: grid; gap: 10px; margin-top: 22px; }
  .option { display: flex; gap: 10px; padding: 13px; border: 1px solid var(--line); border-radius: 12px; color: var(--muted); }
  .option b { display: grid; place-items: center; width: 28px; height: 28px; border-radius: 8px; background: var(--surface-alt); color: var(--primary); flex: none; }
  .section { padding: 76px 0; }
  .section-alt { background: var(--surface-alt); }
  .section-head { max-width: 760px; margin: 0 auto; text-align: center; }
  .section-head .lead { margin-left: auto; margin-right: auto; }
  .grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin-top: 34px; }
  .card { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); padding: 24px; }
  .card p { margin-top: 10px; color: var(--muted); line-height: 1.6; }
  .list { margin: 0; padding-left: 20px; color: var(--muted); line-height: 1.7; }
  .plans { display: grid; grid-template-columns: repeat(3, 1fr); gap: 18px; margin-top: 34px; }
  .plan { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); padding: 26px; display: flex; flex-direction: column; gap: 14px; }
  .plan.feature { border-color: var(--primary); box-shadow: 0 20px 60px rgba(27, 108, 140, 0.14); }
  .plan .label { color: var(--primary); font-size: 0.76rem; font-weight: 850; letter-spacing: 0.1em; text-transform: uppercase; }
  .price { font-size: 1.7rem; font-weight: 800; letter-spacing: -0.03em; }
  .muted { color: var(--muted); line-height: 1.6; }
  .notice { margin-top: 28px; background: rgba(168, 117, 18, 0.1); border: 1px solid rgba(168, 117, 18, 0.25); color: #6e4b0a; border-radius: var(--radius); padding: 18px 20px; line-height: 1.6; }
  .faq { max-width: 820px; margin: 34px auto 0; display: grid; gap: 14px; }
  .qa { background: var(--surface); border: 1px solid var(--line); border-radius: var(--radius); padding: 22px; }
  .qa p { margin-top: 8px; color: var(--muted); line-height: 1.6; }
  .final { text-align: center; padding: 84px 0; }
  .final .lead { margin-left: auto; margin-right: auto; }
  .final-actions { display: flex; justify-content: center; gap: 12px; margin-top: 28px; flex-wrap: wrap; }
  footer { padding: 36px 0; border-top: 1px solid var(--line); color: var(--muted); }

  @media (max-width: 880px) {
    .hero-grid, .grid-3, .plans { grid-template-columns: 1fr; }
    .nav-links { display: none; }
    .hero { padding-top: 56px; }
  }
`;

function LandingPage() {
  return (
    <div className="page">
      <style>{landingStyles}</style>
      <header className="nav">
        <div className="wrap nav-inner">
          <a href="#topo" className="brand">Práxis<small>by iForce</small></a>
          <nav className="nav-links" aria-label="Seções">
            <a href="#como">Como funciona</a>
            <a href="#recursos">Recursos</a>
            <a href="#integracoes">Integrações</a>
            <a href="#planos">Planos</a>
          </nav>
          <a className="btn btn-primary" href="#cta">Solicitar demonstração</a>
        </div>
      </header>

      <main id="topo">
        <section className="hero">
          <div className="wrap hero-grid">
            <div>
              <span className="eyebrow">Avaliações situacionais rastreáveis</span>
              <h1>Transforme situações do dia a dia em avaliações estruturadas.</h1>
              <p className="lead">
                Crie cenários, defina critérios e pesos, compartilhe a avaliação por link e acompanhe respostas, indicadores por competência e registros do percurso.
              </p>
              <p className="lead">
                A Práxis organiza evidências para apoiar análise humana. A interpretação e a decisão final permanecem com a equipe responsável.
              </p>
              <div className="hero-actions">
                <a className="btn btn-primary" href="#cta">Solicitar demonstração</a>
                <a className="btn btn-ghost" href="/comecar">Entrar</a>
              </div>
              <p className="truth-note"><span /> <span>Os recursos, integrações e condições comerciais dependem do plano contratado, da configuração da organização e da disponibilidade do conector correspondente.</span></p>
            </div>

            <aside className="scenario" aria-label="Exemplo demonstrativo de cenário">
              <div className="scenario-head">
                <div><strong>Pessoa participante</strong><span>Cenário 1/3 · abertura</span></div>
                <div className="timer">00:24</div>
              </div>
              <div className="scenario-body">
                <span className="tag">Exemplo demonstrativo</span>
                <p className="message">“Já é a terceira vez que abro chamado e ninguém resolve. Preciso disso hoje, ou vou escalar para cima.”</p>
                <div className="options">
                  <div className="option"><b>A</b><span>Pedir desculpas e prometer retorno sem confirmar com o time.</span></div>
                  <div className="option"><b>B</b><span>Reconhecer a frustração, confirmar o chamado e dar prazo realista alinhado ao time.</span></div>
                  <div className="option"><b>C</b><span>Explicar a política interna e pedir paciência até o próximo ciclo.</span></div>
                </div>
              </div>
            </aside>
          </div>
        </section>

        <section className="section section-alt" id="como">
          <div className="wrap">
            <div className="section-head">
              <span className="eyebrow">Como funciona</span>
              <h2>Da criação ao resultado, com critérios definidos pela equipe.</h2>
              <p className="lead">O fluxo principal permite estruturar a avaliação, configurar critérios, publicar por link e consultar respostas, percurso, indicadores e registros.</p>
            </div>
            <div className="grid-3">
              <div className="card"><h3>1. Estruture a avaliação</h3><p>Defina objetivo, cenários, alternativas, caminhos e tempos quando aplicável.</p></div>
              <div className="card"><h3>2. Configure os critérios</h3><p>Associe competências, pesos, pontuações e respostas que exigem revisão humana.</p></div>
              <div className="card"><h3>3. Compartilhe e analise</h3><p>Envie por link, acompanhe a participação e consulte indicadores e evidências registradas.</p></div>
            </div>
          </div>
        </section>

        <section className="section" id="recursos">
          <div className="wrap">
            <div className="section-head">
              <span className="eyebrow">Recursos</span>
              <h2>O que a plataforma entrega no fluxo atual.</h2>
            </div>
            <div className="grid-3">
              <div className="card"><h3>Pontuação por regras</h3><p>Os critérios, pesos e cálculos são configurados antes da publicação da avaliação.</p></div>
              <div className="card"><h3>Registros do percurso</h3><p>Respostas, etapas, pontos e eventos podem ser consultados por usuários autorizados.</p></div>
              <div className="card"><h3>Revisão humana</h3><p>Respostas críticas podem exigir análise da equipe. A plataforma não toma a decisão final automaticamente.</p></div>
              <div className="card"><h3>Versionamento</h3><p>Uma versão publicada pode ser preservada enquanto a equipe prepara uma nova versão.</p></div>
              <div className="card"><h3>Operação por link</h3><p>A avaliação pode ser aplicada diretamente pela Práxis, sem depender de outro sistema.</p></div>
              <div className="card"><h3>Governança</h3><p>Trilha cronológica, retenção, auditoria e controles de acesso apoiam uma operação responsável.</p></div>
            </div>
          </div>
        </section>

        <section className="section section-alt" id="integracoes">
          <div className="wrap">
            <div className="section-head">
              <span className="eyebrow">Integrações</span>
              <h2>Integre apenas quando houver conector compatível e configurado.</h2>
              <p className="lead">A Práxis funciona por links diretos. Integrações, como Gupy, Recrutei ou API própria, dependem da disponibilidade do conector, credenciais válidas e configuração da organização.</p>
            </div>
          </div>
        </section>

        <section className="section" id="planos">
          <div className="wrap">
            <div className="section-head">
              <span className="eyebrow">Planos</span>
              <h2>Contratação alinhada ao que estiver disponível no módulo de cobrança.</h2>
              <p className="lead">A gestão comercial real acontece na área de planos, pagamentos e créditos. Por isso, esta página não promete valores, descontos anuais ou condições que não estejam disponíveis para contratação no painel.</p>
            </div>
            <div className="plans">
              <div className="plan"><span className="label">Avulso</span><div className="price">Créditos</div><p className="muted">Modelo para uso pontual, conforme pacotes disponíveis no painel de cobrança.</p></div>
              <div className="plan feature"><span className="label">Profissional</span><div className="price">Assinatura mensal</div><p className="muted">Modelo para volume recorrente, com gerenciamento e cobrança no módulo financeiro.</p></div>
              <div className="plan"><span className="label">Enterprise</span><div className="price">Sob consulta</div><p className="muted">Condições comerciais, integrações e suporte específico dependem de negociação e escopo.</p></div>
            </div>
            <p className="notice">Valores, limites, pacotes e disponibilidade devem ser confirmados no painel de cobrança ou em proposta comercial vigente. Esta página evita prometer preço fixo ou desconto anual sem suporte operacional correspondente.</p>
          </div>
        </section>

        <section className="section section-alt" id="faq">
          <div className="wrap">
            <div className="section-head"><span className="eyebrow">FAQ</span><h2>Perguntas frequentes</h2></div>
            <div className="faq">
              <div className="qa"><h3>A Práxis usa IA generativa para avaliar pessoas?</h3><p>Não. A pontuação é calculada a partir de critérios, pesos e regras configurados previamente pela equipe responsável.</p></div>
              <div className="qa"><h3>Preciso integrar a Práxis a outro sistema?</h3><p>Não. A operação pode ser realizada por links diretos. Integrações são opcionais e dependem de conector compatível e configurado.</p></div>
              <div className="qa"><h3>A Práxis toma a decisão final?</h3><p>Não. A plataforma organiza critérios, pontuações e registros para apoiar a análise. A decisão final permanece com a equipe responsável.</p></div>
              <div className="qa"><h3>Como funcionam os preços?</h3><p>Os preços e condições válidos são os disponíveis no módulo de cobrança ou em proposta comercial vigente.</p></div>
            </div>
          </div>
        </section>

        <section className="final" id="cta">
          <div className="wrap">
            <span className="eyebrow">Vamos conversar</span>
            <h2>Estruture cenários, critérios e evidências em um só fluxo.</h2>
            <p className="lead">Conheça a criação de avaliações, a participação por link e a análise dos resultados em uma demonstração da Práxis.</p>
            <div className="final-actions">
              <a className="btn btn-primary" href="mailto:contato@iforce.com.br?subject=Demonstra%C3%A7%C3%A3o%20da%20Pr%C3%A1xis">Solicitar demonstração</a>
              <a className="btn btn-ghost" href="/comecar">Entrar</a>
            </div>
          </div>
        </section>
      </main>

      <footer>
        <div className="wrap">© 2026 iForce · Práxis</div>
      </footer>
    </div>
  );
}
