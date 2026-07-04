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
 * <p>Na visão do processo, este controller funciona como o balcão onde o produto
 * apresenta os termos obrigatórios ao recrutador, confirma se ele já assumiu as
 * responsabilidades necessárias e registra formalmente o aceite. Isso cobre dois
 * cuidados de governança: deixar claro que a decisão final sobre candidatos é
 * humana e, quando houver uso na vertical de saúde, reforçar as regras de uso de
 * dados sensíveis.</p>
 */
@RestController
@RequestMapping("/api/v1/terms")
@Tag(name = "Termos", description = "Termo de responsabilidade do recrutador e registro de aceite (REQ-L5).")
public class TermController {

    private final TermAcceptanceService termAcceptanceService;

    /**
     * Conecta a API de termos ao serviço que conhece as regras de exibição,
     * consulta e registro dos aceites.
     *
     * @param termAcceptanceService serviço responsável pelo processo de aceite dos termos
     */
    public TermController(TermAcceptanceService termAcceptanceService) {
        this.termAcceptanceService = termAcceptanceService;
    }

    /**
     * Entrega para a tela o texto vigente do termo de responsabilidade.
     *
     * <p>No processo, este é o passo em que o usuário lê a declaração que reforça
     * que a Práxis apoia a avaliação, mas não substitui a decisão humana da
     * empresa.</p>
     *
     * @return o conteúdo e a versão atual do termo de responsabilidade
     */
    @GetMapping("/responsibility")
    @Operation(summary = "Texto e versão do termo de responsabilidade")
    public ResponseEntity<TermResponse> responsibilityTerm() {
        return ResponseEntity.ok(termAcceptanceService.responsibilityTerm());
    }

    /**
     * Informa para a tela se o usuário atual já aceitou o termo de responsabilidade.
     *
     * <p>No processo, evita pedir um novo aceite quando a pessoa já aceitou a
     * versão vigente e permite bloquear ou orientar o usuário quando ainda falta
     * concluir essa etapa.</p>
     *
     * @return a situação de aceite do usuário atual para a versão vigente
     */
    @GetMapping("/responsibility/acceptance")
    @Operation(summary = "Situação de aceite do usuário atual")
    public ResponseEntity<TermAcceptanceStatusResponse> responsibilityStatus() {
        return ResponseEntity.ok(termAcceptanceService.responsibilityStatus());
    }

    /**
     * Registra que o usuário logado aceitou o termo de responsabilidade.
     *
     * <p>No processo, este é o momento em que a plataforma guarda a evidência de
     * que uma pessoa autorizada leu e assumiu a versão exibida do termo, com data,
     * usuário e empresa vinculados ao registro.</p>
     *
     * @param request versão do termo exibida e confirmada pelo usuário
     * @return a situação de aceite atualizada após o registro
     */
    @PostMapping("/responsibility/acceptance")
    @Operation(summary = "Registra o aceite do termo de responsabilidade")
    public ResponseEntity<TermAcceptanceStatusResponse> acceptResponsibility(
            @Valid @RequestBody AcceptTermRequest request
    ) {
        return ResponseEntity.ok(termAcceptanceService.acceptResponsibility(request));
    }

    /**
     * Entrega para a tela o texto vigente do termo de uso na vertical de saúde.
     *
     * <p>No processo, apresenta os compromissos adicionais exigidos quando a
     * empresa usa a plataforma em cenários que podem envolver dados sensíveis de
     * saúde.</p>
     *
     * @return o conteúdo e a versão atual do termo de uso em saúde
     */
    @GetMapping("/health-use")
    @Operation(summary = "Texto e versão do termo de uso na vertical de saúde")
    public ResponseEntity<TermResponse> healthUseTerm() {
        return ResponseEntity.ok(termAcceptanceService.healthUseTerm());
    }

    /**
     * Informa para a tela se o usuário atual já aceitou o termo de uso em saúde.
     *
     * <p>No processo, permite liberar a continuidade da operação quando o aceite
     * já existe ou sinalizar que o usuário ainda precisa confirmar as regras antes
     * de publicar avaliações nessa vertical.</p>
     *
     * @return a situação de aceite do usuário atual para o termo de uso em saúde
     */
    @GetMapping("/health-use/acceptance")
    @Operation(summary = "Situação de aceite do termo de uso em saúde pelo usuário atual")
    public ResponseEntity<TermAcceptanceStatusResponse> healthUseStatus() {
        return ResponseEntity.ok(termAcceptanceService.healthUseStatus());
    }

    /**
     * Registra que o usuário logado aceitou o termo de uso em saúde.
     *
     * <p>No processo, cria a comprovação de que a empresa reconheceu os cuidados
     * adicionais de uso de dados sensíveis antes de seguir com avaliações da
     * vertical de saúde.</p>
     *
     * @param request versão do termo de saúde exibida e confirmada pelo usuário
     * @return a situação de aceite atualizada após o registro
     */
    @PostMapping("/health-use/acceptance")
    @Operation(summary = "Registra o aceite do termo de uso em saúde")
    public ResponseEntity<TermAcceptanceStatusResponse> acceptHealthUse(
            @Valid @RequestBody AcceptTermRequest request
    ) {
        return ResponseEntity.ok(termAcceptanceService.acceptHealthUse(request));
    }
}
