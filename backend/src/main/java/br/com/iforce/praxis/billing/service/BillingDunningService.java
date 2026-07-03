package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.billing.dto.DunningNotice;

import br.com.iforce.praxis.billing.model.BillingEventType;

import br.com.iforce.praxis.billing.model.DunningStage;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaBillingEventEntity;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaSubscriptionEntity;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaBillingEventRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaSubscriptionRepository;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.List;


/**
 * Régua de cobrança inteligente (dunning) — avisa o cliente com jeito antes de cortar o acesso.
 *
 * <p>Na visão do processo, quando uma cobrança falha o cliente não é surpreendido com uma suspensão
 * seca: a régua dispara toques educativos de "tente novamente" e vai lembrando enquanto ele está
 * pendente de pagamento ou inadimplente, cobrindo o intervalo até a suspensão dura. Há dois
 * gatilhos: o webhook do Mercado Pago, no instante em que o pagamento é recusado
 * ({@link #notifyPaymentFailure}); e uma varredura recorrente que relembra os que continuam em
 * atraso ({@link #remindClientsBeforeSuspension}). Cada toque vira um evento financeiro append-only
 * ({@link BillingEventType#DUNNING_NOTIFIED}), o que dá rastreabilidade e serve de anti-flood para
 * não repetir a mesma mensagem em curto intervalo.</p>
 */
@Service
public class BillingDunningService {

    private static final Logger log = LoggerFactory.getLogger(BillingDunningService.class);
    private static final String RESOURCE_TYPE = "dunning";
    private static final List<EmpresaStatus> DUNNING_STATUSES =
            List.of(EmpresaStatus.PENDENTE_PAGAMENTO, EmpresaStatus.INADIMPLENTE);

    private final EmpresaRepository empresaRepository;
    private final EmpresaSubscriptionRepository subscriptionRepository;
    private final EmpresaBillingEventRepository eventRepository;
    private final BillingDunningNotificationSender notificationSender;
    private final long reminderMinIntervalHours;

    public BillingDunningService(
            EmpresaRepository empresaRepository,
            EmpresaSubscriptionRepository subscriptionRepository,
            EmpresaBillingEventRepository eventRepository,
            BillingDunningNotificationSender notificationSender,
            @Value("${praxis.billing.dunning-reminder-min-interval-hours:20}") long reminderMinIntervalHours
    ) {
        this.empresaRepository = empresaRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.eventRepository = eventRepository;
        this.notificationSender = notificationSender;
        this.reminderMinIntervalHours = reminderMinIntervalHours;
    }

    /**
     * Dispara o toque educativo no exato momento em que um pagamento falha (chamado do webhook).
     *
     * <p>Fluxo do processo: localiza o cliente e, se ele ainda tem acesso (não foi suspenso nem
     * cancelado por decisão administrativa), registra o toque na trilha financeira e envia a
     * notificação de retry com o prazo de carência vigente. Clientes já bloqueados são ignorados —
     * a régua existe justamente para evitar o corte, não para insistir depois dele.</p>
     *
     * @param empresaId cliente cujo pagamento acabou de ser recusado
     */
    @Transactional
    public void notifyPaymentFailure(String empresaId) {
        EmpresaEntity empresa = empresaRepository.findById(empresaId).orElse(null);
        if (empresa == null || empresa.getStatus().blocksAccess()) {
            return;
        }
        dispatch(empresa, DunningStage.PAYMENT_FAILED, Instant.now());
    }

    /**
     * Varredura recorrente que relembra os clientes em atraso antes da suspensão dura.
     *
     * <p>Fluxo do processo: percorre os clientes nos status financeiros que antecedem o corte
     * ({@link EmpresaStatus#PENDENTE_PAGAMENTO} e {@link EmpresaStatus#INADIMPLENTE}) e, para cada um
     * que ainda não recebeu um toque no intervalo mínimo configurado, envia um lembrete educativo de
     * regularização. O intervalo mínimo evita floodar o cliente quando a varredura roda com
     * frequência. Devolve quantos lembretes foram enviados nesta passada.</p>
     *
     * @param moment instante de referência (injetável para testes determinísticos)
     * @return quantidade de lembretes enviados
     */
    @Transactional
    public int remindClientsBeforeSuspension(Instant moment) {
        Instant floodWindowStart = moment.minus(reminderMinIntervalHours, ChronoUnit.HOURS);
        List<EmpresaEntity> pending = empresaRepository.findByStatuses(DUNNING_STATUSES);
        int reminded = 0;
        for (EmpresaEntity empresa : pending) {
            if (eventRepository.existsByEmpresaIdAndEventTypeAndCreatedAtAfter(
                    empresa.getId(), BillingEventType.DUNNING_NOTIFIED, floodWindowStart)) {
                continue;
            }
            dispatch(empresa, DunningStage.RETRY_REMINDER, moment);
            reminded++;
        }
        if (reminded > 0) {
            log.info("Régua de cobrança: {} lembrete(s) de regularização enviado(s) antes da suspensão.", reminded);
        }
        return reminded;
    }

    /** Registra o toque na trilha financeira e o entrega ao canal de notificação. Uso interno. */
    private void dispatch(EmpresaEntity empresa, DunningStage stage, Instant at) {
        Instant graceUntil = subscriptionRepository
                .findFirstByEmpresaIdOrderByCreatedAtDesc(empresa.getId())
                .map(EmpresaSubscriptionEntity::getGraceUntil)
                .orElse(null);

        recordDunningEvent(empresa.getId(), stage, at);
        notificationSender.sendRetryNotice(new DunningNotice(
                empresa.getId(),
                empresa.getCorporateEmail(),
                empresa.getPhone(),
                stage,
                empresa.getStatus(),
                graceUntil));
    }

    /**
     * Grava o toque de cobrança como evento append-only ({@code empresa_billing_events}). Não é um
     * efeito financeiro — é o registro de que o cliente foi avisado, o que dá rastreabilidade e
     * alimenta o anti-flood. Uso interno.
     */
    private void recordDunningEvent(String empresaId, DunningStage stage, Instant at) {
        EmpresaBillingEventEntity event = new EmpresaBillingEventEntity();
        event.setEmpresaId(empresaId);
        event.setEventType(BillingEventType.DUNNING_NOTIFIED);
        event.setMpResourceType(RESOURCE_TYPE);
        event.setMpStatus(stage.name());
        event.setCreatedAt(at);
        eventRepository.save(event);
    }
}
