package br.com.iforce.praxis.dashboard.controller;

import br.com.iforce.praxis.dashboard.dto.DashboardResponse;

import br.com.iforce.praxis.dashboard.service.DashboardService;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


/**
 * Porta de entrada da tela inicial (painel) da empresa.
 *
 * <p>Na visão do processo, é a primeira coisa que o RH vê ao entrar no
 * sistema: um resumo do que está acontecendo na operação — quantas avaliações
 * estão no ar, quantos candidatos estão respondendo agora, os últimos
 * resultados, o estado das integrações, o consumo do plano e o que convém
 * fazer a seguir. Este componente apenas recebe o pedido da tela e devolve
 * esse resumo já montado; quem calcula os números é o {@link DashboardService}.</p>
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Entrega o resumo completo do painel para a empresa que está logada.
     *
     * <p>Fluxo do processo: quando a tela inicial é aberta, ela chama este
     * endpoint; o sistema identifica a empresa do usuário, reúne os indicadores
     * da operação (avaliações ativas, jornadas, candidatos em andamento,
     * últimos resultados, integrações, uso do plano e ações recomendadas) e
     * devolve tudo pronto para ser exibido.</p>
     *
     * @return o resumo do painel da empresa logada
     */
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }
}
