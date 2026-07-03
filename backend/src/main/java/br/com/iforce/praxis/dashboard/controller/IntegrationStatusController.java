package br.com.iforce.praxis.dashboard.controller;

import br.com.iforce.praxis.dashboard.dto.DashboardResponse.IntegrationStatusItem;

import br.com.iforce.praxis.dashboard.service.DashboardService;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


import java.util.List;


/**
 * Porta de entrada da consulta ao estado das integrações da empresa.
 *
 * <p>Na visão do processo, o Praxis funciona ligado a sistemas externos de
 * recrutamento (como Gupy, Recrutei ou uma API própria da empresa). Esta é a
 * tela/aba onde o RH confere, para cada um desses parceiros, se a conexão está
 * ativa, pendente, com erro ou ainda não configurada — e, quando falta algo,
 * qual é o próximo passo. É o mesmo levantamento que aparece resumido no painel
 * inicial, exposto aqui de forma isolada para a área de integrações.</p>
 */
@RestController
@RequestMapping("/api/v1/integrations/status")
public class IntegrationStatusController {

    private final DashboardService dashboardService;

    public IntegrationStatusController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Lista o estado de cada integração disponível para a empresa logada.
     *
     * <p>Fluxo do processo: identifica a empresa do usuário e devolve, para
     * cada parceiro suportado, se a conexão está ativa, pendente, com erro,
     * desativada ou não configurada, junto da data da última sincronização e da
     * ação sugerida quando ainda há algo a configurar.</p>
     *
     * @return a situação de cada integração da empresa logada
     */
    @GetMapping
    public ResponseEntity<List<IntegrationStatusItem>> getStatus() {
        return ResponseEntity.ok(dashboardService.getIntegrationStatuses());
    }
}
