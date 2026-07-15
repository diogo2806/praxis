package br.com.iforce.praxis.gupy.delivery.controller;

import br.com.iforce.praxis.gupy.delivery.dto.CallbackDeliveryResponse;
import br.com.iforce.praxis.gupy.delivery.model.ResultDeliveryStatus;
import br.com.iforce.praxis.gupy.delivery.service.CallbackDeliveryService;
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
@RequestMapping("/api/v1/gupy/callback-deliveries")
@Tag(name = "Gupy Callback Delivery", description = "Confirmação servidor-servidor do callback_url por GET.")
public class CallbackDeliveryController {

    private final CallbackDeliveryService callbackDeliveryService;

    public CallbackDeliveryController(CallbackDeliveryService callbackDeliveryService) {
        this.callbackDeliveryService = callbackDeliveryService;
    }

    @GetMapping
    @Operation(
            summary = "Lista confirmações de callback",
            description = "Expõe tentativas, confirmação HTTP, erros, retry e DLQ do callback Gupy."
    )
    public ResponseEntity<List<CallbackDeliveryResponse>> listDeliveries(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(callbackDeliveryService.listDeliveries(ResultDeliveryStatus.fromString(status)));
    }

    @PostMapping("/{deliveryId}/reprocess")
    @Operation(
            summary = "Reprocessa callback manualmente",
            description = "Executa nova tentativa de GET para uma entrega de callback que ainda não foi confirmada."
    )
    public ResponseEntity<CallbackDeliveryResponse> reprocess(@PathVariable Long deliveryId) {
        return ResponseEntity.ok(callbackDeliveryService.reprocess(deliveryId));
    }
}
