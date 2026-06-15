const errorPageHtml = `<!doctype html>
<html lang="pt-BR">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Esta página não carregou</title>
    <style>
      body {
        margin: 0;
        min-height: 100vh;
        display: grid;
        place-items: center;
        font-family: Inter, system-ui, sans-serif;
        color: #172033;
        background: #f8fafc;
      }
      main {
        max-width: 420px;
        padding: 24px;
        text-align: center;
      }
      h1 {
        margin: 0;
        font-size: 24px;
      }
      p {
        color: #5b6475;
      }
      .actions {
        display: flex;
        justify-content: center;
        gap: 8px;
        margin-top: 20px;
      }
      a,
      button {
        border-radius: 8px;
        border: 1px solid #d7dce5;
        padding: 10px 14px;
        font: inherit;
      }
      button {
        background: #315fdc;
        color: white;
        border-color: #315fdc;
      }
      a {
        color: #172033;
        text-decoration: none;
        background: white;
      }
      :focus-visible {
        outline: 3px solid #315fdc;
        outline-offset: 2px;
      }
    </style>
  </head>
  <body>
    <main>
      <h1>Esta página não carregou</h1>
      <p>Algo deu errado. Tente novamente ou volte ao painel.</p>
      <div class="actions">
        <button class="primary" onclick="location.reload()">Tentar novamente</button>
        <a class="secondary" href="/">Voltar ao painel</a>
      </div>
    </main>
  </body>
</html>`;

export function renderErrorPage() {
  return errorPageHtml;
}
