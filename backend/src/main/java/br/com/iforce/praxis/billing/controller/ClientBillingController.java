package br.com.iforce.praxis.billing.controller;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;

import br.com.iforce.praxis.billing.dto.BillingEventResponse;

import br.com.iforce.praxis.billing.dto.CheckoutResponse;

import br.com.iforce.praxis.billing.dto.ClientBillingResponse;

import br.com.iforce.praxis.billing.dto.SubscriptionPlanResponse;

import br.com.iforce.praxis.billing.service.ClientBillingService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;


import java.util.List;


/**
 * Porta de entrada (API) da tela de "Plano e cobrança" do próprio cliente.
 *
 * <p>Na visão do processo, é por aqui que a empresa logada consulta e resolve a sua própria conta:
 * vê plano, situação, saldo, uso e histórico financeiro; escolhe um pacote de créditos; abre o
 * checkout de assinatura; e reconfere a assinatura atual depois de autorizar ou corrigir pagamento.
 * O componente descobre qual é a empresa a partir do login, de modo que ninguém veja ou altere a
 * cobrança de outra empresa.</p>
 *
 * <p>Mesmo sendo self-service, a confirmação financeira continua segura: a API só cria links de
 * checkout ou pede nova leitura do Mercado Pago. Créditos, ativação e regularização só acontecem
 * quando o Mercado Pago é consultado pelo motor de cobrança.</p>
 */
@RestController
@RequestMapping("/api/v1/billing")
@Tag(name = "Billing · Cliente", description = "Visão de plano, uso e checkout para o cliente autenticado.")
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
     * Lista os planos e pacotes ativos que podem ser escolhidos no self-service de cobrança.
     *
     * @return planos ativos disponíveis para checkout do cliente
     */
    @GetMapping("/plans")
    @Operation(summary = "Lista planos disponíveis para self-service")
    public ResponseEntity<List<SubscriptionPlanResponse>> plans() {
        return ResponseEntity.ok(clientBillingService.getAvailablePlans());
    }

    /**
     * Cria o checkout de compra de créditos para o próprio cliente AVULSO.
     *
     * @param planId identificador do pacote de créditos escolhido
     * @return link do Mercado Pago para pagamento do pacote
     */
    @PostMapping("/credits/checkout")
    @Operation(summary = "Cria checkout de créditos para o cliente autenticado")
    public ResponseEntity<CheckoutResponse> creditCheckout(@RequestParam Long planId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return ResponseEntity.ok(clientBillingService.createCreditCheckout(empresaId, planId));
    }

    /**
     * Cria o checkout de autorização de assinatura para o próprio cliente PROFISSIONAL.
     *
     * @param planId identificador do plano recorrente escolhido
     * @return link do Mercado Pago para autorização da assinatura
     */
    @PostMapping("/subscription/checkout")
    @Operation(summary = "Cria checkout de assinatura para o cliente autenticado")
    public ResponseEntity<CheckoutResponse> subscriptionCheckout(@RequestParam Long planId) {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return ResponseEntity.ok(clientBillingService.createSubscriptionCheckout(empresaId, planId));
    }

    /**
     * Reconfere a assinatura atual do próprio cliente no Mercado Pago e devolve a visão atualizada.
     *
     * @return visão de cobrança atualizada após a sincronização
     */
    @PostMapping("/subscription/sync")
    @Operation(summary = "Sincroniza a assinatura do cliente autenticado")
    public ResponseEntity<ClientBillingResponse> syncSubscription() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return ResponseEntity.ok(clientBillingService.syncCurrentSubscription(empresaId));
    }

    /**
     * Devolve apenas o extrato de eventos financeiros do cliente logado, para a tela de histórico.
     *
     * @return os eventos financeiros do próprio cliente, do mais recente para o mais antigo
     */
    @GetMapping("/events")
    @Operation(summary = "Histórico de eventos financeiros do empresa autenticado")
    public ResponseEntity<List<BillingEventResponse>> getEvents() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        return ResponseEntity.ok(clientBillingService.getEvents(empresaId));
    }
}
