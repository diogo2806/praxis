package br.com.iforce.praxis.gupy.delivery.controller;

import br.com.iforce.praxis.gupy.delivery.dto.CallbackDeliveryResponse;
import br.com.iforce.praxis.gupy.delivery.model.ResultDeliveryStatus;
import br.com.iforce.praxis.gupy.delivery.service.CallbackDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/gupy/callback-deliveries")
@Tag(
        name = "Gupy Callback Delivery",
        description = "Consulta somente leitura do histórico legado de callbacks servidor-servidor desativados."
)
public class CallbackDeliveryController {

    private final CallbackDeliveryService callbackDeliveryService;

    public CallbackDeliveryController(CallbackDeliveryService callbackDeliveryService) {
        this.callbackDeliveryService = callbackDeliveryService;
    }

    @GetMapping
    @Operation(
            summary = "Lista o histórico legado de callbacks",
            description = "Exibe registros históricos. O callback_url é aberto pelo navegador da pessoa candidata e não pode ser reprocessado pelo servidor."
    )
    public ResponseEntity<List<CallbackDeliveryResponse>> listDeliveries(
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(callbackDeliveryService.listDeliveries(ResultDeliveryStatus.fromString(status)));
    }
}
