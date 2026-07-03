package br.com.iforce.praxis.billing.service;

import br.com.iforce.praxis.billing.persistence.entity.MpWebhookReceiptEntity;

import br.com.iforce.praxis.billing.persistence.repository.MpWebhookReceiptRepository;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


import java.time.Instant;


/**
 * Recebe e processa notificações do Mercado Pago.
 *
 * <p>Fluxo: valida a assinatura, captura o {@code x-request-id}, salva o payload bruto, aplica
 * idempotência (por tópico + id do recurso) e dispara o processamento — que consulta o Mercado
 * Pago antes de aplicar qualquer mudança financeira.</p>
 */
@Service
public class MercadoPagoWebhookService {

    private static final Logger log = LoggerFactory.getLogger(MercadoPagoWebhookService.class);

    private final MercadoPagoSignatureValidator signatureValidator;
    private final MpWebhookReceiptRepository receiptRepository;
    private final BillingService billingService;

    public MercadoPagoWebhookService(
            MercadoPagoSignatureValidator signatureValidator,
            MpWebhookReceiptRepository receiptRepository,
            BillingService billingService
    ) {
        this.signatureValidator = signatureValidator;
        this.receiptRepository = receiptRepository;
        this.billingService = billingService;
    }

    /**
     * @param topic      tipo da notificação (payment, subscription_preapproval, ...)
     * @param dataId     id do recurso referenciado
     * @param xSignature cabeçalho de assinatura
     * @param xRequestId cabeçalho de rastreio
     * @param rawPayload corpo bruto recebido
     */
    @Transactional
    public void handle(String topic, String dataId, String xSignature, String xRequestId, String rawPayload) {
        if (!signatureValidator.isValid(xSignature, xRequestId, dataId)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Assinatura do webhook inválida.");
        }
        if (topic == null || dataId == null || dataId.isBlank()) {
            // Notificações de teste/keep-alive sem recurso: aceitas sem efeito.
            return;
        }

        // Idempotencia por (topico + id do recurso). NAO inclui x-request-id: o Mercado Pago
        // gera um x-request-id novo a cada tentativa de entrega do MESMO evento, entao usa-lo na
        // chave permitiria reprocessar o mesmo pagamento e, por exemplo, creditar em dobro.
        String notificationId = topic + ":" + dataId;
        if (receiptRepository.existsByNotificationId(notificationId)) {
            log.debug("Webhook {} já processado; ignorado (idempotência).", notificationId);
            return;
        }

        MpWebhookReceiptEntity receipt = new MpWebhookReceiptEntity();
        receipt.setNotificationId(notificationId);
        receipt.setTopic(topic);
        receipt.setResourceId(dataId);
        receipt.setRequestId(xRequestId);
        receipt.setRawPayload(truncate(rawPayload));
        receipt.setProcessed(false);
        receipt.setCreatedAt(Instant.now());
        try {
            receiptRepository.saveAndFlush(receipt);
        } catch (DataIntegrityViolationException duplicate) {
            log.debug("Webhook {} recebido em paralelo; ignorado (idempotência).", notificationId);
            return;
        }

        dispatch(topic, dataId, xRequestId);
        receipt.setProcessed(true);
    }

    private void dispatch(String topic, String dataId, String xRequestId) {
        String normalized = topic.toLowerCase();
        if (normalized.contains("preapproval")) {
            billingService.processPreapprovalNotification(dataId, xRequestId);
        } else if (normalized.contains("payment")) {
            billingService.processPaymentNotification(dataId, xRequestId);
        } else {
            log.debug("Tópico de webhook não tratado: {}", topic);
        }
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() > 8000 ? value.substring(0, 8000) : value;
    }
}
