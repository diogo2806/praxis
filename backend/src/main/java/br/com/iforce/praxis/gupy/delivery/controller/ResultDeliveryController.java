package br.com.iforce.praxis.gupy.delivery.controller;

import br.com.iforce.praxis.gupy.delivery.dto.ProcessReadyDeliveriesResponse;
import br.com.iforce.praxis.gupy.delivery.dto.ReprocessDeliveryResponse;
import br.com.iforce.praxis.gupy.delivery.dto.ResultDeliveryResponse;
import br.com.iforce.praxis.gupy.delivery.model.ResultDeliveryStatus;
import br.com.iforce.praxis.gupy.delivery.service.ResultDeliveryService;
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

@RestController
@RequestMapping("/api/v1/gupy/result-deliveries")
@Tag(name = "Gupy Result Delivery", description = "Fila de entrega assíncrona de resultados para result_webhook_url.")
public class ResultDeliveryController {

    private final ResultDeliveryService resultDeliveryService;

    public ResultDeliveryController(ResultDeliveryService resultDeliveryService) {
        this.resultDeliveryService = resultDeliveryService;
    }

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

    @GetMapping("/ready")
    @Operation(summary = "Lista entregas prontas para retry", description = "Retorna entregas pendentes ou em retry com nextAttemptAt vencido.")
    public ResponseEntity<List<ResultDeliveryResponse>> listReadyForRetry() {
        return ResponseEntity.ok(resultDeliveryService.listReadyForRetry());
    }

    @PostMapping("/process-ready")
    @Operation(summary = "Processa entregas prontas", description = "Executa em lote as entregas pendentes ou em retry com nextAttemptAt vencido.")
    public ResponseEntity<ProcessReadyDeliveriesResponse> processReadyDeliveries() {
        return ResponseEntity.ok(resultDeliveryService.processReadyDeliveries());
    }

    @PostMapping("/{deliveryId}/reprocess")
    @Operation(summary = "Reprocessa entrega manualmente", description = "Tenta postar o TestResult novamente no result_webhook_url.")
    public ResponseEntity<ReprocessDeliveryResponse> reprocessDelivery(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(resultDeliveryService.reprocessDelivery(deliveryId));
    }
}
