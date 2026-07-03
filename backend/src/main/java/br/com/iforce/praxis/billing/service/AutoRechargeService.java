package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.billing.model.AutoRechargeStatus;

import br.com.iforce.praxis.billing.model.BillingEventType;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaAutoRechargeConfigEntity;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaBillingEventEntity;

import br.com.iforce.praxis.billing.persistence.entity.SubscriptionPlanEntity;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaAutoRechargeConfigRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaBillingEventRepository;

import br.com.iforce.praxis.billing.persistence.repository.SubscriptionPlanRepository;

import com.fasterxml.jackson.databind.JsonNode;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


import java.time.Duration;

import java.time.Instant;

import java.util.Map;

import java.util.UUID;


/**
 * Recarga automática (auto-top-up) do plano pré-pago AVULSO — recompõe o saldo sem atrito humano.
 *
 * <p>Na visão do processo, funciona como o "abastecimento automático" de um cartão pré-pago: o
 * cliente escolhe, uma vez só, ligar a recarga automática, dizer a partir de qual saldo crítico ela
 * age (por exemplo, 5 avaliações), qual pacote comprar e qual cartão usar. A partir daí, sempre que
 * uma avaliação concluída faz o saldo cair abaixo desse nível, a plataforma cobra o cartão salvo no
 * Mercado Pago e, com o pagamento confirmado, libera um novo lote de créditos — tudo sozinho.</p>
 *
 * <p><b>Como evitamos cobrar o cliente à toa:</b></p>
 * <ul>
 *   <li><b>Uma cobrança por vez:</b> enquanto uma recarga está em andamento o estado fica
 *       {@code PENDING} e uma trava na linha de configuração serializa disparos simultâneos, então
 *       uma rajada de avaliações concluídas ao mesmo tempo gera uma única cobrança.</li>
 *   <li><b>Idempotência no Mercado Pago:</b> a cobrança leva uma chave de idempotência, de modo que
 *       uma retentativa da mesma recarga não cobra o cartão duas vezes.</li>
 *   <li><b>Janela de espera (cooldown):</b> depois de uma tentativa recente que não recompôs o
 *       saldo (recusa ou falha), aguardamos um tempo antes de tentar de novo, para não martelar o
 *       cartão do cliente.</li>
 *   <li><b>Autocura:</b> se uma cobrança fica presa em {@code PENDING} por tempo demais (por
 *       exemplo, uma notificação perdida), depois de um limite ela é considerada vencida e uma nova
 *       recarga pode ser disparada.</li>
 * </ul>
 *
 * <p>A confirmação do pagamento e a soma dos créditos seguem a mesma regra de ouro do resto da
 * cobrança: o Mercado Pago é a fonte da verdade e a aplicação é idempotente — por isso este serviço
 * delega a confirmação ao {@link BillingService#processPaymentNotification}.</p>
 */
@Service
public class AutoRechargeService {

    private static final Logger log = LoggerFactory.getLogger(AutoRechargeService.class);

    /**
     * Prefixo da etiqueta (external_reference) das cobranças de recarga automática. Definido em
     * {@link BillingService} porque é lá que a confirmação do pagamento interpreta esse tipo.
     */
    private static final String KIND_AUTO_CREDIT = BillingService.KIND_AUTO_CREDIT;

    /** Tempo mínimo entre tentativas quando a anterior não recompôs o saldo (recusa/falha). */
    private static final Duration COOLDOWN = Duration.ofMinutes(10);

    /** Após este tempo, uma recarga presa em PENDING é considerada vencida e pode ser refeita. */
    private static final Duration STALE_PENDING = Duration.ofMinutes(30);

    private final EmpresaAutoRechargeConfigRepository configRepository;
    private final EmpresaRepository empresaRepository;
    private final SubscriptionPlanRepository planRepository;
    private final EmpresaBillingEventRepository eventRepository;
    private final CreditService creditService;
    private final MercadoPagoClient mercadoPagoClient;
    private final BillingService billingService;

    public AutoRechargeService(
            EmpresaAutoRechargeConfigRepository configRepository,
            EmpresaRepository empresaRepository,
            SubscriptionPlanRepository planRepository,
            EmpresaBillingEventRepository eventRepository,
            CreditService creditService,
            MercadoPagoClient mercadoPagoClient,
            BillingService billingService
    ) {
        this.configRepository = configRepository;
        this.empresaRepository = empresaRepository;
        this.planRepository = planRepository;
        this.eventRepository = eventRepository;
        this.creditService = creditService;
        this.mercadoPagoClient = mercadoPagoClient;
        this.billingService = billingService;
    }

    // ------------------------------------------------------------------
    // Configuração (self-service do cliente)
    // ------------------------------------------------------------------

    /**
     * Lê a preferência de recarga automática do cliente (ou uma configuração desligada padrão se ele
     * nunca configurou). Apenas leitura.
     *
     * @param empresaId identificador do cliente
     * @return a configuração de recarga automática atual do cliente
     */
    @Transactional(readOnly = true)
    public EmpresaAutoRechargeConfigEntity getConfig(String empresaId) {
        return configRepository.findById(empresaId).orElseGet(() -> {
            EmpresaAutoRechargeConfigEntity fresh = new EmpresaAutoRechargeConfigEntity();
            fresh.setEmpresaId(empresaId);
            fresh.setEnabled(false);
            fresh.setThresholdCredits(5);
            fresh.setStatus(AutoRechargeStatus.IDLE);
            return fresh;
        });
    }

    /**
     * Liga/desliga e ajusta a recarga automática do cliente pré-pago (AVULSO).
     *
     * <p>Fluxo do processo: o cliente define, na sua tela de cobrança, se quer recarga automática, a
     * partir de qual saldo crítico ela deve agir, qual pacote de créditos comprar e qual cartão
     * salvo usar. Ao <em>ligar</em> a recarga, o sistema exige que essas informações estejam
     * completas e coerentes (pacote é mesmo um pacote de créditos AVULSO e há um cartão salvo), para
     * não prometer uma recarga que não teria como cobrar. Só clientes do plano pré-pago podem
     * configurar isto.</p>
     *
     * @param empresaId identificador do cliente
     * @param enabled se a recarga automática deve ficar ligada
     * @param thresholdCredits nível crítico de saldo que dispara a recarga (precisa ser positivo)
     * @param planId pacote de créditos AVULSO a comprar na recarga
     * @param mpCustomerId referência do cliente/pagador salvo no Mercado Pago
     * @param mpCardId referência do cartão salvo no Mercado Pago
     * @return a configuração salva
     * @throws ResponseStatusException se o cliente não for AVULSO, ou se, ao ligar, faltarem dados
     *         ou o pacote não for um pacote de créditos AVULSO válido
     */
    @Transactional
    public EmpresaAutoRechargeConfigEntity configure(String empresaId, boolean enabled, int thresholdCredits,
                                                     Long planId, String mpCustomerId, String mpCardId) {
        EmpresaEntity empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
        if (empresa.getCommercialPlanType() != CommercialPlanType.AVULSO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Recarga automática só está disponível para o plano pré-pago (AVULSO).");
        }
        if (thresholdCredits <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nível crítico de saldo deve ser positivo.");
        }
        if (enabled) {
            if (planId == null || isBlank(mpCustomerId) || isBlank(mpCardId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Para ligar a recarga automática informe o pacote de créditos e o cartão salvo.");
            }
            requireAvulsoCreditPlan(planId);
        }

        Instant now = Instant.now();
        EmpresaAutoRechargeConfigEntity config = configRepository.findById(empresaId)
                .orElseGet(() -> {
                    EmpresaAutoRechargeConfigEntity created = new EmpresaAutoRechargeConfigEntity();
                    created.setEmpresaId(empresaId);
                    created.setStatus(AutoRechargeStatus.IDLE);
                    created.setCreatedAt(now);
                    return created;
                });
        config.setEnabled(enabled);
        config.setThresholdCredits(thresholdCredits);
        config.setPlanId(planId);
        config.setMpCustomerId(mpCustomerId);
        config.setMpCardId(mpCardId);
        config.setUpdatedAt(now);
        if (config.getCreatedAt() == null) {
            config.setCreatedAt(now);
        }
        return configRepository.save(config);
    }

    // ------------------------------------------------------------------
    // Disparo da recarga
    // ------------------------------------------------------------------

    /**
     * Avalia se o cliente precisa de uma recarga automática e, se precisar, cobra o cartão salvo.
     *
     * <p>É o coração da funcionalidade, acionado logo após um consumo de crédito baixar o saldo.
     * Fluxo do processo: confere se a recarga está ligada e se o saldo realmente caiu abaixo do
     * nível crítico; garante que não há outra cobrança em andamento nem uma tentativa recente
     * malsucedida (para não cobrar o cliente à toa); então trava o ciclo, registra o disparo, cobra
     * o cartão salvo no Mercado Pago e confirma o pagamento pela fonte da verdade — o que soma os
     * créditos de forma idempotente. Qualquer falha de comunicação é registrada e o ciclo volta a
     * ficar ocioso, respeitando a janela de espera antes de uma nova tentativa.</p>
     *
     * <p>Este método é tolerante a chamadas "à toa": se nada precisa ser feito, ele simplesmente não
     * faz nada. Assim pode ser chamado a cada avaliação concluída sem efeitos colaterais.</p>
     *
     * @param empresaId identificador do cliente cujo saldo pode ter ficado baixo
     */
    @Transactional
    public void maybeRecharge(String empresaId) {
        // Caminho rápido sem trava: a imensa maioria dos clientes não usa recarga automática.
        EmpresaAutoRechargeConfigEntity peek = configRepository.findById(empresaId).orElse(null);
        if (peek == null || !peek.isEnabled()) {
            return;
        }

        // Trava a configuração para serializar disparos concorrentes do MESMO cliente.
        EmpresaAutoRechargeConfigEntity config = configRepository.findByEmpresaIdForUpdate(empresaId).orElse(null);
        if (config == null || !config.isEnabled()) {
            return;
        }

        EmpresaEntity empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null || empresa.getCommercialPlanType() != CommercialPlanType.AVULSO) {
            return;
        }
        // Saldo já recomposto (ou nunca esteve baixo): nada a fazer. Cobre também o caminho feliz de
        // uma recarga anterior que já entrou.
        if (creditService.getBalance(empresaId) >= config.getThresholdCredits()) {
            return;
        }

        Instant now = Instant.now();
        if (config.getStatus() == AutoRechargeStatus.PENDING) {
            if (!isOlderThan(config.getLastTriggeredAt(), now, STALE_PENDING)) {
                // Já há uma cobrança recente em andamento: espera a confirmação (evita cobrança dupla).
                return;
            }
            log.warn("Recarga automática da empresa {} presa em PENDING desde {}; refazendo.",
                    empresaId, config.getLastTriggeredAt());
        } else if (!isOlderThan(config.getLastTriggeredAt(), now, COOLDOWN)) {
            // Tentativa recente que não recompôs o saldo: aguarda a janela de espera.
            return;
        }

        SubscriptionPlanEntity plan;
        try {
            plan = requireAvulsoCreditPlan(config.getPlanId());
        } catch (ResponseStatusException invalidPlan) {
            markFailed(config, now, "Pacote de créditos inválido para recarga automática.");
            recordEvent(empresaId, BillingEventType.CREDIT_AUTO_RECHARGE_FAILED, null,
                    config.getPendingReference(), "invalid_plan", "Pacote de créditos inválido.");
            return;
        }
        if (isBlank(config.getMpCustomerId()) || isBlank(config.getMpCardId())) {
            markFailed(config, now, "Cartão salvo ausente para recarga automática.");
            recordEvent(empresaId, BillingEventType.CREDIT_AUTO_RECHARGE_FAILED, null,
                    config.getPendingReference(), "no_card", "Sem cartão salvo para cobrar.");
            return;
        }

        // Reserva o ciclo: marca PENDING e registra o disparo ANTES de cobrar, para que uma segunda
        // chamada concorrente (que espera a trava) encontre o estado PENDING e não cobre de novo.
        String reference = KIND_AUTO_CREDIT + ":" + empresaId + ":" + plan.getId() + ":"
                + UUID.randomUUID().toString().substring(0, 8);
        config.setStatus(AutoRechargeStatus.PENDING);
        config.setPendingReference(reference);
        config.setLastTriggeredAt(now);
        config.setLastOutcome("Recarga automática disparada.");
        config.setUpdatedAt(now);
        recordEvent(empresaId, BillingEventType.CREDIT_AUTO_RECHARGE_TRIGGERED, "payment", reference,
                "triggered", "Recarga automática de " + plan.getCreditAmount() + " créditos disparada.");

        JsonNode charge;
        try {
            charge = mercadoPagoClient.chargeSavedCard(plan, config.getMpCustomerId(), config.getMpCardId(),
                    reference, Map.of("empresa_id", empresaId, "plan_id", plan.getId(), "kind", KIND_AUTO_CREDIT),
                    reference);
        } catch (RuntimeException communicationFailure) {
            // A cobrança sequer foi criada: volta a IDLE e registra a falha. A janela de espera (o
            // lastTriggeredAt já marcado) evita uma nova tentativa imediata.
            log.error("Falha ao cobrar cartão salvo na recarga automática da empresa {}.", empresaId);
            markFailed(config, now, "Falha na comunicação com o Mercado Pago.");
            recordEvent(empresaId, BillingEventType.CREDIT_AUTO_RECHARGE_FAILED, "payment", reference,
                    "gateway_error", "Falha na comunicação com o Mercado Pago ao cobrar o cartão.");
            return;
        }

        String paymentId = text(charge, "id");
        if (paymentId == null) {
            markFailed(config, now, "Mercado Pago não retornou um pagamento.");
            recordEvent(empresaId, BillingEventType.CREDIT_AUTO_RECHARGE_FAILED, "payment", reference,
                    "no_payment", "Mercado Pago não retornou identificador de pagamento.");
            return;
        }

        // Confirma pela fonte da verdade: consulta o pagamento no Mercado Pago e aplica o efeito de
        // forma idempotente. Para uma cobrança aprovada, soma os créditos e devolve o ciclo a IDLE;
        // para uma recusa, registra a falha e libera o ciclo; se ainda pendente, o webhook conclui.
        billingService.processPaymentNotification(paymentId, reference);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    /** Libera o ciclo de recarga (volta a IDLE) e registra o motivo do insucesso. Uso interno. */
    private void markFailed(EmpresaAutoRechargeConfigEntity config, Instant now, String outcome) {
        config.setStatus(AutoRechargeStatus.IDLE);
        config.setPendingReference(null);
        config.setLastOutcome(outcome);
        config.setUpdatedAt(now);
    }

    /**
     * Localiza o pacote e garante que ele é mesmo um pacote de créditos AVULSO válido (com
     * quantidade de créditos definida). Uso interno.
     */
    private SubscriptionPlanEntity requireAvulsoCreditPlan(Long planId) {
        if (planId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pacote de créditos não informado.");
        }
        SubscriptionPlanEntity plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pacote de créditos não encontrado."));
        if (!plan.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Pacote de créditos inativo.");
        }
        if (plan.getPlanType() != CommercialPlanType.AVULSO || plan.getCreditAmount() == null
                || plan.getCreditAmount() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Plano não é um pacote de créditos AVULSO.");
        }
        return plan;
    }

    /** Grava um fato de recarga automática na trilha financeira append-only. Uso interno. */
    private void recordEvent(String empresaId, BillingEventType type, String resourceType,
                             String externalReference, String status, String note) {
        EmpresaBillingEventEntity event = new EmpresaBillingEventEntity();
        event.setEmpresaId(empresaId);
        event.setEventType(type);
        event.setMpResourceType(resourceType);
        event.setExternalReference(externalReference);
        event.setMpStatus(status);
        event.setRawPayload(note);
        event.setCreatedAt(Instant.now());
        eventRepository.save(event);
    }

    /** Diz se um instante é nulo (nunca) ou mais antigo que a janela informada a partir de agora. Uso interno. */
    private static boolean isOlderThan(Instant instant, Instant now, Duration window) {
        return instant == null || instant.isBefore(now.minus(window));
    }

    /** Lê com segurança um campo de texto da resposta do Mercado Pago. Uso interno. */
    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /** Diz se um texto está vazio (nulo ou só espaços). Uso interno. */
    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
