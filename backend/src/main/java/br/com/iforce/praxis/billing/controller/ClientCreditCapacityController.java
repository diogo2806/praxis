package br.com.iforce.praxis.billing.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.billing.dto.CreditCapacityResponse;
import br.com.iforce.praxis.billing.service.CreditCapacityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Consulta de saldo, reservas e capacidade para iniciar novas avaliações. */
@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Client Billing", description = "Cobrança e capacidade de uso da empresa autenticada.")
public class ClientCreditCapacityController {

    private final CreditCapacityService creditCapacityService;
    private final CurrentEmpresaService currentEmpresaService;

    public ClientCreditCapacityController(
            CreditCapacityService creditCapacityService,
            CurrentEmpresaService currentEmpresaService
    ) {
        this.creditCapacityService = creditCapacityService;
        this.currentEmpresaService = currentEmpresaService;
    }

    @GetMapping("/credit-capacity")
    @Operation(
            summary = "Consulta créditos livres para novas avaliações",
            description = "Diferencia saldo total, créditos reservados por tentativas abertas e capacidade disponível."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Capacidade retornada."),
            @ApiResponse(responseCode = "403", description = "Acesso negado."),
            @ApiResponse(responseCode = "404", description = "Empresa não encontrada.")
    })
    public ResponseEntity<CreditCapacityResponse> getCapacity() {
        return ResponseEntity.ok(
                creditCapacityService.getCapacity(currentEmpresaService.requiredEmpresaId())
        );
    }
}
