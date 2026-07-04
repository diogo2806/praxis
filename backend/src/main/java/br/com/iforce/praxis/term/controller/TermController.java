package br.com.iforce.praxis.term.controller;

import br.com.iforce.praxis.term.dto.AcceptTermRequest;

import br.com.iforce.praxis.term.dto.TermAcceptanceStatusResponse;

import br.com.iforce.praxis.term.dto.TermResponse;

import br.com.iforce.praxis.term.service.TermAcceptanceService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;


/**
 * Porta de entrada (API) dos termos e seus aceites.
 *
 * <p>Na visão do processo, cuida de dois termos que o recrutador precisa
 * aceitar: o termo de responsabilidade (que reforça que a decisão final sobre
 * o candidato é humana) e o termo de uso da vertical de saúde (para tratar
 * dados sensíveis). É por aqui que a tela mostra o texto e a versão de cada
 * termo, consulta se o usuário atual já aceitou e registra o aceite.</p>
 */
@RestController
@RequestMapping("/api/v1/terms")
@Tag(name = "Termos", description = "Termo de responsabilidade do recrutador e registro de aceite (REQ-L5).")
public class TermController {

    private final TermAcceptanceService termAcceptanceService;

    /**
     * Conecta a API ao serviço que conhece as regras de aceite dos termos.
     *
     * <p>O controller só recebe a solicitação da tela e devolve a resposta HTTP; quem decide
     * qual termo está vigente, se ele já foi aceito e como gravar o aceite é o serviço.</p>
     *
     * @param termAcceptanceService serviço responsável pelo processo de aceite
     */
    public TermController(TermAcceptanceService termAcceptanceService) {
        this.termAcceptanceService = termAcceptanceService;
    }

    /**
     * Devolve o texto e a versão atual do termo de responsabilidade.
     *
     * <p>É usado pela tela para apresentar ao recrutador as responsabilidades assumidas
     * antes de permitir que ele avance no fluxo.</p>
     *
     * @return o conteúdo do termo de responsabilidade
     */
    @GetMapping("/responsibility")
    @Operation(summary = "Texto e versão do termo de responsabilidade")
    public ResponseEntity<TermResponse> responsibilityTerm() {
        return ResponseEntity.ok(termAcceptanceService.responsibilityTerm());
    }

    /**
     * Informa se o usuário logado já aceitou o termo de responsabilidade.
     *
     * <p>A tela usa essa informação para decidir se deve liberar o fluxo ou solicitar
     * que o usuário leia e confirme a versão atual do termo.</p>
     *
     * @return a situação de aceite do usuário atual
     */
    @GetMapping("/responsibility/acceptance")
    @Operation(summary = "Situação de aceite do usuário atual")
    public ResponseEntity<TermAcceptanceStatusResponse> responsibilityStatus() {
        return ResponseEntity.ok(termAcceptanceService.responsibilityStatus());
    }

    /**
     * Registra o aceite do termo de responsabilidade pelo usuário logado.
     *
     * <p>É chamado quando a pessoa confirma na interface que leu e aceitou a versão
     * exibida. O retorno mostra o novo status para atualizar a experiência imediatamente.</p>
     *
     * @param request dados do aceite, incluindo a versão aceita
     * @return a situação de aceite atualizada
     */
    @PostMapping("/responsibility/acceptance")
    @Operation(summary = "Registra o aceite do termo de responsabilidade")
    public ResponseEntity<TermAcceptanceStatusResponse> acceptResponsibility(
            @Valid @RequestBody AcceptTermRequest request
    ) {
        return ResponseEntity.ok(termAcceptanceService.acceptResponsibility(request));
    }

    /**
     * Devolve o texto e a versão atual do termo de uso na vertical de saúde.
     *
     * <p>Esse termo aparece quando o processo envolve dados sensíveis de saúde e precisa
     * deixar claras as responsabilidades adicionais antes da publicação de provas.</p>
     *
     * @return o conteúdo do termo de uso em saúde
     */
    @GetMapping("/health-use")
    @Operation(summary = "Texto e versão do termo de uso na vertical de saúde")
    public ResponseEntity<TermResponse> healthUseTerm() {
        return ResponseEntity.ok(termAcceptanceService.healthUseTerm());
    }

    /**
     * Informa se o usuário logado já aceitou o termo de uso em saúde.
     *
     * <p>A resposta permite que o produto bloqueie ou libere funcionalidades da vertical
     * de saúde conforme o aceite da versão vigente.</p>
     *
     * @return a situação de aceite do usuário atual
     */
    @GetMapping("/health-use/acceptance")
    @Operation(summary = "Situação de aceite do termo de uso em saúde pelo usuário atual")
    public ResponseEntity<TermAcceptanceStatusResponse> healthUseStatus() {
        return ResponseEntity.ok(termAcceptanceService.healthUseStatus());
    }

    /**
     * Registra o aceite do termo de uso em saúde pelo usuário logado.
     *
     * <p>É a confirmação formal de que a pessoa responsável entendeu as condições de uso
     * para processos relacionados à saúde antes de seguir com o fluxo.</p>
     *
     * @param request dados do aceite, incluindo a versão aceita
     * @return a situação de aceite atualizada
     */
    @PostMapping("/health-use/acceptance")
    @Operation(summary = "Registra o aceite do termo de uso em saúde")
    public ResponseEntity<TermAcceptanceStatusResponse> acceptHealthUse(
            @Valid @RequestBody AcceptTermRequest request
    ) {
        return ResponseEntity.ok(termAcceptanceService.acceptHealthUse(request));
    }
}
