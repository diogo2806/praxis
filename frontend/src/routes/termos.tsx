import { createFileRoute } from "@tanstack/react-router";
import { ArrowLeft, FileText } from "lucide-react";

export const Route = createFileRoute("/termos")({ component: TermsPage });

function TermsPage() {
  return (
    <main className="min-h-screen bg-background px-4 py-10 text-foreground">
      <article className="mx-auto max-w-3xl rounded-2xl border border-border bg-card p-6 shadow-sm sm:p-10">
        <a href="/" className="inline-flex items-center gap-2 text-sm text-primary hover:underline">
          <ArrowLeft className="h-4 w-4" aria-hidden /> Voltar
        </a>
        <div className="mt-8 flex items-center gap-3">
          <FileText className="h-6 w-6 text-primary" aria-hidden />
          <h1 className="text-3xl font-semibold">Termos de Uso do Práxis</h1>
        </div>
        <p className="mt-3 text-sm text-muted-foreground">Versão 1.1 · 21 de julho de 2026</p>

        <div className="mt-8 space-y-7 text-sm leading-7 text-muted-foreground">
          <section>
            <h2 className="text-lg font-semibold text-foreground">Serviço</h2>
            <p>O Práxis oferece avaliações situacionais determinísticas para apoiar processos de recrutamento. Não é teste psicológico, não realiza diagnóstico e não substitui entrevista ou decisão humana.</p>
          </section>
          <section>
            <h2 className="text-lg font-semibold text-foreground">Uso pela pessoa candidata</h2>
            <p>O link de participação é pessoal e deve ser utilizado somente pela pessoa convidada. A pessoa candidata deve responder diretamente às situações apresentadas, não compartilhar o acesso, não tentar contornar controles técnicos e não utilizar automações ou terceiros para executar a avaliação. O aceite destes termos não garante contratação, aprovação ou resultado mínimo.</p>
          </section>
          <section>
            <h2 className="text-lg font-semibold text-foreground">Antes de iniciar a avaliação</h2>
            <p>A avaliação somente é liberada depois que a pessoa candidata marca separadamente que leu e aceitou estes Termos de Uso e que leu e está ciente da Política de Privacidade aplicável. A versão e a integridade dos dois documentos, o idioma e o momento do aceite são registrados para auditoria.</p>
          </section>
          <section>
            <h2 className="text-lg font-semibold text-foreground">Responsabilidades da empresa cliente</h2>
            <p>A empresa cliente define finalidade, critérios, base legal, prazo de retenção e pessoas autorizadas. Deve garantir revisão humana em decisões relevantes, não utilizar o resultado de forma discriminatória e manter os dados limitados ao processo informado à pessoa candidata.</p>
          </section>
          <section>
            <h2 className="text-lg font-semibold text-foreground">Uso aceitável</h2>
            <p>É proibido usar o serviço para finalidade ilícita, discriminatória, invasiva ou incompatível com o cargo, tentar acessar dados de outra empresa, burlar controles de segurança ou reutilizar resultados fora da finalidade contratada.</p>
          </section>
          <section>
            <h2 className="text-lg font-semibold text-foreground">Resultados e revisão</h2>
            <p>As pontuações decorrem de alternativas, pesos e critérios definidos previamente. Resultados sinalizados para revisão permanecem bloqueados para entrega final até a conclusão por usuário autorizado.</p>
          </section>
          <section>
            <h2 className="text-lg font-semibold text-foreground">Cobrança e cancelamento</h2>
            <p>Preço, periodicidade, créditos, renovação, parcelamento, cancelamento, reembolso e eventuais condições especiais seguem a proposta comercial e o checkout apresentados antes da contratação. Pagamentos são processados pelo Mercado Pago. Cobranças recorrentes e recargas automáticas devem ser autorizadas expressamente e podem ser canceladas pelo canal de atendimento.</p>
          </section>
          <section>
            <h2 className="text-lg font-semibold text-foreground">Privacidade</h2>
            <p>O tratamento de dados segue a Política de Privacidade, as informações específicas apresentadas antes da participação e o contrato de tratamento de dados firmado com a empresa cliente. Pedidos de titulares e revisão humana podem ser registrados pelo link da participação.</p>
          </section>
          <section>
            <h2 className="text-lg font-semibold text-foreground">Disponibilidade e responsabilidade</h2>
            <p>O serviço é prestado conforme o plano contratado e os níveis de serviço acordados. Nenhuma funcionalidade garante contratação, desempenho profissional futuro ou ausência total de indisponibilidade. Limitações de responsabilidade respeitam a legislação aplicável.</p>
          </section>
          <section>
            <h2 className="text-lg font-semibold text-foreground">Contato e dados cadastrais</h2>
            <p>A fornecedora é a iForce, com razão social, CNPJ, endereço, canal de suporte e foro informados na proposta e no contrato comercial. Divergências entre esta página e o contrato são resolvidas pelo instrumento contratual vigente, sem reduzir direitos legais.</p>
          </section>
        </div>
      </article>
    </main>
  );
}
