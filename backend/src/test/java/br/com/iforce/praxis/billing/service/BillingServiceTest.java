package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.billing.config.MercadoPagoProperties;

import br.com.iforce.praxis.billing.model.BillingEventType;

import br.com.iforce.praxis.billing.model.SubscriptionStatus;

import br.com.iforce.praxis.billing.persistence.entity.SubscriptionPlanEntity;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaBillingEventEntity;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaSubscriptionEntity;

import br.com.iforce.praxis.billing.persistence.repository.SubscriptionPlanRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaBillingEventRepository;

import br.com.iforce.praxis.billing.persistence.repository.EmpresaSubscriptionRepository;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private BillingService service;

    @BeforeEach
    void setUp() {
        MercadoPagoProperties properties =
                new MercadoPagoProperties(
                        true,
                        null,
                        null,
                        "tok",
                        "pub",
                        "sec",
                        null,
                        null,
                        null,
                        null,
                        7,
                        null,
                        null
                );
        service = new BillingService(mercadoPagoClient, properties, planRepository, eventRepository,
                subscriptionRepository, creditService, empresaRepository);
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
    void approvedSubscriptionPaymentReactivatesEmpresa() throws Exception {
        var payment = objectMapper.readTree(
                "{\"status\":\"approved\",\"external_reference\":\"sub:t1:9:abc\",\"transaction_amount\":299.0}");
        when(mercadoPagoClient.getPayment("pay9")).thenReturn(payment);
        when(eventRepository.existsByMpResourceIdAndEventType("pay9", BillingEventType.SUBSCRIPTION_PAYMENT_APPROVED))
                .thenReturn(false);
        EmpresaSubscriptionEntity subscription = new EmpresaSubscriptionEntity();
        subscription.setEmpresaId("t1");
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
    }
}
