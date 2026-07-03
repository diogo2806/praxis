package br.com.iforce.praxis.billing.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.billing.dto.ClientBillingResponse;

import br.com.iforce.praxis.billing.service.ClientBillingService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


/**
 * Porta de entrada (API) da tela de "Plano e cobrança" do próprio cliente.
 *
 * <p>Na visão do processo, é por aqui que a empresa logada consulta a sua própria conta: o plano,
 * a situação, o saldo de créditos, o uso e o histórico financeiro. Este componente só recebe o
 * pedido vindo da tela — descobrindo qual é a empresa a partir do login, de modo que ninguém veja
 * a cobrança de outra — e repassa o trabalho para a regra de negócio ({@link ClientBillingService}).
 * São endpoints apenas de leitura: nada é cobrado ou alterado por aqui (isso fica no painel
 * ADMIN).</p>
 */
@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing · Cliente", description = "Visão de plano e uso para o cliente autenticado.")
public class ClientBillingController {

    private final CurrentEmpresaService currentEmpresaService;
    private final ClientBillingService clientBillingService;

    public ClientBillingController(CurrentEmpresaService currentEmpresaService,
                                   ClientBillingService clientBillingService) {
        this.currentEmpresaService = currentEmpresaService;
        this.clientBillingService = clientBillingService;
    }

    /**
     * Devolve a visão completa de plano, situação, saldo, uso e histórico do cliente logado, para
     * montar a tela de "Plano e cobrança".
     *
     * @return a visão consolidada de cobrança do próprio cliente
     */
    @GetMapping
    @Operation(summary = "Plano, uso e cobrança do empresa autenticado")
    public ResponseEntity<ClientBillingResponse> getBilling() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return ResponseEntity.ok(clientBillingService.getBilling(empresaId));
    }

    /**
     * Devolve apenas o extrato de eventos financeiros do cliente logado, para a tela de histórico.
     *
     * @return os eventos financeiros do próprio cliente, do mais recente para o mais antigo
     */
    @GetMapping("/events")
    @Operation(summary = "Histórico de eventos financeiros do empresa autenticado")
    public ResponseEntity<java.util.List<br.com.iforce.praxis.billing.dto.BillingEventResponse>> getEvents() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return ResponseEntity.ok(clientBillingService.getEvents(empresaId));
    }
}
