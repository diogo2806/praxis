package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.billing.persistence.entity.MpWebhookReceiptEntity;

import br.com.iforce.praxis.billing.persistence.repository.MpWebhookReceiptRepository;
import br.com.iforce.praxis.marketplace.service.MarketplaceOrderService;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.web.server.ResponseStatusException;


import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;

import static org.mockito.Mockito.lenient;

import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class MercadoPagoWebhookServiceTest {

    @Mock private MercadoPagoSignatureValidator signatureValidator;
    @Mock private MpWebhookReceiptRepository receiptRepository;
    @Mock private BillingService billingService;
    @Mock private MercadoPagoClient mercadoPagoClient;
    @Mock private MarketplaceOrderService marketplaceOrderService;

    private MercadoPagoWebhookService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new MercadoPagoWebhookService(
                signatureValidator,
                receiptRepository,
                billingService,
                mercadoPagoClient,
                marketplaceOrderService
        );
        lenient().when(receiptRepository.saveAndFlush(any(MpWebhookReceiptEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void rejectsInvalidSignature() {
        when(signatureValidator.isValid(anyString(), anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.handle("payment", "123", "sig", "req", "{}"))
                .isInstanceOf(ResponseStatusException.class);

        verify(billingService, never()).processPaymentNotification(anyString(), anyString());
    }

    @Test
    void dispatchesPaymentOnce() {
        when(signatureValidator.isValid(any(), any(), any())).thenReturn(true);
        when(receiptRepository.existsByNotificationId("req:payment:123")).thenReturn(false);
        when(mercadoPagoClient.getPayment("123")).thenReturn(objectMapper.createObjectNode());

        service.handle("payment", "123", "sig", "req", "{}");

        verify(mercadoPagoClient).getPayment("123");
        verify(billingService).processPaymentNotification("123", "req");
    }

    @Test
    void skipsDuplicateNotification() {
        when(signatureValidator.isValid(any(), any(), any())).thenReturn(true);
        when(receiptRepository.existsByNotificationId("req:payment:123")).thenReturn(true);

        service.handle("payment", "123", "sig", "req", "{}");

        verify(billingService, never()).processPaymentNotification(anyString(), anyString());
        verify(receiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void routesPreapprovalTopic() {
        when(signatureValidator.isValid(any(), any(), any())).thenReturn(true);
        when(receiptRepository.existsByNotificationId("req:subscription_preapproval:pa1")).thenReturn(false);

        service.handle("subscription_preapproval", "pa1", "sig", "req", "{}");

        verify(billingService).processPreapprovalNotification("pa1", "req");
    }

    @Test
    void routesMarketplacePaymentToMarketplaceOrderService() throws Exception {
        when(signatureValidator.isValid(any(), any(), any())).thenReturn(true);
        when(receiptRepository.existsByNotificationId("req:payment:mp1")).thenReturn(false);
        var payment = objectMapper.readTree(
                "{\"metadata\":{\"order_type\":\"marketplace\"},\"external_reference\":\"marketplace:10\"}"
        );
        when(mercadoPagoClient.getPayment("mp1")).thenReturn(payment);

        service.handle("payment", "mp1", "sig", "req", "{}");

        verify(marketplaceOrderService).processApprovedPayment(payment, "req");
        verify(billingService, never()).processPaymentNotification(anyString(), anyString());
    }
}
