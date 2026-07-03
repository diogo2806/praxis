package br.com.iforce.praxis.billing.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.billing.dto.AutoRechargeConfigRequest;

import br.com.iforce.praxis.billing.dto.AutoRechargeConfigResponse;

import br.com.iforce.praxis.billing.service.AutoRechargeService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PutMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


/**
 * Tela de recarga automática (auto-top-up) do próprio cliente pré-pago (AVULSO).
 *
 * <p>Na visão do processo, é por aqui que a empresa logada liga a "reposição automática" dos seus
 * créditos e ajusta as suas condições — a partir de qual saldo recarregar, qual pacote comprar e
 * qual cartão salvo usar — sem precisar acionar a operação. O componente descobre qual é a empresa
 * pelo login (ninguém configura a recarga de outra) e repassa a regra para a
 * {@link AutoRechargeService}. Está sob {@code /api/v1/billing/**}, já restrito ao papel EMPRESA.</p>
 */
@RestController
@RequestMapping("/api/v1/billing/auto-recharge")
@Tag(name = "Billing · Recarga automática", description = "Auto-top-up de créditos pré-pagos (AVULSO) do cliente autenticado.")
public class AutoRechargeController {

    private final CurrentEmpresaService currentEmpresaService;
    private final AutoRechargeService autoRechargeService;

    public AutoRechargeController(CurrentEmpresaService currentEmpresaService,
                                  AutoRechargeService autoRechargeService) {
        this.currentEmpresaService = currentEmpresaService;
        this.autoRechargeService = autoRechargeService;
    }

    /**
     * Devolve a configuração de recarga automática do cliente logado (desligada por padrão se ele
     * nunca configurou).
     *
     * @return a configuração atual de recarga automática, sem expor os dados do cartão
     */
    @GetMapping
    @Operation(summary = "Consulta a recarga automática do cliente")
    public ResponseEntity<AutoRechargeConfigResponse> getConfig() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return ResponseEntity.ok(AutoRechargeConfigResponse.from(autoRechargeService.getConfig(empresaId)));
    }

    /**
     * Liga/desliga e ajusta a recarga automática do cliente logado. Ao ligar, o pacote de créditos e
     * o cartão salvo passam a ser obrigatórios.
     *
     * @param request preferências de recarga automática do cliente
     * @return a configuração salva, sem expor os dados do cartão
     */
    @PutMapping
    @Operation(summary = "Liga/desliga e ajusta a recarga automática do cliente")
    public ResponseEntity<AutoRechargeConfigResponse> configure(
            @Valid @RequestBody AutoRechargeConfigRequest request
    ) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        var config = autoRechargeService.configure(
                empresaId,
                request.enabled(),
                request.thresholdCredits(),
                request.planId(),
                request.mpCustomerId(),
                request.mpCardId());
        return ResponseEntity.ok(AutoRechargeConfigResponse.from(config));
    }
}
