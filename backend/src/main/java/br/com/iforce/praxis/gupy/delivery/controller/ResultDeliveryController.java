package br.com.iforce.praxis.gupy.delivery.controller;

import br.com.iforce.praxis.gupy.delivery.dto.ProcessReadyDeliveriesResponse;
import br.com.iforce.praxis.gupy.delivery.dto.ReprocessDeliveryResponse;
import br.com.iforce.praxis.gupy.delivery.dto.ResultDeliveryResponse;
import br.com.iforce.praxis.gupy.delivery.model.ResultDeliveryStatus;
import br.com.iforce.praxis.gupy.delivery.service.OutboxResultDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Porta de entrada (API) para acompanhar a entrega de resultados à Gupy.
 *
 * <p>Na visão do processo, quando uma prova termina, o resultado é enviado de
 * volta para a Gupy de forma automática e assíncrona (numa "fila de entrega").
 * É por aqui que a equipe monitora essa fila: vê o que já foi entregue, o que
 * está aguardando nova tentativa, o que falhou definitivamente, e pode
 * disparar o reenvio manualmente quando necessário.</p>
 */
@RestController
@RequestMapping("/api/v1/gupy/result-deliveries")
@Tag(name = "Gupy Result Delivery", description = "Fila de entrega assíncrona de resultados para result_webhook_url.")
public class ResultDeliveryController {

    private final OutboxResultDeliveryService resultDeliveryService;

    public ResultDeliveryController(OutboxResultDeliveryService resultDeliveryService) {
        this.resultDeliveryService = resultDeliveryService;
    }

    /**
     * Lista as entregas de resultado para acompanhamento.
     *
     * @param status filtro opcional pela situação (enviada, em retentativa, falha definitiva)
     * @param simulationId filtro opcional pela prova
     * @param versionNumber filtro opcional pela versão da prova
     * @return as entregas que atendem aos filtros informados
     */
    @GetMapping
    @Operation(
            summary = "Lista entregas de resultado",
            description = "Permite monitorar entregas enviadas, em retry e DLQ, com filtro opcional por simulacao e versao."
    )
    public ResponseEntity<List<ResultDeliveryResponse>> listDeliveries(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String simulationId,
            @RequestParam(required = false) Integer versionNumber
    ) {
        ResultDeliveryStatus resultDeliveryStatus = ResultDeliveryStatus.fromString(status);
        return ResponseEntity.ok(resultDeliveryService.listDeliveries(resultDeliveryStatus, simulationId, versionNumber));
    }

    /**
     * Lista as entregas que já estão prontas para uma nova tentativa de envio
     * (pendentes ou em retentativa cujo horário agendado já chegou).
     *
     * @return as entregas prontas para serem reenviadas
     */
    @GetMapping("/ready")
    @Operation(summary = "Lista entregas prontas para retry", description = "Retorna entregas pendentes ou em retry com nextAttemptAt vencido.")
    public ResponseEntity<List<ResultDeliveryResponse>> listReadyForRetry() {
        return ResponseEntity.ok(resultDeliveryService.listReadyForRetry());
    }

    /**
     * Processa de uma vez (em lote) todas as entregas prontas para reenvio.
     *
     * <p>Dispara as tentativas de envio que já venceram o prazo de espera e
     * informa quantas foram processadas.</p>
     *
     * @return o total processado e as entregas ainda pendentes
     */
    @PostMapping("/process-ready")
    @Operation(summary = "Processa entregas prontas", description = "Executa em lote as entregas pendentes ou em retry com nextAttemptAt vencido.")
    public ResponseEntity<ProcessReadyDeliveriesResponse> processReadyDeliveries() {
        return ResponseEntity.ok(resultDeliveryService.processReadyDeliveries());
    }

    /**
     * Reenvia manualmente uma entrega específica.
     *
     * <p>Usado quando a equipe quer forçar uma nova tentativa de envio de um
     * resultado para a Gupy (por exemplo, após corrigir um problema).</p>
     *
     * @param deliveryId identificador da entrega que será reenviada
     * @return a situação atualizada da entrega após a tentativa
     */
    @PostMapping("/{deliveryId}/reprocess")
    @Operation(summary = "Reprocessa entrega manualmente", description = "Tenta postar o TestResult novamente no result_webhook_url.")
    public ResponseEntity<ReprocessDeliveryResponse> reprocessDelivery(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(resultDeliveryService.reprocessDelivery(deliveryId));
    }
}
