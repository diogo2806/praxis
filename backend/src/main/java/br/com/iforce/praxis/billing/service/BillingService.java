package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.billing.config.MercadoPagoProperties;

import br.com.iforce.praxis.billing.dto.BillingEventResponse;

import br.com.iforce.praxis.billing.dto.CheckoutResponse;

import br.com.iforce.praxis.billing.dto.EmpresaBillingOverviewResponse;

import br.com.iforce.praxis.billing.model.AutoRechargeStatus;

import br.com.iforce.praxis.billing.model.BillingEventType;

import br.com.iforce.praxis.billing.model.SubscriptionStatus;

import br.com.iforce.praxis.billing.persistence.entity.SubscriptionPlanEntity;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaBillingEventEntity;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaSubscriptionEntity;

import br.com.iforce.praxis.billing.persistence.repository.SubscriptionPlanRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaAutoRechargeConfigRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaBillingEventRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaSubscriptionRepository;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.data.domain.PageRequest;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.time.ZoneOffset;

import java.time.temporal.ChronoUnit;

import java.util.List;

import java.util.Optional;

import java.util.UUID;


/**
 * Maestro da cobrança via Mercado Pago — o serviço central que conduz todo o dinheiro do cliente.
 *
 * <p>Na visão do processo, é aqui que a plataforma cria as cobranças (um pacote de créditos ou uma
 * assinatura mensal), escuta o que o Mercado Pago responde sobre cada pagamento e traduz isso em
 * consequências reais: somar créditos, ativar a assinatura, liberar ou bloquear o acesso do
 * cliente. Este serviço não desenha telas nem calcula notas de candidato; ele só cuida da relação
 * financeira entre a empresa cliente e a plataforma.</p>
 *
 * <p><b>Princípio inegociável:</b> o Mercado Pago é a fonte da verdade financeira. Nenhum pagamento
 * é dado como aprovado porque alguém clicou num botão — toda confirmação nasce de uma consulta à
 * API do Mercado Pago, disparada por uma notificação (webhook) ou por uma sincronização manual do
 * ADMIN. Além disso, cada efeito financeiro vira um evento que só acrescenta e nunca apaga
 * (append-only) e é idempotente por recurso do Mercado Pago — ou seja, se a mesma notificação
 * chegar duas vezes, o crédito não é lançado em dobro.</p>
 */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final String KIND_CREDIT = "credit";
    private static final String KIND_SUB = "sub";
    /** Etiqueta de uma compra de créditos originada de recarga automática (cartão salvo). */
    static final String KIND_AUTO_CREDIT = "autocredit";

    private final MercadoPagoClient mercadoPagoClient;
    private final MercadoPagoProperties properties;
    private final SubscriptionPlanRepository planRepository;
    private final EmpresaBillingEventRepository eventRepository;
    private final EmpresaSubscriptionRepository subscriptionRepository;
    private final CreditService creditService;
    private final EmpresaRepository empresaRepository;
    private final BillingDunningService dunningService;
    private final EmpresaAutoRechargeConfigRepository autoRechargeConfigRepository;

    public BillingService(
            MercadoPagoClient mercadoPagoClient,
            MercadoPagoProperties properties,
            SubscriptionPlanRepository planRepository,
            EmpresaBillingEventRepository eventRepository,
            EmpresaSubscriptionRepository subscriptionRepository,
            CreditService creditService,
            EmpresaRepository empresaRepository,
            BillingDunningService dunningService,
            EmpresaAutoRechargeConfigRepository autoRechargeConfigRepository
    ) {
        this.mercadoPagoClient = mercadoPagoClient;
        this.properties = properties;
        this.planRepository = planRepository;
        this.eventRepository = eventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.creditService = creditService;
        this.empresaRepository = empresaRepository;
        this.dunningService = dunningService;
        this.autoRechargeConfigRepository = autoRechargeConfigRepository;
    }

    // ------------------------------------------------------------------
    // Criação de cobranças (ADMIN)
    // ------------------------------------------------------------------

    /**
     * Abre a compra de um pacote de créditos (plano AVULSO).
     *
     * <p>Fluxo do processo: o ADMIN escolhe um cliente e um pacote de créditos; o sistema confere
     * que o pacote realmente é do tipo pré-pago (AVULSO) e pede ao Mercado Pago um "checkout" — a
     * página onde o cliente vai efetivamente pagar. O pedido é registrado como um evento financeiro
     * e o link de pagamento é devolvido para ser enviado ao cliente. Repare que aqui <em>nada</em>
     * de crédito é adicionado ainda: os créditos só entram quando o pagamento for confirmado pelo
     * Mercado Pago.</p>
     *
     * @param empresaId identificador do cliente que vai comprar os créditos
     * @param planId identificador do pacote de créditos escolhido
     * @return o checkout criado, com o link de pagamento do Mercado Pago
     * @throws ResponseStatusException se o cliente ou o pacote não existirem, ou se o pacote não for
     *         um pacote de créditos AVULSO válido
     */
    @Transactional
    public CheckoutResponse createCreditCheckout(String empresaId, Long planId) {
        requireEmpresa(empresaId);
        SubscriptionPlanEntity plan = requirePlan(planId);
        if (plan.getPlanType() != CommercialPlanType.AVULSO || plan.getCreditAmount() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano não é um pacote de créditos AVULSO.");
        }
        String externalReference = reference(KIND_CREDIT, empresaId, planId);
        JsonNode response = mercadoPagoClient.createCreditPreference(plan, externalReference,
                java.util.Map.of("empresa_id", empresaId, "plan_id", planId, "kind", KIND_CREDIT));

        String preferenceId = text(response, "id");
        String initPoint = text(response, "init_point");
        recordEvent(empresaId, BillingEventType.CREDIT_CHECKOUT_CREATED, "preference", preferenceId,
                externalReference, "created", plan.getPriceCents(), plan.getCurrency(), null, response);

        return new CheckoutResponse(KIND_CREDIT, preferenceId, initPoint, externalReference);
    }

    /**
     * Abre uma assinatura mensal recorrente (plano PROFISSIONAL).
     *
     * <p>Fluxo do processo: o ADMIN contrata a mensalidade para um cliente. Antes de criar qualquer
     * coisa, o sistema garante que o cliente ainda não tem uma assinatura ativa, pendente ou
     * inadimplente — para não gerar cobrança duplicada — e confere que o plano escolhido é mesmo do
     * tipo PROFISSIONAL. Em seguida pede ao Mercado Pago para criar a assinatura recorrente, guarda
     * essa assinatura como "pendente" (aguardando o primeiro pagamento), registra o evento e, se o
     * cliente ainda não estava bloqueado, marca-o como "aguardando pagamento". O link de pagamento é
     * devolvido para o cliente autorizar a cobrança recorrente.</p>
     *
     * @param empresaId identificador do cliente que vai assinar
     * @param planId identificador do plano de assinatura escolhido
     * @return o checkout criado, com o link para o cliente autorizar a assinatura no Mercado Pago
     * @throws ResponseStatusException se o cliente já tiver assinatura ativa/pendente/inadimplente,
     *         se o cliente ou o plano não existirem, ou se o plano não for PROFISSIONAL
     */
    @Transactional
    public CheckoutResponse createSubscription(String empresaId, Long planId) {
        EmpresaEntity empresa = requireEmpresa(empresaId);
        SubscriptionPlanEntity plan = requirePlan(planId);
        subscriptionRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId)
                .filter(sub -> sub.getStatus() == SubscriptionStatus.PENDING
                        || sub.getStatus() == SubscriptionStatus.AUTHORIZED
                        || sub.getStatus() == SubscriptionStatus.DELINQUENT)
                .ifPresent(sub -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Cliente já possui assinatura ativa, pendente ou inadimplente.");
                });
        if (plan.getPlanType() != CommercialPlanType.PROFISSIONAL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano não é uma assinatura PROFISSIONAL.");
        }
        String externalReference = reference(KIND_SUB, empresaId, planId);
        JsonNode response = mercadoPagoClient.createPreapproval(plan, empresa.getCorporateEmail(), externalReference);

        String preapprovalId = text(response, "id");
        String initPoint = text(response, "init_point");

        EmpresaSubscriptionEntity subscription = new EmpresaSubscriptionEntity();
        subscription.setEmpresaId(empresaId);
        subscription.setPlanId(planId);
        subscription.setMpPreapprovalId(preapprovalId);
        subscription.setStatus(SubscriptionStatus.PENDING);
        subscription.setInitPoint(initPoint);
        subscription.setCreatedAt(Instant.now());
        subscription.setUpdatedAt(Instant.now());
        subscriptionRepository.save(subscription);

        recordEvent(empresaId, BillingEventType.SUBSCRIPTION_CREATED, "preapproval", preapprovalId,
                externalReference, "pending", plan.getPriceCents(), plan.getCurrency(), null, response);

        if (!empresa.getStatus().blocksAccess()) {
            empresa.setStatus(EmpresaStatus.PENDENTE_PAGAMENTO);
            empresa.setUpdatedAt(Instant.now());
        }
        return new CheckoutResponse(KIND_SUB, preapprovalId, initPoint, externalReference);
    }

    // ------------------------------------------------------------------
    // Processamento de notificações (consulta o MP antes de aplicar)
    // ------------------------------------------------------------------

    /**
     * Trata um aviso de pagamento vindo do Mercado Pago e aplica o efeito financeiro correspondente.
     *
     * <p>Este é o momento em que o dinheiro "acontece de verdade". Fluxo do processo: em vez de
     * confiar no aviso recebido, o sistema consulta o pagamento real no Mercado Pago (a fonte da
     * verdade) e olha o resultado:</p>
     * <ul>
     *   <li><b>aprovado</b> — se era compra de créditos, soma os créditos ao saldo do cliente; se
     *       era mensalidade, marca a assinatura como paga e reativa o acesso;</li>
     *   <li><b>recusado ou cancelado</b> — se era mensalidade, inicia a inadimplência (carência);</li>
     *   <li><b>estornado ou contestado (chargeback)</b> — apenas registra o fato para a trilha
     *       financeira;</li>
     *   <li><b>qualquer outro estado</b> — registra como pagamento pendente.</li>
     * </ul>
     *
     * <p>Tudo é idempotente: se o mesmo pagamento aprovado chegar duas vezes, o crédito é lançado
     * uma vez só. Avisos sem cliente identificável são ignorados com segurança.</p>
     *
     * @param paymentId identificador do pagamento no Mercado Pago a ser consultado
     * @param requestId identificador de rastreio da requisição (para a trilha financeira), pode ser nulo
     */
    @Transactional
    public void processPaymentNotification(String paymentId, String requestId) {
        JsonNode payment = mercadoPagoClient.getPayment(paymentId);
        String status = text(payment, "status");
        String externalReference = text(payment, "external_reference");
        Long amountCents = cents(payment);
        String[] ref = parseReference(externalReference);
        String kind = ref[0];
        String empresaId = ref[1];
        Long planId = ref[2] == null ? null : Long.valueOf(ref[2]);

        if (empresaId == null) {
            log.warn("Pagamento {} sem external_reference atribuível; ignorado.", paymentId);
            return;
        }

        if ("approved".equals(status)) {
            if (KIND_CREDIT.equals(kind) || KIND_AUTO_CREDIT.equals(kind)) {
                boolean autoRecharge = KIND_AUTO_CREDIT.equals(kind);
                if (eventRepository.existsByMpResourceIdAndEventType(paymentId, BillingEventType.CREDIT_PURCHASE_APPROVED)) {
                    return;
                }
                SubscriptionPlanEntity plan = requirePlan(planId);
                EmpresaBillingEventEntity event = recordEvent(empresaId, BillingEventType.CREDIT_PURCHASE_APPROVED,
                        "payment", paymentId, externalReference, status, amountCents, "BRL", requestId, payment);
                String note = (autoRecharge ? "Recarga automática confirmada" : "Compra de créditos confirmada")
                        + " (pagamento " + paymentId + ")";
                creditService.addCredits(empresaId, plan.getCreditAmount(), event.getId(), note);
                if (autoRecharge) {
                    // Recarga aprovada: libera o ciclo para uma futura recarga quando o saldo cair de novo.
                    releaseAutoRecharge(empresaId, "Recarga automática aprovada (pagamento " + paymentId + ").");
                }
            } else if (KIND_SUB.equals(kind)) {
                if (eventRepository.existsByMpResourceIdAndEventType(paymentId, BillingEventType.SUBSCRIPTION_PAYMENT_APPROVED)) {
                    return;
                }
                EmpresaBillingEventEntity event = recordEvent(empresaId, BillingEventType.SUBSCRIPTION_PAYMENT_APPROVED,
                        "payment", paymentId, externalReference, status, amountCents, "BRL", requestId, payment);
                markSubscriptionPaid(empresaId);
                grantSubscriptionCredits(empresaId, planId, event.getId(), paymentId);
            }
        } else if ("rejected".equals(status) || "cancelled".equals(status)) {
            if (KIND_SUB.equals(kind)) {
                recordEvent(empresaId, BillingEventType.SUBSCRIPTION_PAYMENT_REJECTED, "payment", paymentId,
                        externalReference, status, amountCents, "BRL", requestId, payment);
                startDelinquency(empresaId);
            } else if (KIND_AUTO_CREDIT.equals(kind)) {
                // Cartão recusado na recarga automática: registra a falha e libera o ciclo (a janela
                // de espera evita retentativa imediata sobre o mesmo cartão recusado).
                recordEvent(empresaId, BillingEventType.CREDIT_AUTO_RECHARGE_FAILED, "payment", paymentId,
                        externalReference, status, amountCents, "BRL", requestId, payment);
                releaseAutoRecharge(empresaId, "Recarga automática recusada (pagamento " + paymentId + ").");
            } else {
                recordEvent(empresaId, BillingEventType.PAYMENT_PENDING, "payment", paymentId,
                        externalReference, status, amountCents, "BRL", requestId, payment);
            }
        } else if ("refunded".equals(status)) {
            recordEvent(empresaId, BillingEventType.PAYMENT_REFUNDED, "payment", paymentId,
                    externalReference, status, amountCents, "BRL", requestId, payment);
        } else if ("charged_back".equals(status)) {
            recordEvent(empresaId, BillingEventType.PAYMENT_CHARGEBACK, "payment", paymentId,
                    externalReference, status, amountCents, "BRL", requestId, payment);
        } else {
            recordEvent(empresaId, BillingEventType.PAYMENT_PENDING, "payment", paymentId,
                    externalReference, status, amountCents, "BRL", requestId, payment);
        }
    }

    /**
     * Trata um aviso sobre a assinatura (preapproval) vindo do Mercado Pago.
     *
     * <p>Enquanto o método de pagamento cuida de cada cobrança individual, este cuida do
     * "contrato" da mensalidade em si. Fluxo do processo: o sistema consulta a assinatura no
     * Mercado Pago, localiza a assinatura correspondente do cliente e atualiza a situação dela:</p>
     * <ul>
     *   <li><b>autorizada</b> — a assinatura passa a valer, some qualquer prazo de carência e o
     *       acesso do cliente é reativado;</li>
     *   <li><b>pausada</b> — a assinatura fica pausada no Mercado Pago;</li>
     *   <li><b>cancelada</b> — a assinatura é encerrada.</li>
     * </ul>
     *
     * <p>Avisos sobre assinaturas que não conseguem ser vinculadas a nenhum cliente são ignorados
     * com segurança.</p>
     *
     * @param preapprovalId identificador da assinatura no Mercado Pago a ser consultada
     * @param requestId identificador de rastreio da requisição (para a trilha financeira), pode ser nulo
     */
    @Transactional
    public void processPreapprovalNotification(String preapprovalId, String requestId) {
        JsonNode preapproval = mercadoPagoClient.getPreapproval(preapprovalId);
        String status = text(preapproval, "status");
        EmpresaSubscriptionEntity subscription = subscriptionRepository.findByMpPreapprovalId(preapprovalId)
                .orElse(null);
        if (subscription == null) {
            String[] ref = parseReference(text(preapproval, "external_reference"));
            if (ref[1] == null) {
                log.warn("Preapproval {} desconhecido e sem referência; ignorado.", preapprovalId);
                return;
            }
            subscription = subscriptionRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(ref[1]).orElse(null);
            if (subscription == null) {
                return;
            }
        }
        String empresaId = subscription.getEmpresaId();

        if ("authorized".equals(status)) {
            subscription.setStatus(SubscriptionStatus.AUTHORIZED);
            subscription.setGraceUntil(null);
            subscription.setUpdatedAt(Instant.now());
            recordEvent(empresaId, BillingEventType.SUBSCRIPTION_AUTHORIZED, "preapproval", preapprovalId,
                    null, status, null, null, requestId, preapproval);
            activateEmpresa(empresaId);
        } else if ("paused".equals(status)) {
            subscription.setStatus(SubscriptionStatus.PAUSED);
            subscription.setUpdatedAt(Instant.now());
            recordEvent(empresaId, BillingEventType.PAYMENT_PENDING, "preapproval", preapprovalId,
                    null, status, null, null, requestId, preapproval);
        } else if ("cancelled".equals(status)) {
            subscription.setStatus(SubscriptionStatus.CANCELLED);
            subscription.setUpdatedAt(Instant.now());
            recordEvent(empresaId, BillingEventType.SUBSCRIPTION_CANCELLED, "preapproval", preapprovalId,
                    null, status, null, null, requestId, preapproval);
        }
    }

    /**
     * Refaz manualmente a leitura de um recurso do Mercado Pago (rede de segurança do ADMIN).
     *
     * <p>Fluxo do processo: às vezes uma notificação automática se perde no caminho. Para não
     * deixar o cliente com a situação errada, o ADMIN pode pedir ao sistema que vá conferir de novo,
     * diretamente no Mercado Pago, um pagamento ou uma assinatura específicos. O sistema apenas
     * encaminha para o mesmo tratamento das notificações automáticas — ou seja, também consulta a
     * fonte da verdade antes de aplicar qualquer efeito, e continua sendo idempotente.</p>
     *
     * @param resourceType tipo do recurso a reconferir: {@code payment} (pagamento) ou
     *        {@code preapproval}/{@code subscription} (assinatura)
     * @param resourceId identificador do recurso no Mercado Pago
     * @param requestId identificador de rastreio da requisição (para a trilha financeira), pode ser nulo
     * @throws ResponseStatusException se o tipo de recurso informado não for reconhecido
     */
    @Transactional
    public void manualSync(String resourceType, String resourceId, String requestId) {
        if ("payment".equalsIgnoreCase(resourceType)) {
            processPaymentNotification(resourceId, requestId);
        } else if ("preapproval".equalsIgnoreCase(resourceType) || "subscription".equalsIgnoreCase(resourceType)) {
            processPreapprovalNotification(resourceId, requestId);
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tipo de recurso inválido para sincronização.");
        }
    }

    // ------------------------------------------------------------------
    // Visão consolidada
    // ------------------------------------------------------------------

    /**
     * Monta a visão consolidada de cobrança de um cliente para o painel do ADMIN.
     *
     * <p>Reúne, em um só lugar, tudo o que o ADMIN precisa para entender a situação financeira do
     * cliente: o plano contratado, a situação atual (ativo, sem crédito, inadimplente...), o saldo
     * de créditos, os dados da assinatura (quando houver) e o histórico recente de eventos
     * financeiros. É apenas leitura — não altera nada.</p>
     *
     * @param empresaId identificador do cliente a consultar
     * @return a visão consolidada de plano, situação, saldo, assinatura e histórico
     * @throws ResponseStatusException se o cliente não for encontrado
     */
    @Transactional(readOnly = true)
    public EmpresaBillingOverviewResponse overview(String empresaId) {
        EmpresaEntity empresa = requireEmpresa(empresaId);
        EmpresaSubscriptionEntity subscription = subscriptionRepository
                .findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId).orElse(null);
        List<BillingEventResponse> events = eventRepository
                .findByEmpresaIdOrderByCreatedAtDesc(empresaId, PageRequest.of(0, 100)).stream()
                .map(e -> new BillingEventResponse(e.getId(), e.getEventType(), e.getMpResourceType(),
                        e.getMpResourceId(), e.getMpStatus(), e.getAmountCents(), e.getCurrency(), e.getCreatedAt()))
                .toList();

        EmpresaBillingOverviewResponse.SubscriptionInfo subscriptionInfo = subscription == null ? null
                : new EmpresaBillingOverviewResponse.SubscriptionInfo(
                subscription.getId(), subscription.getStatus(), subscription.getMpPreapprovalId(),
                subscription.getInitPoint(), subscription.getCurrentPeriodEnd(),
                subscription.getLastPaymentAt(), subscription.getGraceUntil());

        return new EmpresaBillingOverviewResponse(
                empresaId, empresa.getCommercialPlanType(), empresa.getStatus(),
                creditService.getBalance(empresaId), subscriptionInfo, events);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    /**
     * Marca a assinatura do cliente como paga e em dia: registra o pagamento, estende o período
     * vigente pela duração do ciclo do plano (1 mês na assinatura mensal, 12 meses na anual),
     * limpa qualquer carência e reativa o acesso. Uso interno.
     */
    private void markSubscriptionPaid(String empresaId) {
        subscriptionRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId).ifPresent(subscription -> {
            int intervalMonths = Optional.ofNullable(subscription.getPlanId())
                    .flatMap(planRepository::findById)
                    .map(SubscriptionPlanEntity::getBillingIntervalMonths)
                    .orElse(1);
            subscription.setStatus(SubscriptionStatus.AUTHORIZED);
            subscription.setLastPaymentAt(Instant.now());
            subscription.setCurrentPeriodEnd(
                    Instant.now().atOffset(ZoneOffset.UTC).plusMonths(intervalMonths).toInstant());
            subscription.setGraceUntil(null);
            subscription.setUpdatedAt(Instant.now());
        });
        activateEmpresa(empresaId);
    }

    /**
     * Credita no saldo do cliente o volume de avaliações incluído no ciclo pago da assinatura
     * PROFISSIONAL: o plano mensal credita a cota do mês a cada mensalidade aprovada; o plano
     * anual credita o pool do ano inteiro de uma só vez. A idempotência por pagamento é garantida
     * pelo registro único de {@code SUBSCRIPTION_PAYMENT_APPROVED} feito antes da chamada. Planos
     * sem volume configurado não creditam nada. Uso interno.
     */
    private void grantSubscriptionCredits(String empresaId, Long planId, Long billingEventId, String paymentId) {
        if (planId == null) {
            return;
        }
        planRepository.findById(planId).ifPresent(plan -> {
            if (plan.getCreditAmount() == null || plan.getCreditAmount() <= 0) {
                return;
            }
            creditService.addCredits(empresaId, plan.getCreditAmount(), billingEventId,
                    "Créditos do ciclo da assinatura (pagamento " + paymentId + ")");
        });
    }

    /**
     * Inicia a inadimplência quando uma mensalidade é recusada: coloca a assinatura em carência
     * (um prazo configurável para o cliente regularizar) e marca o cliente como inadimplente, sem
     * ainda cortar o acesso. Em seguida aciona a régua de cobrança para avisar o cliente com um
     * toque educativo de retry (e-mail/SMS), abrindo caminho para a regularização antes da suspensão
     * dura. Uso interno.
     */
    private void startDelinquency(String empresaId) {
        Instant graceUntil = Instant.now().plus(properties.gracePeriodDays(), ChronoUnit.DAYS);
        subscriptionRepository.findFirstByEmpresaIdOrderByCreatedAtDesc(empresaId).ifPresent(subscription -> {
            subscription.setStatus(SubscriptionStatus.DELINQUENT);
            subscription.setGraceUntil(graceUntil);
            subscription.setUpdatedAt(Instant.now());
        });
        empresaRepository.findById(empresaId).ifPresent(empresa -> {
            if (!empresa.getStatus().blocksAccess()) {
                empresa.setStatus(EmpresaStatus.INADIMPLENTE);
                empresa.setUpdatedAt(Instant.now());
            }
        });
        dunningService.notifyPaymentFailure(empresaId);
    }

    /**
     * Reativa o acesso do cliente quando ele volta a ficar regular: só tira do "aguardando
     * pagamento", "inadimplente" ou "sem crédito", sem mexer em quem já estava suspenso ou
     * cancelado por decisão administrativa. Uso interno.
     */
    private void activateEmpresa(String empresaId) {
        empresaRepository.findById(empresaId).ifPresent(empresa -> {
            if (empresa.getStatus() == EmpresaStatus.PENDENTE_PAGAMENTO
                    || empresa.getStatus() == EmpresaStatus.INADIMPLENTE
                    || empresa.getStatus() == EmpresaStatus.SEM_CREDITO) {
                empresa.setStatus(EmpresaStatus.ATIVO);
                empresa.setUpdatedAt(Instant.now());
            }
        });
    }

    /**
     * Devolve o ciclo de recarga automática ao estado ocioso quando a cobrança do cartão salvo se
     * resolve (aprovada ou recusada), liberando uma futura recarga assim que o saldo cair de novo.
     * Não faz nada se o cliente não usa recarga automática. Uso interno.
     */
    private void releaseAutoRecharge(String empresaId, String outcome) {
        autoRechargeConfigRepository.findById(empresaId).ifPresent(config -> {
            config.setStatus(AutoRechargeStatus.IDLE);
            config.setPendingReference(null);
            config.setLastOutcome(outcome);
            config.setUpdatedAt(Instant.now());
        });
    }

    /**
     * Grava um fato financeiro na trilha append-only ({@code empresa_billing_events}): guarda o
     * tipo do evento, o recurso do Mercado Pago envolvido, o valor e o retorno bruto. É o que dá
     * rastreabilidade a toda movimentação de dinheiro do cliente. Uso interno.
     */
    private EmpresaBillingEventEntity recordEvent(String empresaId, BillingEventType type, String resourceType,
                                                 String resourceId, String externalReference, String mpStatus,
                                                 Long amountCents, String currency, String requestId, JsonNode raw) {
        EmpresaBillingEventEntity event = new EmpresaBillingEventEntity();
        event.setEmpresaId(empresaId);
        event.setEventType(type);
        event.setMpResourceType(resourceType);
        event.setMpResourceId(resourceId);
        event.setExternalReference(externalReference);
        event.setMpStatus(mpStatus);
        event.setAmountCents(amountCents);
        event.setCurrency(currency);
        event.setRequestId(requestId);
        event.setRawPayload(truncate(raw == null ? null : raw.toString()));
        event.setCreatedAt(Instant.now());
        return eventRepository.save(event);
    }

    /** Localiza o cliente pelo identificador ou falha com "cliente não encontrado". Uso interno. */
    private EmpresaEntity requireEmpresa(String empresaId) {
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
    }

    /**
     * Localiza o plano/pacote pelo identificador, garantindo que ele existe e continua ativo (não
     * é possível cobrar por um plano desativado). Uso interno.
     */
    private SubscriptionPlanEntity requirePlan(Long planId) {
        if (planId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano não informado.");
        }
        SubscriptionPlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plano não encontrado."));
        if (!plan.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano inativo.");
        }
        return plan;
    }

    /**
     * Cria a "etiqueta" única que viaja junto da cobrança no Mercado Pago e permite reconhecer,
     * quando o pagamento voltar, de qual cliente, qual plano e qual tipo (crédito ou assinatura)
     * ele era. Uso interno.
     */
    private static String reference(String kind, String empresaId, Long planId) {
        return kind + ":" + empresaId + ":" + planId + ":" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Lê de volta a etiqueta criada em {@link #reference}, devolvendo {tipo, cliente, plano}. É
     * assim que um pagamento que chega do Mercado Pago é atribuído ao cliente certo. Uso interno.
     */
    private static String[] parseReference(String externalReference) {
        if (externalReference == null) {
            return new String[]{null, null, null};
        }
        String[] parts = externalReference.split(":");
        if (parts.length < 3) {
            return new String[]{null, null, null};
        }
        return new String[]{parts[0], parts[1], parts[2]};
    }

    /** Lê com segurança um campo de texto da resposta do Mercado Pago, tolerando ausências. Uso interno. */
    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /** Converte o valor do pagamento (em reais) para centavos, o formato guardado na trilha. Uso interno. */
    private static Long cents(JsonNode payment) {
        if (payment == null) {
            return null;
        }
        JsonNode amount = payment.get("transaction_amount");
        if (amount == null || amount.isNull()) {
            return null;
        }
        return Math.round(amount.asDouble() * 100);
    }

    /** Limita o tamanho do retorno bruto guardado, evitando que a trilha cresça sem controle. Uso interno. */
    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 8000 ? value.substring(0, 8000) : value;
    }
}
