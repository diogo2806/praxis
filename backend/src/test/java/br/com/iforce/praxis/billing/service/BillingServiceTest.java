package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.billing.config.MercadoPagoProperties;

import br.com.iforce.praxis.billing.model.AutoRechargeStatus;

import br.com.iforce.praxis.billing.model.BillingEventType;

import br.com.iforce.praxis.billing.model.SubscriptionStatus;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaAutoRechargeConfigEntity;

import br.com.iforce.praxis.billing.persistence.entity.SubscriptionPlanEntity;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaBillingEventEntity;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaSubscriptionEntity;

import br.com.iforce.praxis.billing.persistence.repository.SubscriptionPlanRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaAutoRechargeConfigRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaBillingEventRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaSubscriptionRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;


import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyInt;

import static org.mockito.ArgumentMatchers.anyLong;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.lenient;

import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock private MercadoPagoClient mercadoPagoClient;
    @Mock private SubscriptionPlanRepository planRepository;
    @Mock private EmpresaBillingEventRepository eventRepository;
    @Mock private EmpresaSubscriptionRepository subscriptionRepository;
    @Mock private CreditService creditService;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private BillingDunningService dunningService;
    @Mock private EmpresaAutoRechargeConfigRepository autoRechargeConfigRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BillingService service;

    @BeforeEach
    void setUp() {
        MercadoPagoProperties properties =
                new MercadoPagoProperties(
                        true,
                        null,
                        "tok",
                        "pub",
                        "sec",
                        7,
                        null,
                        null
                );
        service = new BillingService(mercadoPagoClient, properties, planRepository, eventRepository,
                subscriptionRepository, creditService, empresaRepository, dunningService,
                autoRechargeConfigRepository);
        lenient().when(eventRepository.save(any(EmpresaBillingEventEntity.class))).thenAnswer(invocation -> {
            EmpresaBillingEventEntity event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId(1L);
            }
            return event;
        });
    }

    private SubscriptionPlanEntity plan(CommercialPlanType type, Integer credits) {
        SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
        plan.setId(5L);
        plan.setCode("P");
        plan.setName("Plano");
        plan.setPlanType(type);
        plan.setPriceCents(49900);
        plan.setCurrency("BRL");
        plan.setCreditAmount(credits);
        return plan;
    }

    @Test
    void approvedCreditPaymentConsultsMercadoPagoThenAddsCredits() throws Exception {
        var payment = objectMapper.readTree(
                "{\"status\":\"approved\",\"external_reference\":\"credit:t1:5:abc\",\"transaction_amount\":499.0}");
        when(mercadoPagoClient.getPayment("pay1")).thenReturn(payment);
        when(eventRepository.existsByMpResourceIdAndEventType("pay1", BillingEventType.CREDIT_PURCHASE_APPROVED))
                .thenReturn(false);
        when(planRepository.findById(5L)).thenReturn(Optional.of(plan(CommercialPlanType.AVULSO, 100)));

        service.processPaymentNotification("pay1", "req-1");

        // A confirmação veio de consulta ao Mercado Pago (fonte da verdade).
        verify(mercadoPagoClient).getPayment("pay1");
        verify(creditService).addCredits(eq("t1"), eq(100), anyLong(), anyString());
        verify(eventRepository).save(any(EmpresaBillingEventEntity.class));
    }

    @Test
    void approvedCreditPaymentIsIdempotent() throws Exception {
        var payment = objectMapper.readTree(
                "{\"status\":\"approved\",\"external_reference\":\"credit:t1:5:abc\",\"transaction_amount\":499.0}");
        when(mercadoPagoClient.getPayment("pay1")).thenReturn(payment);
        when(eventRepository.existsByMpResourceIdAndEventType("pay1", BillingEventType.CREDIT_PURCHASE_APPROVED))
                .thenReturn(true);

        service.processPaymentNotification("pay1", "req-1");

        verify(creditService, never()).addCredits(anyString(), anyInt(), any(), anyString());
    }

    @Test
    void rejectedSubscriptionPaymentStartsDelinquencyWithGrace() throws Exception {
        var payment = objectMapper.readTree(
                "{\"status\":\"rejected\",\"external_reference\":\"sub:t1:9:abc\",\"transaction_amount\":299.0}");
        when(mercadoPagoClient.getPayment("pay9")).thenReturn(payment);

        EmpresaSubscriptionEntity subscription = new EmpresaSubscriptionEntity();
        subscription.setEmpresaId("t1");
        subscription.setStatus(SubscriptionStatus.AUTHORIZED);
        when(subscriptionRepository.findFirstByEmpresaIdOrderByCreatedAtDesc("t1"))
                .thenReturn(Optional.of(subscription));
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("t1");
        empresa.setStatus(EmpresaStatus.ATIVO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));

        service.processPaymentNotification("pay9", "req-9");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.DELINQUENT);
        assertThat(subscription.getGraceUntil()).isNotNull();
        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.INADIMPLENTE);
    }

    @Test
    void approvedSubscriptionPaymentReactivatesEmpresaAndGrantsMonthlyQuota() throws Exception {
        var payment = objectMapper.readTree(
                "{\"status\":\"approved\",\"external_reference\":\"sub:t1:9:abc\",\"transaction_amount\":299.0}");
        when(mercadoPagoClient.getPayment("pay9")).thenReturn(payment);
        when(eventRepository.existsByMpResourceIdAndEventType("pay9", BillingEventType.SUBSCRIPTION_PAYMENT_APPROVED))
                .thenReturn(false);
        SubscriptionPlanEntity plan = plan(CommercialPlanType.PROFISSIONAL, 30);
        plan.setId(9L);
        plan.setBillingIntervalMonths(1);
        when(planRepository.findById(9L)).thenReturn(Optional.of(plan));
        EmpresaSubscriptionEntity subscription = new EmpresaSubscriptionEntity();
        subscription.setEmpresaId("t1");
        subscription.setPlanId(9L);
        subscription.setStatus(SubscriptionStatus.PENDING);
        when(subscriptionRepository.findFirstByEmpresaIdOrderByCreatedAtDesc("t1"))
                .thenReturn(Optional.of(subscription));
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("t1");
        empresa.setStatus(EmpresaStatus.PENDENTE_PAGAMENTO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));

        service.processPaymentNotification("pay9", "req-9");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.AUTHORIZED);
        assertThat(subscription.getLastPaymentAt()).isNotNull();
        assertThat(empresa.getStatus()).isEqualTo(EmpresaStatus.ATIVO);
        // A mensalidade aprovada credita a cota do mês no saldo do cliente.
        verify(creditService).addCredits(eq("t1"), eq(30), anyLong(), anyString());
        // O período vigente é estendido por 1 mês (~30 dias, nunca chega perto de 1 ano).
        assertThat(subscription.getCurrentPeriodEnd())
                .isBefore(java.time.Instant.now().plus(45, java.time.temporal.ChronoUnit.DAYS));
    }

    @Test
    void approvedAnnualSubscriptionPaymentGrantsFullYearPool() throws Exception {
        var payment = objectMapper.readTree(
                "{\"status\":\"approved\",\"external_reference\":\"sub:t1:9:abc\",\"transaction_amount\":17787.6}");
        when(mercadoPagoClient.getPayment("pay9")).thenReturn(payment);
        when(eventRepository.existsByMpResourceIdAndEventType("pay9", BillingEventType.SUBSCRIPTION_PAYMENT_APPROVED))
                .thenReturn(false);
        SubscriptionPlanEntity plan = plan(CommercialPlanType.PROFISSIONAL, 360);
        plan.setId(9L);
        plan.setBillingIntervalMonths(12);
        when(planRepository.findById(9L)).thenReturn(Optional.of(plan));
        EmpresaSubscriptionEntity subscription = new EmpresaSubscriptionEntity();
        subscription.setEmpresaId("t1");
        subscription.setPlanId(9L);
        subscription.setStatus(SubscriptionStatus.PENDING);
        when(subscriptionRepository.findFirstByEmpresaIdOrderByCreatedAtDesc("t1"))
                .thenReturn(Optional.of(subscription));
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("t1");
        empresa.setStatus(EmpresaStatus.PENDENTE_PAGAMENTO);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa));

        service.processPaymentNotification("pay9", "req-9");

        // O pagamento anual credita o pool do ano inteiro de uma vez...
        verify(creditService).addCredits(eq("t1"), eq(360), anyLong(), anyString());
        // ...e o período vigente é estendido por 12 meses.
        assertThat(subscription.getCurrentPeriodEnd())
                .isAfter(java.time.Instant.now().plus(300, java.time.temporal.ChronoUnit.DAYS));
    }

    @Test
    void approvedAutoRechargePaymentAddsCreditsAndReleasesCycle() throws Exception {
        var payment = objectMapper.readTree(
                "{\"status\":\"approved\",\"external_reference\":\"autocredit:t1:5:abc\",\"transaction_amount\":499.0}");
        when(mercadoPagoClient.getPayment("pay-auto")).thenReturn(payment);
        when(eventRepository.existsByMpResourceIdAndEventType("pay-auto", BillingEventType.CREDIT_PURCHASE_APPROVED))
                .thenReturn(false);
        when(planRepository.findById(5L)).thenReturn(Optional.of(plan(CommercialPlanType.AVULSO, 100)));
        EmpresaAutoRechargeConfigEntity config = new EmpresaAutoRechargeConfigEntity();
        config.setEmpresaId("t1");
        config.setStatus(AutoRechargeStatus.PENDING);
        config.setPendingReference("autocredit:t1:5:abc");
        when(autoRechargeConfigRepository.findById("t1")).thenReturn(Optional.of(config));

        service.processPaymentNotification("pay-auto", "req-auto");

        // Soma os créditos como qualquer compra confirmada...
        verify(creditService).addCredits(eq("t1"), eq(100), anyLong(), anyString());
        // ...e libera o ciclo de recarga para uma futura reposição.
        assertThat(config.getStatus()).isEqualTo(AutoRechargeStatus.IDLE);
        assertThat(config.getPendingReference()).isNull();
    }

    @Test
    void rejectedAutoRechargePaymentRecordsFailureAndReleasesCycle() throws Exception {
        var payment = objectMapper.readTree(
                "{\"status\":\"rejected\",\"external_reference\":\"autocredit:t1:5:abc\",\"transaction_amount\":499.0}");
        when(mercadoPagoClient.getPayment("pay-auto")).thenReturn(payment);
        EmpresaAutoRechargeConfigEntity config = new EmpresaAutoRechargeConfigEntity();
        config.setEmpresaId("t1");
        config.setStatus(AutoRechargeStatus.PENDING);
        config.setPendingReference("autocredit:t1:5:abc");
        when(autoRechargeConfigRepository.findById("t1")).thenReturn(Optional.of(config));

        service.processPaymentNotification("pay-auto", "req-auto");

        // Cartão recusado não soma créditos, registra a falha e libera o ciclo.
        verify(creditService, never()).addCredits(anyString(), anyInt(), any(), anyString());
        ArgumentCaptor<EmpresaBillingEventEntity> event = ArgumentCaptor.forClass(EmpresaBillingEventEntity.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo(BillingEventType.CREDIT_AUTO_RECHARGE_FAILED);
        assertThat(config.getStatus()).isEqualTo(AutoRechargeStatus.IDLE);
    }
}
