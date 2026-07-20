import { createFileRoute } from "@tanstack/react-router";
import { ArrowLeft, Network } from "lucide-react";

const subprocessors = [
  { name: "Provedor de infraestrutura configurado pela iForce", purpose: "Hospedagem da aplicação, banco, rede e backup", location: "Conforme contrato e região configurada" },
  { name: "Mercado Pago", purpose: "Checkout, assinaturas, pagamentos e prevenção a fraude", location: "Brasil e infraestrutura informada pelo fornecedor" },
  { name: "Gupy", purpose: "Criação de participações e recebimento de resultados quando a integração estiver habilitada", location: "Conforme contrato entre a empresa cliente e a Gupy" },
  { name: "Recrutei", purpose: "Integração ATS quando habilitada pela empresa cliente", location: "Conforme contrato entre a empresa cliente e o fornecedor" },
  { name: "Armazenamento de objetos configurado", purpose: "Mídias das avaliações, quando utilizado", location: "Região definida na configuração operacional" },
];

export const Route = createFileRoute("/suboperadores")({ component: SubprocessorsPage });

function SubprocessorsPage() {
  return (
    <main className="min-h-screen bg-background px-4 py-10 text-foreground">
      <article className="mx-auto max-w-4xl rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-10">
        <a href="/" className="inline-flex items-center gap-2 text-sm text-primary hover:underline">
          <ArrowLeft className="h-4 w-4" aria-hidden /> Voltar
        </a>
        <div className="mt-8 flex items-center gap-3">
          <Network className="h-6 w-6 text-primary" aria-hidden />
          <h1 className="text-3xl font-semibold">Suboperadores e compartilhamentos</h1>
        </div>
        <p className="mt-3 text-sm text-muted-foreground">Versão 1.0 · 20 de julho de 2026</p>
        <p className="mt-6 text-sm leading-7 text-muted-foreground">
          Somente fornecedores necessários ao serviço e integrações habilitadas pela empresa cliente recebem dados. O contrato e o DPA devem identificar o fornecedor efetivamente utilizado, sua região e o mecanismo aplicável a eventual transferência internacional.
        </p>

        <div className="mt-8 overflow-x-auto">
          <table className="w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-border text-foreground">
                <th className="px-3 py-3">Fornecedor/categoria</th>
                <th className="px-3 py-3">Finalidade</th>
                <th className="px-3 py-3">Local</th>
              </tr>
            </thead>
            <tbody>
              {subprocessors.map((item) => (
                <tr key={item.name} className="border-b border-border align-top text-muted-foreground">
                  <td className="px-3 py-4 font-medium text-foreground">{item.name}</td>
                  <td className="px-3 py-4">{item.purpose}</td>
                  <td className="px-3 py-4">{item.location}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>

        <section className="mt-8 rounded-xl border border-border bg-background/60 p-4 text-sm leading-6 text-muted-foreground">
          <h2 className="font-semibold text-foreground">Governança</h2>
          <p className="mt-2">A lista contratual deve ser atualizada antes de habilitar novo fornecedor. Quando houver tratamento fora do Brasil, o mecanismo de transferência e as salvaguardas devem constar do DPA e do contrato do fornecedor.</p>
        </section>
      </article>
    </main>
  );
}
