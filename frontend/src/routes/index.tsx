import { useEffect } from "react";
import { createFileRoute } from "@tanstack/react-router";

export const Route = createFileRoute("/")({
  head: () => ({
    meta: [
      { title: "Práxis — Avaliação comportamental situacional" },
      {
        name: "description",
        content:
          "Avalie pessoas por como decidem em situações reais. Pontuação determinística, trilha auditável e integrações com ATS, sistemas hospitalares e plataformas educacionais. Sem IA julgando ninguém.",
      },
    ],
    links: [
      { rel: "preconnect", href: "https://fonts.googleapis.com" },
      { rel: "preconnect", href: "https://fonts.gstatic.com", crossOrigin: "anonymous" },
      {
        rel: "stylesheet",
        href: "https://fonts.googleapis.com/css2?family=Fraunces:opsz,wght@9..144,380;9..144,460;9..144,560;9..144,640&family=IBM+Plex+Mono:wght@400;500;600&family=IBM+Plex+Sans:wght@400;450;500;600;700&display=swap",
      },
    ],
  }),
  component: LandingPage,
});

const landingStyles = `
  :root{
    --bg: oklch(0.985 0.006 85);
    --bg-alt: oklch(0.965 0.009 235);
    --surface: oklch(1 0 0);
    --ink: oklch(0.24 0.02 240);
    --muted: oklch(0.49 0.018 240);
    --faint: oklch(0.62 0.015 240);
    --line: oklch(0.9 0.01 240);
    --line-soft: oklch(0.93 0.008 240);
    --primary: oklch(0.5 0.1 233);
    --primary-deep: oklch(0.4 0.09 238);
    --navy: oklch(0.19 0.035 245);
    --navy-2: oklch(0.24 0.04 245);
    --on-navy: oklch(0.95 0.012 235);
    --on-navy-mut: oklch(0.74 0.02 235);
    --gold: oklch(0.76 0.13 80);
    --gold-deep: oklch(0.62 0.12 76);
    --success: oklch(0.6 0.13 150);
    --danger: oklch(0.58 0.18 28);

    --r-sm: 0.5rem;
    --r: 0.75rem;
    --r-lg: 1.1rem;
    --r-pill: 999px;

    --font-display: 'Fraunces', Georgia, 'Times New Roman', serif;
    --font-sans: 'IBM Plex Sans', system-ui, -apple-system, 'Segoe UI', sans-serif;
    --font-mono: 'IBM Plex Mono', ui-monospace, 'SFMono-Regular', monospace;

    --maxw: 1140px;
    --pad-x: clamp(1.25rem, 4vw, 2rem);
    --sec-y: clamp(4.5rem, 9vw, 7.5rem);

    --shadow-sm: 0 1px 2px oklch(0.4 0.05 245 / 0.06), 0 1px 1px oklch(0.4 0.05 245 / 0.04);
    --shadow: 0 18px 40px -24px oklch(0.35 0.06 245 / 0.30), 0 4px 12px -6px oklch(0.35 0.06 245 / 0.12);
    --shadow-lg: 0 40px 80px -40px oklch(0.30 0.06 245 / 0.40);
  }

  *{box-sizing:border-box}
  html{scroll-behavior:smooth}
  body{
    margin:0;
    font-family:var(--font-sans);
    color:var(--ink);
    background:var(--bg);
    line-height:1.6;
    font-size:17px;
    -webkit-font-smoothing:antialiased;
    text-rendering:optimizeLegibility;
  }
  @media (max-width:560px){ body{font-size:16px} }

  h1,h2,h3,h4{font-family:var(--font-display);font-weight:560;line-height:1.08;letter-spacing:-0.01em;margin:0;color:var(--ink)}
  h1{font-size:clamp(2.4rem,5.4vw,4rem);font-weight:460}
  h2{font-size:clamp(1.9rem,3.8vw,2.9rem);font-weight:460}
  h3{font-size:1.18rem;letter-spacing:0}
  p{margin:0}
  a{color:inherit;text-decoration:none}

  .wrap{max-width:var(--maxw);margin-inline:auto;padding-inline:var(--pad-x)}
  .eyebrow{
    font-family:var(--font-mono);font-size:0.72rem;font-weight:500;letter-spacing:0.16em;
    text-transform:uppercase;color:var(--primary);display:inline-flex;align-items:center;gap:0.55rem;
  }
  .eyebrow::before{content:"";width:1.4rem;height:1px;background:var(--gold-deep)}
  .lead{font-size:clamp(1.05rem,1.6vw,1.22rem);color:var(--muted);max-width:46ch}

  /* buttons */
  .btn{
    display:inline-flex;align-items:center;justify-content:center;gap:0.5rem;
    font-family:var(--font-sans);font-weight:600;font-size:0.98rem;
    padding:0.82rem 1.4rem;border-radius:var(--r-pill);cursor:pointer;
    border:1px solid transparent;transition:transform .15s ease, box-shadow .2s ease, background .2s ease, border-color .2s ease;
    min-height:2.95rem;white-space:nowrap;
  }
  .btn:active{transform:translateY(1px)}
  .btn-primary{background:var(--primary);color:white;box-shadow:0 8px 20px -10px oklch(0.5 0.1 233 / 0.7)}
  .btn-primary:hover{background:var(--primary-deep);box-shadow:0 12px 26px -10px oklch(0.5 0.1 233 / 0.8)}
  .btn-ghost{background:transparent;color:var(--ink);border-color:var(--line)}
  .btn-ghost:hover{border-color:var(--primary);color:var(--primary);background:oklch(0.5 0.1 233 / 0.05)}
  .btn-gold{background:var(--ink);color:white}
  .btn-gold:hover{background:oklch(0.18 0.02 240)}
  .btn-on-navy{background:var(--on-navy);color:var(--navy)}
  .btn-on-navy:hover{background:white}
  .btn-arrow{transition:transform .2s ease}
  .btn:hover .btn-arrow{transform:translateX(3px)}

  /* nav */
  header.nav{position:sticky;top:0;z-index:50;background:oklch(0.985 0.006 85 / 0.82);backdrop-filter:saturate(140%) blur(12px);border-bottom:1px solid transparent;transition:border-color .3s, box-shadow .3s}
  header.nav.scrolled{border-color:var(--line-soft);box-shadow:var(--shadow-sm)}
  .nav-inner{display:flex;align-items:center;justify-content:space-between;height:4.6rem;gap:1rem}
  .brand{display:flex;align-items:baseline;gap:0.5rem;font-family:var(--font-display);font-size:1.4rem;font-weight:560;letter-spacing:-0.01em}
  .brand .dot{width:0.42rem;height:0.42rem;border-radius:50%;background:var(--gold);display:inline-block;transform:translateY(-0.05rem)}
  .brand small{font-family:var(--font-mono);font-size:0.62rem;letter-spacing:0.14em;text-transform:uppercase;color:var(--faint);font-weight:500}
  .nav-links{display:flex;align-items:center;gap:0.35rem}
  .nav-links a.link{padding:0.5rem 0.8rem;border-radius:var(--r-sm);font-size:0.92rem;font-weight:500;color:var(--muted);transition:color .15s, background .15s}
  .nav-links a.link:hover{color:var(--ink);background:oklch(0.5 0.1 233 / 0.06)}
  .nav-cta{display:flex;align-items:center;gap:0.6rem}
  .nav-cta .link{font-size:0.92rem;font-weight:500;color:var(--muted);padding:0.4rem 0.4rem}
  .nav-cta .link:hover{color:var(--ink)}
  .menu-btn{display:none;background:none;border:1px solid var(--line);border-radius:var(--r-sm);width:2.7rem;height:2.7rem;cursor:pointer;align-items:center;justify-content:center}
  .menu-btn svg{width:1.3rem;height:1.3rem;stroke:var(--ink)}

  @media (max-width:920px){
    .nav-links{display:none}
    .nav-cta .link{display:none}
    .menu-btn{display:flex}
    .mobile-open .nav-links{
      display:flex;flex-direction:column;position:absolute;top:4.6rem;left:0;right:0;
      background:var(--surface);padding:0.8rem var(--pad-x) 1.4rem;border-bottom:1px solid var(--line);gap:0.1rem;box-shadow:var(--shadow)
    }
    .mobile-open .nav-links a.link{padding:0.85rem 0.6rem;font-size:1rem;border-bottom:1px solid var(--line-soft);border-radius:0}
  }

  /* hero */
  .hero{position:relative;overflow:hidden;padding-top:clamp(2.5rem,5vw,4.5rem);padding-bottom:var(--sec-y)}
  .hero::before{
    content:"";position:absolute;inset:0;z-index:0;pointer-events:none;
    background:
      radial-gradient(60% 50% at 78% 8%, oklch(0.76 0.13 80 / 0.10), transparent 70%),
      radial-gradient(70% 60% at 8% 0%, oklch(0.5 0.1 233 / 0.08), transparent 60%);
  }
  .hero-grid{position:relative;z-index:1;display:grid;grid-template-columns:1.05fr 0.95fr;gap:clamp(2rem,4vw,4rem);align-items:center}
  @media (max-width:940px){ .hero-grid{grid-template-columns:1fr;gap:2.75rem} }
  .hero-copy h1{margin:1.1rem 0 0}
  .hero-copy h1 .accent{font-style:italic;color:var(--primary)}
  .hero-copy .lead{margin-top:1.3rem;max-width:48ch}
  .hero-ctas{display:flex;gap:0.8rem;margin-top:2rem;flex-wrap:wrap}
  .integrations-line{display:flex;align-items:center;gap:0.6rem;margin-top:2rem;font-size:0.9rem;color:var(--faint)}
  .integrations-line .pip{width:0.5rem;height:0.5rem;border-radius:50%;background:var(--success);box-shadow:0 0 0 3px oklch(0.6 0.13 150 / 0.18)}
  .integrations-line b{color:var(--muted);font-weight:600}

  /* hero reveal animation */
  .reveal-up{opacity:0;transform:translateY(14px);animation:revealUp .7s cubic-bezier(.2,.7,.2,1) forwards}
  .d1{animation-delay:.05s}.d2{animation-delay:.15s}.d3{animation-delay:.27s}.d4{animation-delay:.4s}.d5{animation-delay:.52s}
  @keyframes revealUp{to{opacity:1;transform:none}}

  /* scenario card */
  .scenario{
    background:var(--surface);border:1px solid var(--line);border-radius:var(--r-lg);
    box-shadow:var(--shadow-lg);overflow:hidden;
  }
  .sc-top{display:flex;align-items:center;justify-content:space-between;padding:0.9rem 1.1rem;border-bottom:1px solid var(--line-soft);background:linear-gradient(var(--bg-alt),transparent)}
  .sc-id{display:flex;align-items:center;gap:0.65rem}
  .avatar{width:2.2rem;height:2.2rem;border-radius:50%;background:linear-gradient(135deg,var(--primary),var(--primary-deep));color:white;display:grid;place-items:center;font-weight:600;font-size:0.8rem;font-family:var(--font-mono)}
  .sc-id .who{font-weight:600;font-size:0.92rem;line-height:1.1}
  .sc-id .stage{font-family:var(--font-mono);font-size:0.68rem;color:var(--faint);letter-spacing:0.04em}
  .sc-timer{font-family:var(--font-mono);font-weight:500;font-size:0.95rem;color:var(--muted);display:flex;align-items:center;gap:0.4rem}
  .sc-timer .tdot{width:0.45rem;height:0.45rem;border-radius:50%;background:var(--gold);animation:pulse 1.6s ease-in-out infinite}
  @keyframes pulse{50%{opacity:0.35}}
  .sc-body{padding:1.15rem 1.2rem 1.3rem}
  .sc-tag{font-family:var(--font-mono);font-size:0.66rem;letter-spacing:0.12em;text-transform:uppercase;color:var(--danger);font-weight:500}
  .sc-msg{font-family:var(--font-display);font-size:1.18rem;line-height:1.35;margin:0.55rem 0 1.1rem;color:var(--ink)}
  .sc-opts{display:flex;flex-direction:column;gap:0.55rem}
  .opt{display:flex;gap:0.7rem;align-items:flex-start;text-align:left;width:100%;background:var(--surface);border:1px solid var(--line);border-radius:var(--r);padding:0.7rem 0.8rem;cursor:pointer;font:inherit;font-size:0.9rem;color:var(--ink);transition:border-color .15s, background .15s, transform .1s;line-height:1.35}
  .opt:hover{border-color:var(--primary);background:oklch(0.5 0.1 233 / 0.04)}
  .opt .key{flex:none;width:1.5rem;height:1.5rem;border-radius:0.4rem;border:1px solid var(--line);display:grid;place-items:center;font-family:var(--font-mono);font-size:0.78rem;font-weight:600;color:var(--muted);transition:.15s}
  .opt:hover .key{border-color:var(--primary);color:var(--primary)}
  .opt.picked{border-color:var(--primary);background:oklch(0.5 0.1 233 / 0.06)}
  .opt.picked .key{background:var(--primary);color:white;border-color:var(--primary)}
  .sc-note{margin-top:1rem;font-size:0.82rem;color:var(--faint);display:flex;gap:0.55rem;align-items:flex-start;min-height:1.2rem;transition:color .2s}
  .sc-note svg{flex:none;width:1rem;height:1rem;margin-top:0.15rem;stroke:var(--gold-deep)}
  .sc-note.resolved{color:var(--muted)}

  /* generic section */
  section{position:relative}
  .sec{padding-block:var(--sec-y)}
  .sec-alt{background:var(--bg-alt)}
  .sec-head{max-width:62ch}
  .sec-head h2{margin-top:0.9rem}
  .sec-head .lead{margin-top:1rem}

  /* problem split */
  .split{display:grid;grid-template-columns:1fr 1fr;gap:1.1rem;margin-top:2.8rem}
  @media (max-width:780px){ .split{grid-template-columns:1fr} }
  .col{border-radius:var(--r-lg);padding:1.6rem 1.6rem 1.7rem;border:1px solid var(--line)}
  .col.bad{background:var(--surface)}
  .col.good{background:linear-gradient(180deg, oklch(0.5 0.1 233 / 0.05), var(--surface));border-color:oklch(0.5 0.1 233 / 0.25)}
  .col h3{display:flex;align-items:center;gap:0.6rem;font-family:var(--font-sans);font-weight:700;font-size:1rem;letter-spacing:0;margin-bottom:1.1rem}
  .col .badge{font-family:var(--font-mono);font-size:0.62rem;letter-spacing:0.1em;text-transform:uppercase;padding:0.25rem 0.55rem;border-radius:var(--r-pill);font-weight:500}
  .col.bad .badge{background:oklch(0.58 0.18 28 / 0.1);color:var(--danger)}
  .col.good .badge{background:oklch(0.6 0.13 150 / 0.14);color:var(--success)}
  .clist{list-style:none;margin:0;padding:0;display:flex;flex-direction:column;gap:0.85rem}
  .clist li{display:flex;gap:0.7rem;align-items:flex-start;font-size:0.96rem;color:var(--muted)}
  .clist li svg{flex:none;width:1.15rem;height:1.15rem;margin-top:0.18rem}
  .col.bad .clist svg{stroke:var(--danger)}
  .col.good .clist svg{stroke:var(--success)}
  .col.good .clist li{color:var(--ink)}

  /* steps */
  .steps{display:grid;grid-template-columns:repeat(3,1fr);gap:1.1rem;margin-top:2.8rem}
  @media (max-width:780px){ .steps{grid-template-columns:1fr} }
  .step{position:relative;background:var(--surface);border:1px solid var(--line);border-radius:var(--r-lg);padding:1.7rem 1.5rem}
  .step .num{font-family:var(--font-mono);font-size:0.8rem;font-weight:600;color:var(--gold-deep);letter-spacing:0.1em}
  .step h3{margin:0.9rem 0 0.5rem;font-size:1.12rem}
  .step p{font-size:0.95rem;color:var(--muted)}
  .step .rule{position:absolute;top:1.9rem;right:-0.55rem;width:1.1rem;height:1px;background:var(--line);display:none}
  @media(min-width:781px){ .step:not(:last-child) .rule{display:block} }

  /* feature + signature scorecard */
  .feat-grid{display:grid;grid-template-columns:1fr 1fr;gap:clamp(2rem,4vw,3.4rem);margin-top:2.8rem;align-items:start}
  @media (max-width:900px){ .feat-grid{grid-template-columns:1fr} }
  .feats{display:grid;grid-template-columns:1fr 1fr;gap:1.4rem 1.6rem}
  @media (max-width:520px){ .feats{grid-template-columns:1fr} }
  .feat h3{font-family:var(--font-sans);font-weight:700;font-size:1rem;letter-spacing:0;display:flex;gap:0.55rem;align-items:center;margin-bottom:0.4rem}
  .feat .ico{flex:none;width:1.9rem;height:1.9rem;border-radius:0.55rem;display:grid;place-items:center;background:oklch(0.5 0.1 233 / 0.09)}
  .feat .ico svg{width:1.05rem;height:1.05rem;stroke:var(--primary)}
  .feat p{font-size:0.92rem;color:var(--muted)}

  /* the signature evidence card */
  .evidence{position:sticky;top:6rem;background:var(--surface);border:1px solid var(--line);border-radius:var(--r-lg);box-shadow:var(--shadow);overflow:hidden}
  .ev-top{padding:1.05rem 1.2rem;border-bottom:1px solid var(--line-soft);display:flex;align-items:center;justify-content:space-between;background:linear-gradient(var(--bg-alt),transparent)}
  .ev-top .t{font-family:var(--font-mono);font-size:0.68rem;letter-spacing:0.12em;text-transform:uppercase;color:var(--faint)}
  .ev-seal{display:inline-flex;align-items:center;gap:0.4rem;font-family:var(--font-mono);font-size:0.68rem;font-weight:500;color:var(--gold-deep);border:1px solid oklch(0.76 0.13 80 / 0.4);background:oklch(0.76 0.13 80 / 0.1);padding:0.28rem 0.6rem;border-radius:var(--r-pill)}
  .ev-seal svg{width:0.85rem;height:0.85rem;stroke:var(--gold-deep)}
  .ev-body{padding:1.3rem 1.3rem 1.4rem}
  .ev-score{display:flex;align-items:baseline;gap:0.7rem}
  .ev-score .n{font-family:var(--font-mono);font-weight:600;font-size:3.2rem;line-height:1;color:var(--ink);letter-spacing:-0.02em}
  .ev-score .of{font-family:var(--font-mono);font-size:1rem;color:var(--faint)}
  .ev-score .decision{margin-left:auto;font-size:0.78rem;font-weight:600;padding:0.32rem 0.7rem;border-radius:var(--r-pill);background:oklch(0.6 0.13 150 / 0.14);color:var(--success)}
  .ev-comp{margin-top:1.4rem;display:flex;flex-direction:column;gap:0.85rem}
  .cbar .clabel{display:flex;justify-content:space-between;font-size:0.82rem;margin-bottom:0.32rem;color:var(--muted)}
  .cbar .clabel b{color:var(--ink);font-family:var(--font-mono);font-weight:500}
  .track{height:0.5rem;border-radius:var(--r-pill);background:oklch(0.5 0.06 240 / 0.1);overflow:hidden}
  .fill{height:100%;border-radius:var(--r-pill);background:linear-gradient(90deg,var(--primary),oklch(0.62 0.12 215));width:0;transition:width 1.1s cubic-bezier(.2,.7,.2,1)}
  .ev-trail{margin-top:1.4rem;border-top:1px dashed var(--line);padding-top:1rem}
  .ev-trail .lab{font-family:var(--font-mono);font-size:0.64rem;letter-spacing:0.12em;text-transform:uppercase;color:var(--faint);margin-bottom:0.55rem}
  .ev-trail code{display:block;font-family:var(--font-mono);font-size:0.74rem;line-height:1.7;color:var(--muted);white-space:pre-wrap}
  .ev-trail code .ok{color:var(--primary);font-weight:500}
  .ev-trail code .gold{color:var(--gold-deep)}

  /* use-cases grid */
  .use-cases{display:grid;grid-template-columns:repeat(3,1fr);gap:1.1rem;margin-top:2.8rem}
  @media (max-width:880px){ .use-cases{grid-template-columns:1fr 1fr;gap:1rem} }
  @media (max-width:560px){ .use-cases{grid-template-columns:1fr} }
  .uc{background:var(--surface);border:1px solid var(--line);border-radius:var(--r-lg);padding:1.5rem 1.4rem;transition:transform .15s, box-shadow .15s}
  .uc:hover{transform:translateY(-2px);box-shadow:var(--shadow)}
  .uc .uc-ico{width:2.4rem;height:2.4rem;border-radius:0.7rem;display:grid;place-items:center;margin-bottom:1rem}
  .uc .uc-ico svg{width:1.3rem;height:1.3rem}
  .uc h3{font-family:var(--font-sans);font-weight:700;font-size:1rem;letter-spacing:0;margin-bottom:0.4rem}
  .uc p{font-size:0.9rem;color:var(--muted);line-height:1.5}
  .uc .uc-examples{margin-top:0.7rem;font-size:0.82rem;color:var(--faint);font-style:italic}
  .uc-recruit .uc-ico{background:oklch(0.5 0.1 233 / 0.1)}
  .uc-recruit .uc-ico svg{stroke:var(--primary)}
  .uc-health .uc-ico{background:oklch(0.6 0.13 150 / 0.12)}
  .uc-health .uc-ico svg{stroke:var(--success)}
  .uc-edu .uc-ico{background:oklch(0.76 0.13 80 / 0.15)}
  .uc-edu .uc-ico svg{stroke:var(--gold-deep)}
  .uc-corp .uc-ico{background:oklch(0.5 0.1 233 / 0.08)}
  .uc-corp .uc-ico svg{stroke:var(--primary-deep)}
  .uc-compliance .uc-ico{background:oklch(0.58 0.18 28 / 0.1)}
  .uc-compliance .uc-ico svg{stroke:var(--danger)}
  .uc-public .uc-ico{background:oklch(0.49 0.018 240 / 0.1)}
  .uc-public .uc-ico svg{stroke:var(--muted)}

  /* integrations band */
  .integ-band{display:grid;grid-template-columns:1fr 0.85fr;gap:clamp(2rem,5vw,4rem);align-items:center}
  @media (max-width:880px){ .integ-band{grid-template-columns:1fr;gap:2.4rem} }
  .flow{display:flex;flex-direction:column;gap:0.7rem}
  .flow .fstep{display:flex;align-items:center;gap:0.9rem;background:var(--surface);border:1px solid var(--line);border-radius:var(--r);padding:0.85rem 1rem;font-size:0.95rem}
  .flow .fstep .fn{flex:none;width:1.7rem;height:1.7rem;border-radius:50%;background:var(--ink);color:white;display:grid;place-items:center;font-family:var(--font-mono);font-size:0.78rem;font-weight:600}
  .flow .fstep.gp .fn{background:var(--gold-deep)}
  .connect{background:var(--surface);border:1px solid var(--line);border-radius:var(--r-lg);padding:1.6rem 1.5rem 1.3rem;box-shadow:var(--shadow)}
  .connect .cstep{display:flex;gap:0.9rem;align-items:flex-start}
  .connect .ci{flex:none;width:2.4rem;height:2.4rem;border-radius:0.7rem;display:grid;place-items:center;font-family:var(--font-display);font-weight:600;font-size:1.05rem}
  .connect .ci svg{width:1.2rem;height:1.2rem;stroke:#3a2a05}
  .ci-ats{background:oklch(0.6 0.13 150 / 0.14);color:var(--success)}
  .ci-prx{background:oklch(0.5 0.1 233 / 0.1);color:var(--primary)}
  .ci-ok{background:var(--gold)}
  .connect .cstep b{display:block;font-size:0.98rem;color:var(--ink)}
  .connect .cstep > div span{font-size:0.86rem;color:var(--muted);display:block;margin-top:0.12rem}
  .connect .cdown{height:1rem;margin:0.4rem 0 0.4rem 1.15rem;border-left:2px dashed var(--line)}
  .connect .cnote{margin-top:1.2rem;padding-top:1rem;border-top:1px solid var(--line-soft);font-size:0.84rem;color:var(--muted);display:flex;align-items:center;gap:0.5rem}
  .connect .cnote .pip{flex:none;width:0.5rem;height:0.5rem;border-radius:50%;background:var(--success);box-shadow:0 0 0 3px oklch(0.6 0.13 150 / 0.18)}

  /* governance — dark band */
  .dark-band{background:var(--navy);color:var(--on-navy);position:relative;overflow:hidden}
  .dark-band::before{content:"";position:absolute;inset:0;background:radial-gradient(50% 40% at 85% 0%, oklch(0.76 0.13 80 / 0.10), transparent 70%);pointer-events:none}
  .dark-band h2,.dark-band h3{color:var(--on-navy)}
  .dark-band .eyebrow{color:var(--gold)}
  .dark-band .eyebrow::before{background:var(--gold)}
  .dark-band .lead{color:var(--on-navy-mut)}
  .gov-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:1.1rem;margin-top:2.8rem}
  @media (max-width:820px){ .gov-grid{grid-template-columns:1fr} }
  .gov{border:1px solid oklch(1 0 0 / 0.12);border-radius:var(--r-lg);padding:1.5rem 1.4rem;background:oklch(1 0 0 / 0.03)}
  .gov .ico{width:2.1rem;height:2.1rem;border-radius:0.55rem;display:grid;place-items:center;background:oklch(0.76 0.13 80 / 0.15);margin-bottom:1rem}
  .gov .ico svg{width:1.15rem;height:1.15rem;stroke:var(--gold)}
  .gov h3{font-family:var(--font-sans);font-weight:700;font-size:1.02rem;letter-spacing:0;margin-bottom:0.45rem}
  .gov p{font-size:0.92rem;color:var(--on-navy-mut)}
  .gov-foot{margin-top:2.4rem;display:flex;flex-wrap:wrap;gap:0.6rem;align-items:center}
  .chip{font-family:var(--font-mono);font-size:0.72rem;letter-spacing:0.06em;color:var(--on-navy-mut);border:1px solid oklch(1 0 0 / 0.16);border-radius:var(--r-pill);padding:0.4rem 0.85rem}
  .chip b{color:var(--on-navy);font-weight:600}

  /* pricing */
  .roi{display:flex;gap:0.8rem;align-items:flex-start;max-width:60ch;margin-top:1.4rem;font-size:1.02rem;color:var(--muted)}
  .roi svg{flex:none;width:1.3rem;height:1.3rem;margin-top:0.25rem;stroke:var(--gold-deep)}
  .roi b{color:var(--ink);font-weight:600}
  .toggle{display:inline-flex;align-items:center;gap:0.2rem;margin-top:2rem;background:var(--surface);border:1px solid var(--line);border-radius:var(--r-pill);padding:0.28rem}
  .toggle button{font:inherit;font-size:0.86rem;font-weight:600;border:none;background:none;cursor:pointer;color:var(--muted);padding:0.5rem 1.05rem;border-radius:var(--r-pill);transition:.2s}
  .toggle button.on{background:var(--ink);color:white}
  .toggle .save{font-family:var(--font-mono);font-size:0.66rem;color:var(--gold-deep);font-weight:500;padding-right:0.6rem;letter-spacing:0.04em}

  .plans{display:grid;grid-template-columns:repeat(3,1fr);gap:1.2rem;margin-top:2.4rem;align-items:stretch}
  @media (max-width:920px){ .plans{grid-template-columns:1fr;max-width:460px} }
  .plan{display:flex;flex-direction:column;background:var(--surface);border:1px solid var(--line);border-radius:var(--r-lg);padding:1.8rem 1.6rem;position:relative;transition:transform .2s ease, box-shadow .2s ease}
  .plan:hover{transform:translateY(-3px);box-shadow:var(--shadow)}
  .plan.feature{border-color:var(--primary);box-shadow:var(--shadow);background:linear-gradient(180deg, oklch(0.5 0.1 233 / 0.04), var(--surface))}
  .plan .ptag{position:absolute;top:-0.8rem;left:1.6rem;font-family:var(--font-mono);font-size:0.66rem;letter-spacing:0.1em;text-transform:uppercase;font-weight:600;background:var(--primary);color:white;padding:0.32rem 0.7rem;border-radius:var(--r-pill)}
  .plan .pname{font-family:var(--font-display);font-size:1.4rem;font-weight:560}
  .plan .pfor{font-size:0.85rem;color:var(--faint);margin-top:0.25rem;min-height:2.4em}
  .price{margin:1.2rem 0 0;display:flex;align-items:baseline;gap:0.35rem;flex-wrap:wrap}
  .price .cur{font-family:var(--font-mono);font-size:1rem;color:var(--muted);font-weight:500}
  .price .amt{font-family:var(--font-mono);font-size:2.5rem;font-weight:600;line-height:1;color:var(--ink);letter-spacing:-0.02em}
  .price .per{font-size:0.85rem;color:var(--faint)}
  .price.consulta .amt{font-size:1.7rem}
  .psub{font-size:0.82rem;color:var(--muted);margin-top:0.55rem;min-height:1.4em}
  .plan .btn{margin-top:1.4rem;width:100%}
  .plist{list-style:none;margin:1.5rem 0 0;padding:1.4rem 0 0;border-top:1px solid var(--line-soft);display:flex;flex-direction:column;gap:0.7rem;flex:1}
  .plist li{display:flex;gap:0.6rem;align-items:flex-start;font-size:0.9rem;color:var(--muted)}
  .plist li svg{flex:none;width:1.05rem;height:1.05rem;margin-top:0.18rem;stroke:var(--primary)}
  .plist li.head{color:var(--ink);font-weight:600;font-size:0.86rem}
  .plist li.head svg{stroke:var(--gold-deep)}
  .pfoot{margin-top:2rem;font-size:0.86rem;color:var(--faint);display:flex;gap:0.55rem;align-items:flex-start;max-width:70ch}
  .pfoot svg{flex:none;width:1.05rem;height:1.05rem;margin-top:0.16rem;stroke:var(--success)}

  /* faq */
  .faq{margin-top:2.6rem;max-width:780px;border-top:1px solid var(--line)}
  .qa{border-bottom:1px solid var(--line)}
  .qa button{width:100%;text-align:left;background:none;border:none;cursor:pointer;font:inherit;padding:1.25rem 0.25rem;display:flex;justify-content:space-between;align-items:center;gap:1rem;color:var(--ink)}
  .qa button .q{font-family:var(--font-display);font-size:1.1rem;font-weight:460}
  .qa .ic{flex:none;width:1.5rem;height:1.5rem;position:relative;transition:transform .25s}
  .qa .ic::before,.qa .ic::after{content:"";position:absolute;background:var(--primary);border-radius:2px;top:50%;left:50%;transform:translate(-50%,-50%)}
  .qa .ic::before{width:0.95rem;height:2px}
  .qa .ic::after{width:2px;height:0.95rem;transition:transform .25s}
  .qa.open .ic::after{transform:translate(-50%,-50%) scaleY(0)}
  .qa .ans{max-height:0;overflow:hidden;transition:max-height .3s ease}
  .qa .ans p{padding:0 0.25rem 1.3rem;color:var(--muted);font-size:0.98rem;max-width:68ch}

  /* final cta */
  .final{text-align:center;padding-block:clamp(4.5rem,8vw,7rem)}
  .final h2{max-width:18ch;margin-inline:auto}
  .final .lead{margin:1.2rem auto 0;text-align:center}
  .final .hero-ctas{justify-content:center;margin-top:2.2rem}

  /* footer */
  footer{background:var(--navy);color:var(--on-navy-mut);padding-block:2.6rem}
  .foot-inner{display:flex;justify-content:space-between;align-items:center;gap:1rem;flex-wrap:wrap}
  footer .brand{color:var(--on-navy)}
  footer .brand small{color:oklch(0.6 0.02 245)}
  footer .fcopy{font-size:0.84rem;font-family:var(--font-mono);letter-spacing:0.02em}

  /* focus + motion */
  :focus-visible{outline:3px solid color-mix(in oklab, var(--primary) 65%, white);outline-offset:2px;border-radius:3px}
  @media (prefers-reduced-motion: reduce){
    *,*::before,*::after{animation-duration:.001ms!important;animation-iteration-count:1!important;transition-duration:.001ms!important;scroll-behavior:auto!important}
    .reveal-up{opacity:1;transform:none}
  }
  /* ===== scenario mini-report ===== */
  .sc-report{margin-top:0.95rem;border-top:1px dashed var(--line);padding-top:0.95rem;max-height:0;opacity:0;overflow:hidden;transition:max-height .4s ease, opacity .3s ease}
  .sc-report.show{max-height:380px;opacity:1}
  .sc-report .rh{display:flex;align-items:center;justify-content:space-between;gap:0.8rem;margin-bottom:0.8rem}
  .sc-report .rl{font-family:var(--font-mono);font-size:0.64rem;letter-spacing:0.12em;text-transform:uppercase;color:var(--faint)}
  .sc-report .tag{font-size:0.7rem;font-weight:600;padding:0.26rem 0.62rem;border-radius:var(--r-pill);white-space:nowrap}
  .tag-ok{background:oklch(0.6 0.13 150 / 0.14);color:var(--success)}
  .tag-rev{background:oklch(0.76 0.13 80 / 0.18);color:var(--gold-deep)}
  .sc-report .pts{display:flex;flex-direction:column;gap:0.5rem}
  .ptrow{display:grid;grid-template-columns:8.5rem 1fr 1.8rem;align-items:center;gap:0.6rem}
  .ptrow .pn{font-size:0.8rem;color:var(--muted)}
  .ptrow .pt-track{height:0.42rem;border-radius:var(--r-pill);background:oklch(0.5 0.06 240 / 0.1);overflow:hidden}
  .ptrow .pt-fill{height:100%;border-radius:var(--r-pill);background:linear-gradient(90deg,var(--primary),oklch(0.62 0.12 215));width:0;transition:width .55s cubic-bezier(.2,.7,.2,1)}
  .ptrow .pv{font-family:var(--font-mono);font-size:0.78rem;font-weight:600;color:var(--primary);text-align:right}
  .ptrow .pv.zero{color:var(--faint)}
  .sc-report .read{margin-top:0.85rem;font-size:0.84rem;color:var(--muted);line-height:1.5}
  .sc-report .read b{color:var(--ink);font-weight:600}
  .sc-report .invisible-note{margin-top:0.75rem;font-size:0.74rem;color:var(--faint);display:flex;gap:0.45rem;align-items:flex-start}
  .sc-report .invisible-note svg{flex:none;width:0.95rem;height:0.95rem;margin-top:0.1rem;stroke:var(--faint)}

  /* ===== Mapa da simulação — product mock (native canvas palette) ===== */
  .builder{margin-top:3rem}
  .builder .blead{display:flex;align-items:flex-start;gap:0.65rem;max-width:62ch;margin-bottom:1.4rem;color:var(--muted);font-size:1rem;line-height:1.55}
  .builder .blead svg{flex:none;width:1.25rem;height:1.25rem;margin-top:0.22rem;stroke:var(--gold-deep)}
  .builder .blead b{color:var(--ink);font-weight:600}
  .appwin{border:1px solid var(--line);border-radius:var(--r-lg);overflow:hidden;box-shadow:var(--shadow-lg);background:#FCFAF6}
  .appbar{display:flex;align-items:center;gap:0.5rem;padding:0.7rem 1rem;background:#fff;border-bottom:1px solid #EBEEF0}
  .appbar .dots{display:flex;gap:0.42rem}
  .appbar .dots i{width:0.7rem;height:0.7rem;border-radius:50%;display:block;background:#D9DFE3}
  .appbar .dots i:first-child{background:#E1A536}
  .appbar .url{margin-left:0.5rem;font-family:var(--font-mono);font-size:0.74rem;color:#9AA3AA}
  .mapwin{padding:1.2rem 1.3rem 0}
  .maphead .meyebrow{font-family:var(--font-mono);font-size:0.66rem;letter-spacing:0.14em;text-transform:uppercase;color:#1B6C8C;font-weight:600}
  .maphead h3{font-family:var(--font-display);font-size:clamp(1.3rem,3vw,1.6rem);font-weight:460;color:#172128;margin:0.3rem 0 0}
  .mapstats{display:flex;align-items:center;flex-wrap:wrap;gap:0.5rem 1rem;margin-top:0.7rem;font-size:0.86rem;color:#616A71}
  .mapstats b{color:#172128;font-weight:700;font-family:var(--font-mono)}
  .mapstats .sep{width:3px;height:3px;border-radius:50%;background:#9AA3AA}
  .mapstats .pill-amber{display:inline-flex;align-items:center;gap:0.35rem;font-size:0.78rem;font-weight:600;color:#7A5410;background:#FCF2DB;border:1px solid #E6C98A;border-radius:var(--r-pill);padding:0.24rem 0.68rem}
  .mapstats .pill-amber svg{width:0.85rem;height:0.85rem;stroke:#E1A536}
  .canvas{position:relative;margin-top:1rem;background:#FCFAF6;background-image:radial-gradient(#E6E3DA 1.4px, transparent 1.4px);background-size:18px 18px;border-top:1px solid #EBEEF0;overflow-x:auto;overflow-y:hidden;-webkit-overflow-scrolling:touch}
  .canvas-inner{position:relative;width:720px;height:360px;margin:0 auto}
  .canvas svg.edges{position:absolute;inset:0;width:720px;height:360px;overflow:visible;pointer-events:none;z-index:5}
  .canvas svg.edges path{fill:none;stroke:#b7bfce;stroke-width:1.8}
  .edge-label{position:absolute;font-size:10.5px;color:#616A71;background:#fff;border:1px solid #D9DFE3;border-radius:999px;padding:1.5px 9px;z-index:13;white-space:nowrap;box-shadow:0 1px 2px rgba(20,30,55,.05)}
  .vxn{position:absolute;width:255px;background:#fff;border:1px solid #D9DFE3;border-radius:12px;box-shadow:0 2px 6px rgba(20,30,55,.07);z-index:10}
  .vxn.root .vxmsg{border-top:3px solid #1B6C8C;border-radius:12px 12px 0 0}
  .vxmsg{padding:9px 11px;border-bottom:1px solid #EBEEF0}
  .vxhead{display:flex;align-items:center;justify-content:space-between;margin-bottom:6px;gap:6px}
  .vxid{display:inline-flex;align-items:center;gap:5px;font-family:var(--font-mono);font-size:11px;font-weight:650;color:#0E5570}
  .vxid svg{width:12px;height:12px;stroke:#1B6C8C;flex:none}
  .vxbadge{font-family:var(--font-sans);font-size:8.5px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;color:#1B6C8C;background:#E2F1FA;padding:1px 5px;border-radius:5px}
  .vxlock{width:11px!important;height:11px!important;stroke:#9AA3AA!important;margin-left:1px}
  .vxtimer{display:inline-flex;align-items:center;gap:3px;font-family:var(--font-mono);font-size:10.5px;color:#9AA3AA;flex:none}
  .vxtimer svg{width:11px;height:11px;stroke:#9AA3AA}
  .vxtext{font-size:12px;line-height:1.4;color:#172128}
  .vxsec-l{display:flex;justify-content:space-between;font-size:9.5px;font-weight:700;letter-spacing:.04em;text-transform:uppercase;color:#9AA3AA;padding:8px 11px 4px}
  .vxcomp{padding:0 11px 8px;display:flex;flex-direction:column;gap:3px}
  .vxcrow{display:grid;grid-template-columns:auto 1fr auto;align-items:center;gap:10px;font-size:11px}
  .vxcrow .cn{color:#616A71;font-weight:600}
  .vxcrow .cin{justify-self:start;width:54px;height:18px;border:1px solid #D9DFE3;border-radius:5px;background:#fff}
  .vxcrow .cacc{font-family:var(--font-mono);font-weight:700;color:#0E5570;font-size:11.5px}
  .vxcrow .cacc.z{color:#9AA3AA}
  .vxout{border-top:1px solid #EBEEF0;padding:7px 11px}
  .vxorow{display:flex;align-items:center;gap:6px}
  .vxorow .otext{flex:1;min-width:0;font-size:11.5px;color:#172128;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
  .vxsel{flex:none;display:inline-flex;align-items:center;gap:3px;height:22px;padding:0 6px;border:1px solid #D9DFE3;border-radius:6px;background:#fff;font-family:var(--font-mono);font-size:10.5px;color:#0E5570}
  .vxsel svg{width:9px;height:9px;stroke:#616A71}
  .vxbtn{flex:none;width:20px;height:20px;border-radius:6px;display:grid;place-items:center;border:1px solid #D9DFE3}
  .vxbtn svg{width:11px;height:11px}
  .vxbtn.del{background:#FAE7E5;border-color:#FAE7E5;stroke:#CC3E35}
  .vxbtn.add{background:#E2F1FA;border-color:#E2F1FA;stroke:#1B6C8C}
  .vxto{display:flex;align-items:center;gap:6px;padding:6px 11px;border-top:1px solid #EBEEF0;background:#FCFBF7}
  .vxto .tl{flex:none;font-size:9.5px;font-weight:700;text-transform:uppercase;color:#9AA3AA;display:inline-flex;align-items:center;gap:4px}
  .vxto .tl svg{width:11px;height:11px;stroke:#E1A536}
  .vxto .tsel{flex:1;min-width:0;height:20px;border:1px solid #E6C98A;border-radius:6px;background:#fff;font-size:10.5px;color:#7A5410;display:flex;align-items:center;padding:0 6px}
  .vxto .add{flex:none;width:20px;height:20px;border-radius:6px;display:grid;place-items:center;background:#FCF2DB;border:1px solid #E6C98A;stroke:#7A5410}
  .vxto .add svg{width:11px;height:11px}
  .vxfoot{padding:6px 9px;border-top:1px solid #EBEEF0}
  .vxfoot .fadd{display:inline-flex;align-items:center;gap:4px;font-size:11px;font-weight:600;color:#1B6C8C;border:1px dashed #D9DFE3;border-radius:7px;padding:3px 9px;background:transparent}
  .vxfoot .fadd svg{width:11px;height:11px;stroke:#1B6C8C}
  .vxport{position:absolute;width:11px;height:11px;border-radius:50%;background:#fff;border:2px solid #1B6C8C;z-index:12}

`;
const landingMarkup = `<header class="nav" id="nav">
  <div class="wrap nav-inner">
    <a href="#topo" class="brand" aria-label="Práxis, por iForce">
      Práxis<span class="dot"></span><small>by iForce</small>
    </a>
    <nav class="nav-links" aria-label="Seções">
      <a class="link" href="#problema">Por que</a>
      <a class="link" href="#como">Como funciona</a>
      <a class="link" href="#aplicacoes">Onde se aplica</a>
      <a class="link" href="#integracoes">Integrações</a>
      <a class="link" href="#precos">Preços</a>
    </nav>
    <div class="nav-cta">
      <a class="link" href="#entrar">Entrar</a>
      <a class="btn btn-primary" href="#cta">Agendar demo</a>
    </div>
    <button class="menu-btn" id="menuBtn" aria-label="Abrir menu" aria-expanded="false">
      <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round"><path d="M4 7h16M4 12h16M4 17h16"/></svg>
    </button>
  </div>
</header>

<main id="topo">

  <!-- HERO -->
  <section class="hero">
    <div class="wrap hero-grid">
      <div class="hero-copy">
        <span class="eyebrow reveal-up d1">Avaliação situacional · sem IA julgando pessoas</span>
        <h1 class="reveal-up d2">Veja como a pessoa <span class="accent">decide</span> no cenário real.</h1>
        <p class="lead reveal-up d3">Antes de gastar tempo com entrevistas, a Práxis mostra como alguém age numa situação real — com pontuação por competência e uma trilha que qualquer auditor pode verificar. Para recrutamento, saúde, educação ou onde decisão importa.</p>
        <div class="hero-ctas reveal-up d4">
          <a class="btn btn-primary" href="#cta">Agendar demonstração <span class="btn-arrow">→</span></a>
          <a class="btn btn-ghost" href="#aplicacoes">Ver onde se aplica</a>
        </div>
        <div class="integrations-line reveal-up d5">
          <span class="pip"></span>
          <span>Integra com <b>Gupy</b>, <b>Recrutei</b> e outros sistemas — o resultado volta para a plataforma de origem.</span>
        </div>
      </div>

      <!-- interactive scenario -->
      <div class="scenario reveal-up d4" aria-label="Demonstração de um cenário">
        <div class="sc-top">
          <div class="sc-id">
            <div class="avatar">CM</div>
            <div>
              <div class="who">Carlos M.</div>
              <div class="stage">ETAPA 1/3 · ABERTURA</div>
            </div>
          </div>
          <div class="sc-timer"><span class="tdot"></span>00:24</div>
        </div>
        <div class="sc-body">
          <div class="sc-tag">Cliente · furioso</div>
          <p class="sc-msg">“Já é a terceira vez que abro chamado e ninguém resolve. Preciso disso hoje, ou vou escalar para cima.”</p>
          <div class="sc-opts" role="group" aria-label="Como você agiria?">
            <button class="opt"><span class="key">A</span><span>Pedir desculpas e prometer retorno em 30min, sem confirmar com o time.</span></button>
            <button class="opt"><span class="key">B</span><span>Reconhecer a frustração, confirmar o número do chamado e dar um prazo realista alinhado ao time.</span></button>
            <button class="opt"><span class="key">C</span><span>Explicar a política interna de SLA e pedir paciência até o próximo ciclo.</span></button>
            <button class="opt"><span class="key">D</span><span>Encaminhar direto para o supervisor sem tentar resolver.</span></button>
          </div>
          <p class="sc-note" id="scNote">
            <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2 4 5v6c0 5 3.4 8.3 8 11 4.6-2.7 8-6 8-11V5z"/></svg>
            <span>Demonstração interativa. Escolha uma resposta — todas são plausíveis de propósito.</span>
          </p>
          <div class="sc-report" id="scReport" aria-live="polite">
            <div class="rh"><span class="rl">Leitura desta decisão</span><span class="tag" id="rTag"></span></div>
            <div class="pts" id="rPts"></div>
            <p class="read" id="rRead"></p>
            <p class="invisible-note">
              <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 3l18 18M10.6 10.6a2 2 0 0 0 2.9 2.9M9.5 5.2A9 9 0 0 1 21 12a13 13 0 0 1-2.1 2.9M6.3 6.3A13 13 0 0 0 3 12a12 12 0 0 0 6 6"/></svg>
              <span>Para o candidato isso fica invisível — ele só decide. Você recebe a leitura.</span>
            </p>
          </div>
        </div>
      </div>
    </div>
  </section>

  <!-- PROBLEM -->
  <section class="sec sec-alt" id="problema">
    <div class="wrap">
      <div class="sec-head">
        <span class="eyebrow">O problema</span>
        <h2>O teste tradicional virou alvo fácil.</h2>
        <p class="lead">Provas de múltipla escolha e redação perderam confiabilidade: o candidato consulta a IA, decora a resposta certa e passa. Você entrevista quem é bom de prova — não quem sabe lidar com a situação.</p>
      </div>
      <div class="split">
        <div class="col bad">
          <h3><span class="badge">Teste comum</span></h3>
          <ul class="clist">
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>Mede conhecimento decorável, não comportamento.</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>A resposta “certa” é óbvia e fácil de colar com IA.</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>Nota sem contexto: o gestor não sabe o porquê.</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>Entrevistas desperdiçadas com quem não tem o perfil.</li>
          </ul>
        </div>
        <div class="col good">
          <h3><span class="badge">Práxis</span></h3>
          <ul class="clist">
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Coloca a pessoa para decidir num cenário real do cargo.</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Todas as opções são plausíveis: mede julgamento.</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Score por competência com a trilha exata de cada ponto.</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>O gestor entrevista quem já provou saber lidar.</li>
          </ul>
        </div>
      </div>
    </div>
  </section>

  <!-- HOW IT WORKS -->
  <section class="sec" id="como">
    <div class="wrap">
      <div class="sec-head">
        <span class="eyebrow">Como funciona</span>
        <h2>Um cenário ramificado, montado pelo seu RH.</h2>
        <p class="lead">Sem programar e sem IA decidindo nada. O RH escreve o caso, as respostas e quanto cada uma vale por competência. A pontuação é determinística: regra e cálculo.</p>
      </div>
      <div class="steps">
        <div class="step">
          <span class="rule"></span>
          <span class="num">PASSO 01</span>
          <h3>O RH monta o caso</h3>
          <p>Define a situação crítica do cargo, as respostas possíveis e o peso de cada competência.</p>
        </div>
        <div class="step">
          <span class="rule"></span>
          <span class="num">PASSO 02</span>
          <h3>A pessoa decide</h3>
          <p>Pelo link ou direto no seu sistema, ela entra no cenário, enfrenta a situação e escolhe como agir — sob o tempo de cada etapa.</p>
        </div>
        <div class="step">
          <span class="num">PASSO 03</span>
          <h3>Você recebe a evidência</h3>
          <p>Score por competência e a trilha de decisão voltam para o seu sistema, prontos para análise e tomada de decisão.</p>
        </div>
      </div>

      <!-- Mapa da simulação — product mock -->
      <div class="builder">
        <p class="blead">
          <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M3 12h18M3 18h18"/></svg>
          <span>Não é planilha nem formulário: o RH desenha o caso <b>visualmente</b>, ligando cada resposta ao próximo turno. É a tela que a sua equipe usa de verdade.</span>
        </p>
        <div class="appwin">
          <div class="appbar"><span class="dots"><i></i><i></i><i></i></span><span class="url">praxis.iforce.com.br/nova/mapa</span></div>
          <div class="mapwin">
            <div class="maphead">
              <span class="meyebrow">Passo 3 · construtor do fluxo</span>
              <h3>Mapa da simulação</h3>
              <div class="mapstats">
                <span><b>6</b> etapas</span><span class="sep"></span>
                <span><b>2</b> saídas</span><span class="sep"></span>
                <span><b>0</b> encerramentos</span>
                <span class="pill-amber"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 2h4M12 8v5l3 2M12 22a8 8 0 1 0 0-16 8 8 0 0 0 0 16z"/></svg> 6 sem tempo</span>
              </div>
            </div>
            <div class="canvas">
              <div class="canvas-inner">
                <svg class="edges" viewBox="0 0 720 360" preserveAspectRatio="none" aria-hidden="true"><path d="M285 224 C355 224 360 84 430 84"/></svg>
                <span class="edge-label" style="left:300px;top:188px">alternativa A</span>

                <!-- turno-1 (raiz) -->
                <article class="vxn root" style="left:30px;top:40px">
                  <span class="vxport" style="left:-6px;top:78px;border-color:#aeb6c6"></span>
                  <span class="vxport" style="right:-6px;top:184px"></span>
                  <div class="vxmsg">
                    <div class="vxhead">
                      <span class="vxid"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5h16v11H8l-4 4z"/></svg>turno-1 <span class="vxbadge">início</span><svg class="vxlock" viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M6 11h12v9H6zM9 11V8a3 3 0 0 1 6 0v3"/></svg></span>
                      <span class="vxtimer"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 8v5l3 2M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z"/></svg>45s</span>
                    </div>
                    <div class="vxtext">Frustrado, o cliente entrou em contato após encontrar um erro no aplicativo e não…</div>
                  </div>
                  <div class="vxsec-l"><span>Competências</span><span>Acum.</span></div>
                  <div class="vxcomp">
                    <div class="vxcrow"><span class="cn">Pro</span><span class="cin"></span><span class="cacc">50</span></div>
                    <div class="vxcrow"><span class="cn">RdC</span><span class="cin"></span><span class="cacc">50</span></div>
                    <div class="vxcrow"><span class="cn">C3</span><span class="cin"></span><span class="cacc z">0</span></div>
                  </div>
                  <div class="vxout">
                    <div class="vxsec-l" style="padding:0 0 5px"><span>Saídas (respostas)</span></div>
                    <div class="vxorow">
                      <span class="otext">alternativa A</span>
                      <span class="vxsel">→ turno-2 <svg viewBox="0 0 24 24" fill="none" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"/></svg></span>
                      <span class="vxbtn del"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 7h14M9 7V5h6v2M7 7l1 13h8l1-13"/></svg></span>
                    </div>
                  </div>
                  <div class="vxto">
                    <span class="tl"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 2h4M12 8v5l3 2M12 22a8 8 0 1 0 0-16 8 8 0 0 0 0 16z"/></svg> Tempo acaba</span>
                    <span class="tsel">— defina —</span>
                    <span class="add"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 6v12M6 12h12"/></svg></span>
                  </div>
                  <div class="vxfoot"><span class="fadd"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 6v12M6 12h12"/></svg> saída</span></div>
                </article>

                <!-- turno-2 -->
                <article class="vxn" style="left:430px;top:40px">
                  <span class="vxport" style="left:-6px;top:44px"></span>
                  <div class="vxmsg">
                    <div class="vxhead">
                      <span class="vxid"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 15l6-6M8 8H6a4 4 0 0 0 0 8h2M16 16h2a4 4 0 0 0 0-8h-2"/></svg>turno-2</span>
                      <span class="vxtimer"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 8v5l3 2M12 21a9 9 0 1 0 0-18 9 9 0 0 0 0 18z"/></svg>45s</span>
                    </div>
                    <div class="vxtext">O bug parece estar relacionado ao login. Já tentou reiniciar o aplicativo?</div>
                  </div>
                  <div class="vxsec-l"><span>Competências</span><span>Acum.</span></div>
                  <div class="vxcomp">
                    <div class="vxcrow"><span class="cn">Pro</span><span class="cin"></span><span class="cacc">100</span></div>
                    <div class="vxcrow"><span class="cn">RdC</span><span class="cin"></span><span class="cacc">100</span></div>
                    <div class="vxcrow"><span class="cn">C3</span><span class="cin"></span><span class="cacc z">0</span></div>
                  </div>
                  <div class="vxout">
                    <div class="vxsec-l" style="padding:0 0 5px"><span>Saídas (respostas)</span></div>
                    <div class="vxorow">
                      <span class="otext">Sim, reiniciei mas não…</span>
                      <span class="vxsel">— defina — <svg viewBox="0 0 24 24" fill="none" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"><path d="M6 9l6 6 6-6"/></svg></span>
                      <span class="vxbtn add"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 6v12M6 12h12"/></svg></span>
                    </div>
                  </div>
                  <div class="vxto">
                    <span class="tl"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10 2h4M12 8v5l3 2M12 22a8 8 0 1 0 0-16 8 8 0 0 0 0 16z"/></svg> Tempo acaba</span>
                    <span class="tsel">— defina —</span>
                    <span class="add"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 6v12M6 12h12"/></svg></span>
                  </div>
                  <div class="vxfoot"><span class="fadd"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 6v12M6 12h12"/></svg> saída</span></div>
                </article>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </section>

  <!-- FEATURES + SIGNATURE EVIDENCE CARD -->
  <section class="sec sec-alt" id="evidencia">
    <div class="wrap">
      <div class="sec-head">
        <span class="eyebrow">Por dentro</span>
        <h2>Feito para decisão defensável.</h2>
        <p class="lead">Cada nota é decomponível. Você consegue mostrar, ponto a ponto, por que uma pessoa recebeu o que recebeu.</p>
      </div>

      <div class="feat-grid">
        <div class="feats">
          <div class="feat">
            <h3><span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v18M3 12h18"/></svg></span>Sem IA julgando candidato</h3>
            <p>A pontuação vem de critérios, pesos e cálculo. Zero caixa-preta, zero custo de IA, totalmente explicável.</p>
          </div>
          <div class="feat">
            <h3><span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12h4l3-8 4 16 3-8h4"/></svg></span>Score justo entre caminhos</h3>
            <p>A nota é normalizada pelo caminho percorrido: quem segue um cenário mais curto não é penalizado.</p>
          </div>
          <div class="feat">
            <h3><span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 12l2 2 4-4M21 12a9 9 0 1 1-18 0 9 9 0 0 1 18 0z"/></svg></span>Trilha auditável</h3>
            <p>Cada ponto tem origem: qual etapa, qual escolha, qual rubrica. Pronto para o gestor e para o jurídico.</p>
          </div>
          <div class="feat">
            <h3><span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3a9 9 0 1 0 9 9M21 3l-9 9"/></svg></span>Decide, não reprova</h3>
            <p>Erro crítico aciona revisão humana. A decisão final é sempre de uma pessoa.</p>
          </div>
          <div class="feat">
            <h3><span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 5h16v14H4zM4 9h16M9 9v10"/></svg></span>Biblioteca de cenários</h3>
            <p>Modelos prontos por área e senioridade. O RH edita, testa com um piloto e publica quando quiser.</p>
          </div>
          <div class="feat">
            <h3><span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 12a8 8 0 0 1 16 0M8 12a4 4 0 0 1 8 0M12 12v8"/></svg></span>No seu sistema</h3>
            <p>Ninguém troca de ferramenta. Nota e competências chegam direto na plataforma que você já usa.</p>
          </div>
        </div>

        <!-- signature -->
        <aside class="evidence" aria-label="Exemplo de cartão de evidência de um candidato">
          <div class="ev-top">
            <span class="t">Cartão de evidência · att_4f7c</span>
            <span class="ev-seal"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 12l2 2 4-4M12 3a9 9 0 1 0 9 9 9 9 0 0 0-9-9z"/></svg>verificável</span>
          </div>
          <div class="ev-body">
            <div class="ev-score">
              <span class="n">82</span><span class="of">/100</span>
              <span class="decision">Recomendar entrevista</span>
            </div>
            <div class="ev-comp" id="evComp">
              <div class="cbar"><div class="clabel"><span>Comunicação</span><b>88</b></div><div class="track"><span class="fill" data-w="88"></span></div></div>
              <div class="cbar"><div class="clabel"><span>Resolução de problemas</span><b>80</b></div><div class="track"><span class="fill" data-w="80"></span></div></div>
              <div class="cbar"><div class="clabel"><span>Aderência à política</span><b>76</b></div><div class="track"><span class="fill" data-w="76"></span></div></div>
            </div>
            <div class="ev-trail">
              <div class="lab">Trilha de decisão</div>
              <code>turno-1 ▸ <span class="ok">B</span>  +3 Comunicação · +2 Resolução
turno-2 ▸ <span class="ok">A</span>  +2 Resolução · +1 Política
turno-3 ▸ <span class="ok">C</span>  +2 Comunicação
<span class="gold">———  nenhum erro crítico registrado</span></code>
            </div>
          </div>
        </aside>
      </div>
    </div>
  </section>

  <!-- ONDE SE APLICA -->
  <section class=”sec” id=”aplicacoes”>
    <div class=”wrap”>
      <div class=”sec-head”>
        <span class=”eyebrow”>Onde se aplica</span>
        <h2>Onde decisão importa, a Práxis entrega evidência.</h2>
        <p class=”lead”>O mesmo motor — cenário situacional com score determinístico — se adapta a qualquer contexto onde você precisa avaliar como pessoas decidem.</p>
      </div>
      <div class=”use-cases”>
        <div class=”uc uc-recruit”>
          <span class=”uc-ico”><svg viewBox=”0 0 24 24” fill=”none” stroke-width=”2” stroke-linecap=”round” stroke-linejoin=”round”><path d=”M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2”/><circle cx=”9” cy=”7” r=”4”/><path d=”M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75”/></svg></span>
          <h3>Recrutamento e seleção</h3>
          <p>Avalie candidatos antes da entrevista. O gestor vê como a pessoa lida com a situação real do cargo.</p>
          <div class=”uc-examples”>Processos seletivos, trainees, vagas de volume, headhunting</div>
        </div>
        <div class=”uc uc-health”>
          <span class=”uc-ico”><svg viewBox=”0 0 24 24” fill=”none” stroke-width=”2” stroke-linecap=”round” stroke-linejoin=”round”><path d=”M22 12h-4l-3 9L9 3l-3 9H2”/></svg></span>
          <h3>Saúde — profissionais</h3>
          <p>Avalie tomada de decisão clínica: triagem, comunicação com paciente, dilemas éticos e conduta sob pressão.</p>
          <div class=”uc-examples”>Seleção de residentes, enfermagem, equipes de emergência</div>
        </div>
        <div class=”uc uc-health”>
          <span class=”uc-ico”><svg viewBox=”0 0 24 24” fill=”none” stroke-width=”2” stroke-linecap=”round” stroke-linejoin=”round”><path d=”M12 21.35l-1.45-1.32C5.4 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.4 6.86-8.55 11.54L12 21.35z”/></svg></span>
          <h3>Saúde — pacientes</h3>
          <p>Avalie prontidão para autocuidado: como o paciente decide em cenários de adesão ao tratamento, manejo de crises e hábitos de saúde.</p>
          <div class=”uc-examples”>Doenças crônicas, pré-cirúrgico, saúde mental, reabilitação</div>
        </div>
        <div class=”uc uc-edu”>
          <span class=”uc-ico”><svg viewBox=”0 0 24 24” fill=”none” stroke-width=”2” stroke-linecap=”round” stroke-linejoin=”round”><path d=”M2 3h6a4 4 0 0 1 4 4v14a3 3 0 0 0-3-3H2z”/><path d=”M22 3h-6a4 4 0 0 0-4 4v14a3 3 0 0 1 3-3h7z”/></svg></span>
          <h3>Educação</h3>
          <p>Meça competências socioemocionais de alunos ou avalie professores em cenários de gestão de sala e inclusão.</p>
          <div class=”uc-examples”>Escolas, universidades, concursos docentes, formação continuada</div>
        </div>
        <div class=”uc uc-corp”>
          <span class=”uc-ico”><svg viewBox=”0 0 24 24” fill=”none” stroke-width=”2” stroke-linecap=”round” stroke-linejoin=”round”><path d=”M12 20V10M18 20V4M6 20v-4”/></svg></span>
          <h3>Desenvolvimento interno</h3>
          <p>Diagnóstico de gaps de liderança, avaliação para promoção e programas de T&D com pré/pós mensuração.</p>
          <div class=”uc-examples”>Sucessão, PDI, academias corporativas, onboarding de líderes</div>
        </div>
        <div class=”uc uc-compliance”>
          <span class=”uc-ico”><svg viewBox=”0 0 24 24” fill=”none” stroke-width=”2” stroke-linecap=”round” stroke-linejoin=”round”><path d=”M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z”/></svg></span>
          <h3>Compliance e segurança</h3>
          <p>Teste como colaboradores reagem a cenários de fraude, conflito de interesse, segurança do trabalho ou LGPD.</p>
          <div class=”uc-examples”>Bancos, indústria, mineração, farmacêutico, varejo</div>
        </div>
      </div>
    </div>
  </section>

  <!-- INTEGRAÇÕES -->
  <section class=”sec sec-alt” id=”integracoes”>
    <div class=”wrap”>
      <div class=”sec-head”>
        <span class=”eyebrow”>Integrações</span>
        <h2>Seu sistema organiza o fluxo. A Práxis adiciona a evidência.</h2>
        <p class=”lead”>A Práxis entra como uma etapa no processo que você já tem. A pessoa nem percebe que mudou de sistema, e o resultado volta sozinho — sem ninguém trocar de ferramenta.</p>
      </div>
      <div class=”integ-band”>
        <div class=”flow”>
          <div class=”fstep gp”><span class=”fn”>1</span><span>Participante recebe o convite pelo <b>seu sistema</b></span></div>
          <div class=”fstep”><span class=”fn”>2</span><span>Faz a avaliação situacional na Práxis</span></div>
          <div class=”fstep”><span class=”fn”>3</span><span>Score e competências voltam automaticamente (com retentativas)</span></div>
          <div class=”fstep gp”><span class=”fn”>4</span><span>Você decide com base na evidência, sem sair da plataforma</span></div>
        </div>
        <div class=”connect” aria-label=”Como a integração funciona”>
          <div class=”cstep”>
            <span class=”ci ci-ats”>A</span>
            <div><b>Processo criado no seu sistema</b><span>Você adiciona a Práxis como etapa de avaliação (Gupy, Recrutei, API, webhook).</span></div>
          </div>
          <div class=”cdown”></div>
          <div class=”cstep”>
            <span class=”ci ci-prx”>P</span>
            <div><b>A pessoa faz a avaliação</b><span>Dentro do fluxo do seu sistema, sem cadastro novo.</span></div>
          </div>
          <div class=”cdown”></div>
          <div class=”cstep”>
            <span class=”ci ci-ok”><svg viewBox=”0 0 24 24” fill=”none” stroke-width=”2.4” stroke-linecap=”round” stroke-linejoin=”round”><path d=”M20 6 9 17l-5-5”/></svg></span>
            <div><b>Resultado entregue</b><span>A nota volta sozinha — mesmo se a conexão cair no meio.</span></div>
          </div>
          <div class=”cnote”><span class=”pip”></span> Gupy · Recrutei · API aberta · Webhook — sem planilha ou copiar-e-colar.</div>
        </div>
      </div>
    </div>
  </section>

  <!-- GOVERNANCE (dark) -->
  <section class="sec dark-band" id="governanca">
    <div class="wrap">
      <div class="sec-head">
        <span class="eyebrow">Governança & auditoria</span>
        <h2>Pronto para o jurídico e para o compliance.</h2>
        <p class="lead">As mesmas garantias que a sua equipe de governança exigiria de um sistema crítico — sem precisar pedir.</p>
      </div>
      <div class="gov-grid">
        <div class="gov">
          <span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 7a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2zM9 9h6M9 13h6M9 17h4"/></svg></span>
          <h3>Trilha imutável</h3>
          <p>Cada evento da tentativa fica registrado — criação, resposta, timeout e finalização — e não pode ser alterado depois.</p>
        </div>
        <div class="gov">
          <span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3l7 3v6c0 5-3.5 8-7 9-3.5-1-7-4-7-9V6z"/></svg></span>
          <h3>LGPD por desenho</h3>
          <p>Bases legais expostas, retenção configurável e anonimização programada após o ciclo da seleção.</p>
        </div>
        <div class="gov">
          <span class="ico"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 3v18M5 7l7-4 7 4M5 7v6c0 4 3 6 7 7 4-1 7-3 7-7V7"/></svg></span>
          <h3>Defensabilidade</h3>
          <p>Pesos versionados e critérios visíveis permitem reconstruir por que cada candidato recebeu cada ponto.</p>
        </div>
      </div>
      <div class="gov-foot">
        <span class="chip"><b>WCAG 2.1 AA</b> · acessível por teclado e leitor de tela</span>
        <span class="chip">conforme a <b>LBI</b> · tempo ajustável por etapa</span>
        <span class="chip">multi-tenant · <b>isolamento por empresa</b></span>
      </div>
    </div>
  </section>

  <!-- PRICING -->
  <section class="sec" id="precos">
    <div class="wrap">
      <div class="sec-head">
        <span class="eyebrow">Planos</span>
        <h2>Comece por avaliação. Cresça quando fizer sentido.</h2>
        <p class="lead">Sem custo de IA, porque não há IA na pontuação. Você paga pela avaliação — não por inferência.</p>
        <div class="roi">
          <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 1v22M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>
          <span>Uma decisão baseada em achismo custa caro — em qualquer área. A Práxis custa uma fração — e deixa o porquê de cada resultado registrado.</span>
        </div>
        <div class="toggle" role="group" aria-label="Ciclo de cobrança">
          <button id="bMonthly" class="on" aria-pressed="true">Mensal</button>
          <button id="bAnnual" aria-pressed="false">Anual</button>
          <span class="save">2 meses grátis</span>
        </div>
      </div>

      <div class="plans">
        <!-- Avulso -->
        <div class="plan">
          <div class="pname">Avulso</div>
          <div class="pfor">Para pilotos e demandas pontuais, sem mensalidade.</div>
          <div class="price"><span class="cur">R$</span><span class="amt">69,90</span><span class="per">/ pessoa avaliada</span></div>
          <div class="psub">Pague só por quem você avaliar.</div>
          <a class="btn btn-ghost" href="#cta">Começar</a>
          <ul class="plist">
            <li class="head"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>O essencial para validar</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Integrações nativas (Gupy, Recrutei, API)</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>1 simulação ativa</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Biblioteca de cenários (modelos)</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Score por competência</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Trilha auditável</li>
          </ul>
        </div>

        <!-- Crescimento (feature) -->
        <div class="plan feature">
          <span class="ptag">Mais escolhido</span>
          <div class="pname">Crescimento</div>
          <div class="pfor">Para quem avalia com volume recorrente.</div>
          <div class="price"><span class="cur">R$</span><span class="amt" data-monthly="7.990" data-annual="6.658">7.990</span><span class="per" id="cycMonthly">/ mês</span></div>
          <div class="psub" id="cycSub">R$ 39,90/avaliação · 200 inclusas/mês · +R$ 49,90 por adicional</div>
          <a class="btn btn-primary" href="#cta">Agendar demo <span class="btn-arrow">→</span></a>
          <ul class="plist">
            <li class="head"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"/></svg>Tudo do Avulso, e mais:</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Simulações e vagas ilimitadas</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Edição completa da biblioteca</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Painel comparativo: ranqueie e compare participantes</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Modo piloto antes de publicar</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Governança e LGPD configuráveis</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Multiusuário e suporte prioritário</li>
          </ul>
        </div>

        <!-- Enterprise -->
        <div class="plan">
          <div class="pname">Enterprise</div>
          <div class="pfor">Para alto volume e exigência de compliance.</div>
          <div class="price consulta"><span class="amt">Sob consulta</span></div>
          <div class="psub">Contrato anual, escopo sob medida.</div>
          <a class="btn btn-gold" href="#cta">Falar com vendas</a>
          <ul class="plist">
            <li class="head"><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12h14"/></svg>Tudo do Crescimento, e mais:</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Trilha imutável com retenção sob medida</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Talent-match entre participantes</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Validação assistida: estudo nota × desempenho</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>SSO / SAML e ambiente dedicado</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>SLA e gerente de conta</li>
            <li><svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 6 9 17l-5-5"/></svg>Contrato e DPA com o jurídico</li>
          </ul>
        </div>
      </div>

      <p class="pfoot">
        <svg viewBox="0 0 24 24" fill="none" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 12l2 2 4-4M12 3a9 9 0 1 0 9 9 9 9 0 0 0-9-9z"/></svg>
        Todos os planos incluem integrações nativas e a trilha auditável. Preços em reais, impostos à parte.
      </p>
    </div>
  </section>

  <!-- FAQ -->
  <section class="sec sec-alt" id="faq">
    <div class="wrap">
      <div class="sec-head">
        <span class="eyebrow">FAQ</span>
        <h2>Perguntas frequentes</h2>
      </div>
      <div class="faq">
        <div class="qa">
          <button aria-expanded="false"><span class="q">A Práxis usa IA generativa para avaliar pessoas?</span><span class="ic"></span></button>
          <div class="ans"><p>Não. A pontuação sai de critérios e pesos versionados definidos pela sua equipe. É regra e cálculo — auditável e sem custo de inferência.</p></div>
        </div>
        <div class="qa">
          <button aria-expanded="false"><span class="q">Com quais sistemas a Práxis integra?</span><span class="ic"></span></button>
          <div class="ans"><p>Hoje integra nativamente com Gupy e Recrutei. Para outros sistemas (hospitalares, educacionais, ERPs), oferecemos API aberta e webhook — a Práxis recebe o convite e devolve o resultado automaticamente, com retentativas quando há falha temporária.</p></div>
        </div>
        <div class="qa">
          <button aria-expanded="false"><span class="q">Como é a cobrança?</span><span class="ic"></span></button>
          <div class="ans"><p>No plano Avulso você paga por candidato avaliado, sem mensalidade — ideal para pilotos. No Crescimento, é uma assinatura por faixa de volume, com candidatos incluídos e adicionais por candidato. O Enterprise é contrato anual com escopo sob medida.</p></div>
        </div>
        <div class="qa">
          <button aria-expanded="false"><span class="q">Quanto tempo leva para colocar no ar?</span><span class="ic"></span></button>
          <div class="ans"><p>Com a biblioteca de cenários, dá para publicar um teste no mesmo dia: o RH escolhe um modelo do cargo, ajusta o caso e os pesos, testa em modo piloto e publica.</p></div>
        </div>
        <div class="qa">
          <button aria-expanded="false"><span class="q">O participante vê pesos, gabarito ou marcadores críticos?</span><span class="ic"></span></button>
          <div class="ans"><p>Nunca. A visão do participante é limpa. Pesos, critérios e marcadores ficam restritos ao painel admin e à trilha de auditoria.</p></div>
        </div>
        <div class="qa">
          <button aria-expanded="false"><span class="q">A Práxis reprova ou elimina alguém sozinha?</span><span class="ic"></span></button>
          <div class="ans"><p>Não. A Práxis recomenda, nunca elimina automaticamente. Erro crítico aciona revisão humana — a decisão final é sempre de uma pessoa.</p></div>
        </div>
        <div class="qa">
          <button aria-expanded="false"><span class="q">Funciona para áreas fora de recrutamento?</span><span class="ic"></span></button>
          <div class="ans"><p>Sim. O motor é o mesmo: cenário situacional com score por competência. Já se aplica a saúde (avaliação clínica e prontidão de pacientes), educação (competências socioemocionais), compliance (conduta em dilemas éticos) e desenvolvimento interno (promoção e sucessão).</p></div>
        </div>
      </div>
    </div>
  </section>

  <!-- FINAL CTA -->
  <section class="final" id="cta">
    <div class="wrap">
      <span class="eyebrow" style="justify-content:center;display:flex">Vamos conversar</span>
      <h2 style="margin-top:1rem">Menos achismo. Mais decisão por evidência.</h2>
      <p class="lead">Mostre como as pessoas decidem antes de tomar qualquer próxima decisão sobre elas. Agende uma demonstração da Práxis.</p>
      <div class="hero-ctas">
        <a class="btn btn-primary" href="mailto:contato@iforce.com.br?subject=Demonstra%C3%A7%C3%A3o%20da%20Pr%C3%A1xis">Agendar demonstração <span class="btn-arrow">→</span></a>
        <a class="btn btn-ghost" href="#precos">Rever os planos</a>
      </div>
    </div>
  </section>

</main>

<footer id="entrar">
  <div class="wrap foot-inner">
    <a href="#topo" class="brand">Práxis<span class="dot"></span><small>by iForce</small></a>
    <span class="fcopy">© 2026 iForce · praxis.iforce.com.br</span>
  </div>
</footer>`;

function LandingPage() {
  useEffect(() => {
    const nav = document.getElementById("nav");
    const menuBtn = document.getElementById("menuBtn");
    const note = document.getElementById("scNote");
    const report = document.getElementById("scReport");
    const rTag = document.getElementById("rTag");
    const rPts = document.getElementById("rPts");
    const rRead = document.getElementById("rRead");
    const comp = document.getElementById("evComp");
    const bM = document.getElementById("bMonthly");
    const bA = document.getElementById("bAnnual");
    const amt = document.querySelector<HTMLElement>(".amt[data-monthly]");
    const cyc = document.getElementById("cycMonthly");
    const sub = document.getElementById("cycSub");

    const cleanups: Array<() => void> = [];

    if (nav) {
      const onScroll = () => nav.classList.toggle("scrolled", window.scrollY > 8);
      onScroll();
      window.addEventListener("scroll", onScroll, { passive: true });
      cleanups.push(() => window.removeEventListener("scroll", onScroll));
    }

    if (nav && menuBtn) {
      const onMenuClick = () => {
        const open = nav.classList.toggle("mobile-open");
        menuBtn.setAttribute("aria-expanded", String(open));
      };
      menuBtn.addEventListener("click", onMenuClick);
      cleanups.push(() => menuBtn.removeEventListener("click", onMenuClick));

      document.querySelectorAll<HTMLAnchorElement>(".nav-links a").forEach((anchor) => {
        const closeMenu = () => {
          nav.classList.remove("mobile-open");
          menuBtn.setAttribute("aria-expanded", "false");
        };
        anchor.addEventListener("click", closeMenu);
        cleanups.push(() => anchor.removeEventListener("click", closeMenu));
      });
    }

    const reads: Record<
      string,
      { tag: string; cls: string; pts: Array<[string, number]>; read: string }
    > = {
      A: {
        tag: "Decis?o registrada",
        cls: "tag-ok",
        pts: [
          ["Comunica??o", 2],
          ["Resolu??o de problemas", 0],
          ["Ader?ncia ? pol?tica", 0],
        ],
        read:
          "<b>Acolhe, mas promete sem garantir.</b> Cria uma expectativa que o time pode n?o conseguir cumprir.",
      },
      B: {
        tag: "Decis?o registrada",
        cls: "tag-ok",
        pts: [
          ["Comunica??o", 3],
          ["Resolu??o de problemas", 2],
          ["Ader?ncia ? pol?tica", 1],
        ],
        read:
          "<b>Acolhe, assume responsabilidade e alinha com o time.</b> A resposta mais equilibrada do turno.",
      },
      C: {
        tag: "Decis?o registrada",
        cls: "tag-ok",
        pts: [
          ["Comunica??o", 1],
          ["Resolu??o de problemas", 0],
          ["Ader?ncia ? pol?tica", 2],
        ],
        read:
          "<b>Tecnicamente correta, por?m fria.</b> Segue a pol?tica, mas ignora um cliente j? irritado.",
      },
      D: {
        tag: "Aciona revis?o humana",
        cls: "tag-rev",
        pts: [
          ["Comunica??o", 0],
          ["Resolu??o de problemas", 0],
          ["Ader?ncia ? pol?tica", 0],
        ],
        read:
          "<b>Esquiva da situa??o.</b> Marcada como decis?o cr?tica ? encaminhada para a revis?o de uma pessoa.",
      },
    };

    document.querySelectorAll<HTMLButtonElement>(".opt").forEach((button) => {
      const onPick = () => {
        document.querySelectorAll(".opt").forEach((option) => option.classList.remove("picked"));
        button.classList.add("picked");
        const key = button.querySelector(".key")?.textContent?.trim() ?? "";
        const data = reads[key];
        if (!data || !note || !report || !rTag || !rPts || !rRead) return;

        note.style.display = "none";
        rTag.textContent = data.tag;
        rTag.className = "tag " + data.cls;
        rPts.innerHTML = data.pts
          .map(
            ([name, value]) =>
              '<div class="ptrow"><span class="pn">' +
              name +
              '</span><span class="pt-track"><span class="pt-fill"></span></span><span class="pv' +
              (value === 0 ? " zero" : "") +
              '">+' +
              value +
              "</span></div>",
          )
          .join("");
        rRead.innerHTML = data.read;
        report.classList.add("show");
        requestAnimationFrame(() =>
          requestAnimationFrame(() => {
            rPts.querySelectorAll<HTMLElement>(".pt-fill").forEach((fill, index) => {
              fill.style.width = (data.pts[index][1] / 3) * 100 + "%";
            });
          }),
        );
      };
      button.addEventListener("click", onPick);
      cleanups.push(() => button.removeEventListener("click", onPick));
    });

    if (comp) {
      const fillBars = () => {
        comp.querySelectorAll<HTMLElement>(".fill").forEach((fill) => {
          fill.style.width = (fill.dataset.w ?? "0") + "%";
        });
      };

      if ("IntersectionObserver" in window) {
        const observer = new IntersectionObserver(
          (entries) => {
            entries.forEach((entry) => {
              if (entry.isIntersecting) {
                fillBars();
                observer.disconnect();
              }
            });
          },
          { threshold: 0.4 },
        );
        observer.observe(comp);
        cleanups.push(() => observer.disconnect());
      } else {
        fillBars();
      }
    }

    if (bM && bA && amt && cyc && sub) {
      const setCycle = (annual: boolean) => {
        bM.classList.toggle("on", !annual);
        bA.classList.toggle("on", annual);
        bM.setAttribute("aria-pressed", String(!annual));
        bA.setAttribute("aria-pressed", String(annual));
        amt.textContent = annual ? (amt.dataset.annual ?? "") : (amt.dataset.monthly ?? "");
        cyc.textContent = annual ? "/ m?s, no anual" : "/ m?s";
        sub.textContent = annual
          ? "R$ 33,25/avaliação · 200 inclusas/mês · faturado anual (R$ 79.900)"
          : "R$ 39,90/avaliação · 200 inclusas/mês · +R$ 49,90 por adicional";
      };
      const monthly = () => setCycle(false);
      const annual = () => setCycle(true);
      bM.addEventListener("click", monthly);
      bA.addEventListener("click", annual);
      cleanups.push(() => bM.removeEventListener("click", monthly));
      cleanups.push(() => bA.removeEventListener("click", annual));
    }

    document.querySelectorAll<HTMLButtonElement>(".qa button").forEach((button) => {
      const onToggle = () => {
        const qa = button.parentElement;
        const ans = qa?.querySelector<HTMLElement>(".ans");
        if (!qa || !ans) return;
        const open = qa.classList.toggle("open");
        button.setAttribute("aria-expanded", String(open));
        ans.style.maxHeight = open ? ans.scrollHeight + "px" : "0";
      };
      button.addEventListener("click", onToggle);
      cleanups.push(() => button.removeEventListener("click", onToggle));
    });

    return () => cleanups.forEach((cleanup) => cleanup());
  }, []);

  return (
    <>
      <style>{landingStyles}</style>
      <div dangerouslySetInnerHTML={{ __html: landingMarkup }} />
    </>
  );
}
