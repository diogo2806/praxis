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

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;

import java.time.temporal.ChronoUnit;

import java.util.Map;

import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.ArgumentMatchers.eq;

import static org.mockito.Mockito.lenient;

import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class AutoRechargeServiceTest {

    @Mock private EmpresaAutoRechargeConfigRepository configRepository;
    @Mock private EmpresaRepository empresaRepository;
    @Mock private SubscriptionPlanRepository planRepository;
    @Mock private EmpresaBillingEventRepository eventRepository;
    @Mock private CreditService creditService;
    @Mock private MercadoPagoClient mercadoPagoClient;
    @Mock private BillingService billingService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AutoRechargeService service;

    @BeforeEach
    void setUp() {
        service = new AutoRechargeService(configRepository, empresaRepository, planRepository, eventRepository,
                creditService, mercadoPagoClient, billingService);
        lenient().when(eventRepository.save(any(EmpresaBillingEventEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(configRepository.save(any(EmpresaAutoRechargeConfigEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private EmpresaEntity empresa(CommercialPlanType plan) {
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId("t1");
        empresa.setCommercialPlanType(plan);
        return empresa;
    }

    private SubscriptionPlanEntity avulsoPlan() {
        SubscriptionPlanEntity plan = new SubscriptionPlanEntity();
        plan.setId(5L);
        plan.setCode("AVULSO_100");
        plan.setName("Pacote 100 avaliações");
        plan.setPlanType(CommercialPlanType.AVULSO);
        plan.setPriceCents(49900);
        plan.setCurrency("BRL");
        plan.setCreditAmount(100);
        plan.setActive(true);
        return plan;
    }

    private EmpresaAutoRechargeConfigEntity config(boolean enabled, AutoRechargeStatus status, Instant lastTriggeredAt) {
        EmpresaAutoRechargeConfigEntity config = new EmpresaAutoRechargeConfigEntity();
        config.setEmpresaId("t1");
        config.setEnabled(enabled);
        config.setThresholdCredits(5);
        config.setPlanId(5L);
        config.setMpCustomerId("cust-1");
        config.setMpCardId("card-1");
        config.setStatus(status);
        config.setLastTriggeredAt(lastTriggeredAt);
        return config;
    }

    /** Deixa a configuração visível tanto no caminho rápido (findById) quanto na trava (forUpdate). */
    private void stubConfig(EmpresaAutoRechargeConfigEntity config) {
        when(configRepository.findById("t1")).thenReturn(Optional.of(config));
        lenient().when(configRepository.findByEmpresaIdForUpdate("t1")).thenReturn(Optional.of(config));
    }

    // ------------------------------------------------------------------
    // maybeRecharge — guardas
    // ------------------------------------------------------------------

    @Test
    void skipsWhenNoConfig() {
        when(configRepository.findById("t1")).thenReturn(Optional.empty());

        service.maybeRecharge("t1");

        verify(mercadoPagoClient, never()).chargeSavedCard(any(), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void skipsWhenDisabled() {
        stubConfig(config(false, AutoRechargeStatus.IDLE, null));

        service.maybeRecharge("t1");

        verify(mercadoPagoClient, never()).chargeSavedCard(any(), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void skipsWhenBalanceStillAboveThreshold() {
        stubConfig(config(true, AutoRechargeStatus.IDLE, null));
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO)));
        when(creditService.getBalance("t1")).thenReturn(5); // == threshold, ainda não é crítico

        service.maybeRecharge("t1");

        verify(mercadoPagoClient, never()).chargeSavedCard(any(), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void skipsWhenRechargeAlreadyPendingAndFresh() {
        stubConfig(config(true, AutoRechargeStatus.PENDING, Instant.now()));
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO)));
        when(creditService.getBalance("t1")).thenReturn(2);

        service.maybeRecharge("t1");

        // Já há uma cobrança recente em andamento: não dispara outra (evita cobrança dupla).
        verify(mercadoPagoClient, never()).chargeSavedCard(any(), anyString(), anyString(), anyString(), any(), anyString());
    }

    @Test
    void skipsWhenRecentFailureWithinCooldown() {
        stubConfig(config(true, AutoRechargeStatus.IDLE, Instant.now().minus(2, ChronoUnit.MINUTES)));
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO)));
        when(creditService.getBalance("t1")).thenReturn(2);

        service.maybeRecharge("t1");

        // Tentativa recente que não recompôs o saldo: aguarda a janela de espera.
        verify(mercadoPagoClient, never()).chargeSavedCard(any(), anyString(), anyString(), anyString(), any(), anyString());
    }

    // ------------------------------------------------------------------
    // maybeRecharge — disparo
    // ------------------------------------------------------------------

    @Test
    void chargesSavedCardAndConfirmsWhenBalanceBelowThreshold() throws Exception {
        EmpresaAutoRechargeConfigEntity config = config(true, AutoRechargeStatus.IDLE, null);
        stubConfig(config);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO)));
        when(creditService.getBalance("t1")).thenReturn(3);
        when(planRepository.findById(5L)).thenReturn(Optional.of(avulsoPlan()));
        var charge = objectMapper.readTree("{\"id\":\"pay-1\",\"status\":\"approved\"}");
        when(mercadoPagoClient.chargeSavedCard(any(), eq("cust-1"), eq("card-1"), anyString(), any(), anyString()))
                .thenReturn(charge);

        service.maybeRecharge("t1");

        // Trava o ciclo, cobra o cartão salvo e confirma pela fonte da verdade.
        assertThat(config.getStatus()).isEqualTo(AutoRechargeStatus.PENDING);
        assertThat(config.getPendingReference()).startsWith("autocredit:t1:5:");
        verify(mercadoPagoClient).chargeSavedCard(any(), eq("cust-1"), eq("card-1"),
                eq(config.getPendingReference()), any(Map.class), eq(config.getPendingReference()));
        verify(billingService).processPaymentNotification(eq("pay-1"), eq(config.getPendingReference()));

        ArgumentCaptor<EmpresaBillingEventEntity> event = ArgumentCaptor.forClass(EmpresaBillingEventEntity.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo(BillingEventType.CREDIT_AUTO_RECHARGE_TRIGGERED);
    }

    @Test
    void retriesWhenPendingIsStale() throws Exception {
        EmpresaAutoRechargeConfigEntity config = config(true, AutoRechargeStatus.PENDING,
                Instant.now().minus(45, ChronoUnit.MINUTES));
        stubConfig(config);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO)));
        when(creditService.getBalance("t1")).thenReturn(1);
        when(planRepository.findById(5L)).thenReturn(Optional.of(avulsoPlan()));
        var charge = objectMapper.readTree("{\"id\":\"pay-2\",\"status\":\"approved\"}");
        when(mercadoPagoClient.chargeSavedCard(any(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenReturn(charge);

        service.maybeRecharge("t1");

        // Recarga presa em PENDING há tempo demais: autocura e dispara nova cobrança.
        verify(mercadoPagoClient).chargeSavedCard(any(), anyString(), anyString(), anyString(), any(), anyString());
        verify(billingService).processPaymentNotification(eq("pay-2"), anyString());
    }

    @Test
    void marksFailedWhenChargeCommunicationFails() {
        EmpresaAutoRechargeConfigEntity config = config(true, AutoRechargeStatus.IDLE, null);
        stubConfig(config);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO)));
        when(creditService.getBalance("t1")).thenReturn(3);
        when(planRepository.findById(5L)).thenReturn(Optional.of(avulsoPlan()));
        when(mercadoPagoClient.chargeSavedCard(any(), anyString(), anyString(), anyString(), any(), anyString()))
                .thenThrow(new RuntimeException("MP fora do ar"));

        service.maybeRecharge("t1");

        // Cobrança não criada: volta a IDLE, registra a falha e NÃO tenta confirmar.
        assertThat(config.getStatus()).isEqualTo(AutoRechargeStatus.IDLE);
        assertThat(config.getPendingReference()).isNull();
        verify(billingService, never()).processPaymentNotification(anyString(), anyString());
        ArgumentCaptor<EmpresaBillingEventEntity> event = ArgumentCaptor.forClass(EmpresaBillingEventEntity.class);
        verify(eventRepository, org.mockito.Mockito.atLeastOnce()).save(event.capture());
        assertThat(event.getAllValues())
                .anyMatch(e -> e.getEventType() == BillingEventType.CREDIT_AUTO_RECHARGE_FAILED);
    }

    @Test
    void marksFailedWhenSavedCardMissing() {
        EmpresaAutoRechargeConfigEntity config = config(true, AutoRechargeStatus.IDLE, null);
        config.setMpCardId(null);
        stubConfig(config);
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO)));
        when(creditService.getBalance("t1")).thenReturn(3);
        when(planRepository.findById(5L)).thenReturn(Optional.of(avulsoPlan()));

        service.maybeRecharge("t1");

        assertThat(config.getStatus()).isEqualTo(AutoRechargeStatus.IDLE);
        verify(mercadoPagoClient, never()).chargeSavedCard(any(), anyString(), anyString(), anyString(), any(), anyString());
        ArgumentCaptor<EmpresaBillingEventEntity> event = ArgumentCaptor.forClass(EmpresaBillingEventEntity.class);
        verify(eventRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo(BillingEventType.CREDIT_AUTO_RECHARGE_FAILED);
    }

    // ------------------------------------------------------------------
    // configure
    // ------------------------------------------------------------------

    @Test
    void configureEnablesWithValidPlanAndCard() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO)));
        when(planRepository.findById(5L)).thenReturn(Optional.of(avulsoPlan()));
        when(configRepository.findById("t1")).thenReturn(Optional.empty());

        EmpresaAutoRechargeConfigEntity saved = service.configure("t1", true, 5, 5L, "cust-1", "card-1");

        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getThresholdCredits()).isEqualTo(5);
        assertThat(saved.getPlanId()).isEqualTo(5L);
        assertThat(saved.getStatus()).isEqualTo(AutoRechargeStatus.IDLE);
    }

    @Test
    void configureRejectsNonAvulsoEmpresa() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.PROFISSIONAL)));

        assertThatThrownBy(() -> service.configure("t1", true, 5, 5L, "cust-1", "card-1"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void configureRejectsEnableWithoutCard() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO)));

        assertThatThrownBy(() -> service.configure("t1", true, 5, 5L, null, null))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void configureRejectsNonPositiveThreshold() {
        when(empresaRepository.findById("t1")).thenReturn(Optional.of(empresa(CommercialPlanType.AVULSO)));

        assertThatThrownBy(() -> service.configure("t1", true, 0, 5L, "cust-1", "card-1"))
                .isInstanceOf(ResponseStatusException.class);
    }
}
