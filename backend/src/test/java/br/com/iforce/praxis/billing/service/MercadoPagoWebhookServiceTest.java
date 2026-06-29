package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.billing.persistence.entity.MpWebhookReceiptEntity;
import br.com.iforce.praxis.billing.persistence.repository.MpWebhookReceiptRepository;
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

    private MercadoPagoWebhookService service;

    @BeforeEach
    void setUp() {
        service = new MercadoPagoWebhookService(signatureValidator, receiptRepository, billingService);
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
        when(receiptRepository.existsByNotificationId("payment:123")).thenReturn(false);

        service.handle("payment", "123", "sig", "req", "{}");

        verify(billingService).processPaymentNotification("123", "req");
    }

    @Test
    void skipsDuplicateNotification() {
        when(signatureValidator.isValid(any(), any(), any())).thenReturn(true);
        when(receiptRepository.existsByNotificationId("payment:123")).thenReturn(true);

        service.handle("payment", "123", "sig", "req", "{}");

        verify(billingService, never()).processPaymentNotification(anyString(), anyString());
        verify(receiptRepository, never()).saveAndFlush(any());
    }

    @Test
    void routesPreapprovalTopic() {
        when(signatureValidator.isValid(any(), any(), any())).thenReturn(true);
        when(receiptRepository.existsByNotificationId("subscription_preapproval:pa1")).thenReturn(false);

        service.handle("subscription_preapproval", "pa1", "sig", "req", "{}");

        verify(billingService).processPreapprovalNotification("pa1", "req");
    }
}
