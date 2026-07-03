package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.billing.dto.BillingEventResponse;

import br.com.iforce.praxis.billing.dto.ClientBillingResponse;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaBillingEventRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaSubscriptionRepository;

import br.com.iforce.praxis.gupy.model.AttemptStatus;

import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;

import org.springframework.data.domain.PageRequest;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.ArrayList;

import java.util.List;


/**
 * Página de cobrança que o próprio cliente vê (a empresa logada olhando a sua conta).
 *
 * <p>Enquanto o {@link BillingService} é o motor que movimenta o dinheiro e o painel do ADMIN
 * enxerga qualquer cliente, este serviço monta a visão que o <em>próprio</em> cliente tem da sua
 * situação: qual plano contratou, se está regular, quanto de crédito tem, quanto vem usando e o
 * histórico das últimas movimentações financeiras. Também sugere quais botões fazem sentido para
 * ele agora (comprar créditos, ver a assinatura, atualizar o pagamento...). É tudo leitura — este
 * serviço não cobra nem altera nada.</p>
 */
@Service
public class ClientBillingService {

    private final EmpresaRepository empresaRepository;
    private final EmpresaSubscriptionRepository subscriptionRepository;
    private final EmpresaBillingEventRepository eventRepository;
    private final CandidateAttemptRepository attemptRepository;
    private final CreditService creditService;

    public ClientBillingService(EmpresaRepository empresaRepository,
                                EmpresaSubscriptionRepository subscriptionRepository,
                                EmpresaBillingEventRepository eventRepository,
                                CandidateAttemptRepository attemptRepository,
                                CreditService creditService) {
        this.empresaRepository = empresaRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.attemptRepository = attemptRepository;
        this.creditService = creditService;
    }

    /**
     * Monta a tela de "Plano e cobrança" do cliente logado.
     *
     * <p>Reúne, em uma resposta só, tudo o que o cliente precisa ver sobre a própria conta: o plano
     * contratado, a situação financeira em linguagem simples (regular, pendente, inadimplente, sem
     * crédito ou cancelado), o saldo de créditos, um resumo de uso (avaliações concluídas nos
     * últimos 7 e 30 dias e no total), os dados da assinatura quando houver, as ações sugeridas para
     * ele e o histórico recente de eventos financeiros. É apenas leitura.</p>
     *
     * @param empresaId identificador do cliente logado
     * @return a visão consolidada de plano, situação, saldo, uso, assinatura, ações e histórico
     * @throws ResponseStatusException se o cliente não for encontrado
     */
    @Transactional(readOnly = true)
    public ClientBillingResponse getBilling(String empresaId) {
        EmpresaEntity empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));

        Instant now = Instant.now();
        long last7 = attemptRepository.countByEmpresaIdAndStatusAndFinishedAtAfter(
                empresaId, AttemptStatus.COMPLETED, now.minus(7, ChronoUnit.DAYS));
        long last30 = attemptRepository.countByEmpresaIdAndStatusAndFinishedAtAfter(
                empresaId, AttemptStatus.COMPLETED, now.minus(30, ChronoUnit.DAYS));
        long allTime = attemptRepository.countByEmpresaIdAndStatus(empresaId, AttemptStatus.COMPLETED);

        var subscription = subscriptionRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId).orElse(null);

        ClientBillingResponse.SubscriptionInfo subscriptionInfo = subscription == null ? null
                : new ClientBillingResponse.SubscriptionInfo(
                subscription.getStatus(),
                subscription.getCurrentPeriodEnd(),
                subscription.getLastPaymentAt(),
                subscription.getGraceUntil());

        List<String> actions = resolveActions(empresa.getCommercialPlanType(), empresa.getStatus(),
                creditService.getBalance(empresaId));

        List<BillingEventResponse> events = eventRepository
                .findByEmpresaIdOrderByCreatedAtDesc(empresaId, PageRequest.of(0, 50)).stream()
                .map(e -> new BillingEventResponse(e.getId(), e.getEventType(), e.getMpResourceType(),
                        e.getMpResourceId(), e.getMpStatus(), e.getAmountCents(), e.getCurrency(), e.getCreatedAt()))
                .toList();

        return new ClientBillingResponse(
                empresaId,
                empresa.getCommercialPlanType(),
                empresa.getStatus(),
                financialStatusLabel(empresa.getStatus()),
                creditService.getBalance(empresaId),
                new ClientBillingResponse.UsageSummary(last7, last30, allTime),
                subscriptionInfo,
                actions,
                events);
    }

    /**
     * Lista o histórico de eventos financeiros do cliente logado.
     *
     * <p>É o "extrato" que o cliente abre para conferir suas movimentações: compras de crédito,
     * cobranças de mensalidade, estornos e afins, da mais recente para a mais antiga. Serve à tela
     * de histórico e é apenas leitura.</p>
     *
     * @param empresaId identificador do cliente logado
     * @return os eventos financeiros mais recentes, do mais novo para o mais antigo
     */
    @Transactional(readOnly = true)
    public List<BillingEventResponse> getEvents(String empresaId) {
        return eventRepository
                .findByEmpresaIdOrderByCreatedAtDesc(empresaId, PageRequest.of(0, 100)).stream()
                .map(e -> new BillingEventResponse(e.getId(), e.getEventType(), e.getMpResourceType(),
                        e.getMpResourceId(), e.getMpStatus(), e.getAmountCents(), e.getCurrency(), e.getCreatedAt()))
                .toList();
    }

    /**
     * Decide quais ações fazem sentido oferecer ao cliente conforme o plano e a situação: quem é
     * pré-pago vê "comprar créditos"; quem é assinante vê "ver assinatura" e, se estiver
     * inadimplente, "atualizar pagamento"; quem é enterprise é direcionado ao suporte. É o que
     * transforma a situação financeira nos botões certos da tela. Uso interno.
     */
    private static List<String> resolveActions(CommercialPlanType plan, EmpresaStatus status, int creditBalance) {
        List<String> actions = new ArrayList<>();
        if (plan == CommercialPlanType.AVULSO) {
            actions.add("BUY_CREDITS");
            actions.add("VIEW_HISTORY");
        } else if (plan == CommercialPlanType.PROFISSIONAL) {
            actions.add("VIEW_SUBSCRIPTION");
            if (status == EmpresaStatus.INADIMPLENTE || status == EmpresaStatus.PENDENTE_PAGAMENTO) {
                actions.add("UPDATE_PAYMENT");
            }
        } else if (plan == CommercialPlanType.ENTERPRISE) {
            actions.add("CONTACT_SUPPORT");
        }
        return actions;
    }

    /**
     * Traduz a situação interna do cliente em um rótulo financeiro simples para exibir na tela
     * (regular, pendente de pagamento, inadimplente, sem crédito ou cancelado). Uso interno.
     */
    private static String financialStatusLabel(EmpresaStatus status) {
        return switch (status) {
            case ATIVO, EM_TESTE -> "REGULAR";
            case PENDENTE_PAGAMENTO -> "PENDENTE_PAGAMENTO";
            case INADIMPLENTE -> "INADIMPLENTE";
            case SEM_CREDITO -> "SEM_CREDITO";
            case SUSPENSO, CANCELADO -> "CANCELADO";
        };
    }
}
