package br.com.iforce.praxis.candidate.controller;

import br.com.iforce.praxis.candidate.dto.DataSubjectRequest;
import br.com.iforce.praxis.candidate.dto.HealthConsentRequest;
import br.com.iforce.praxis.candidate.dto.ParticipacaoResponse;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaRequest;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaResponse;
import br.com.iforce.praxis.candidate.dto.ReviewRequest;
import br.com.iforce.praxis.candidate.service.CandidateDataRequestService;
import br.com.iforce.praxis.candidate.service.CandidateHealthConsentService;
import br.com.iforce.praxis.candidate.service.CandidateReviewRequestService;
import br.com.iforce.praxis.gupy.service.CandidateAttemptService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Porta de entrada (API) do fluxo público do candidato durante a avaliação.
 *
 * <p>É por aqui que a tela usada pelo próprio candidato conversa com o
 * sistema enquanto ele faz a prova: carregar a etapa atual, enviar respostas,
 * pedir revisão humana do resultado e registrar consentimento de dados de
 * saúde. O contrato público entrega somente os dados necessários para exibir
 * e executar a etapa atual; transições, pontuações e demais regras técnicas
 * permanecem resolvidas no servidor.</p>
 */
@RestController
@RequestMapping("/candidate/attempts")
@Tag(name = "Participacoes", description = "Fluxo publico do candidato para executar a avaliacao.")
public class CandidateAttemptController {

    private final CandidateAttemptService candidateAttemptService;
    private final CandidateReviewRequestService candidateReviewRequestService;
    private final CandidateDataRequestService candidateDataRequestService;
    private final CandidateHealthConsentService candidateHealthConsentService;

    public CandidateAttemptController(
            CandidateAttemptService candidateAttemptService,
            CandidateReviewRequestService candidateReviewRequestService,
            CandidateDataRequestService candidateDataRequestService,
            CandidateHealthConsentService candidateHealthConsentService
    ) {
        this.candidateAttemptService = candidateAttemptService;
        this.candidateReviewRequestService = candidateReviewRequestService;
        this.candidateDataRequestService = candidateDataRequestService;
        this.candidateHealthConsentService = candidateHealthConsentService;
    }

    /**
     * Carrega a tela atual da prova para o candidato.
     *
     * <p>Devolve a etapa em que o candidato está com o conteúdo necessário
     * para continuar. A topologia da avaliação e as regras de transição não
     * fazem parte da resposta pública.</p>
     *
     * @param attemptId identificador da participação do candidato
     * @return a etapa atual da avaliação
     */
    @GetMapping("/{attemptId}")
    @Operation(
            summary = "Carrega participacao do candidato",
            description = "Retorna somente a etapa atual e seu conteúdo visual; transições e regras técnicas são resolvidas no servidor."
    )
    public ResponseEntity<ParticipacaoResponse> getCandidateAttempt(@PathVariable String attemptId) {
        return ResponseEntity.ok(candidateAttemptService.findCandidateAttempt(attemptId));
    }

    /**
     * Registra a resposta que o candidato deu na etapa atual.
     *
     * <p>Guarda a resposta e avança o fluxo. Quando é a última etapa, o
     * sistema fecha a avaliação e calcula o resultado.</p>
     *
     * @param attemptId identificador da participação do candidato
     * @param request a resposta informada pelo candidato
     * @return a próxima etapa ou o desfecho da avaliação
     */
    @PostMapping("/{attemptId}/answers")
    @Operation(
            summary = "Registra resposta do candidato",
            description = "Registra a resposta da etapa atual e recalcula o resultado quando a avaliacao termina."
    )
    public ResponseEntity<RegistrarRespostaResponse> submitAnswer(
            @PathVariable String attemptId,
            @Valid @RequestBody RegistrarRespostaRequest request
    ) {
        return ResponseEntity.ok(candidateAttemptService.submitAnswer(attemptId, request));
    }

    /**
     * Registra o pedido do candidato para que um humano revise o resultado.
     *
     * <p>Direito garantido pela LGPD (art. 20): o candidato pode pedir que
     * uma pessoa revise a decisão. O pedido fica registrado na trilha de
     * auditoria para que o recrutador o atenda.</p>
     *
     * @param attemptId identificador da participação do candidato
     * @param request justificativa opcional do pedido de revisão
     * @return confirmação sem conteúdo (apenas registra o pedido)
     */
    @PostMapping("/{attemptId}/review-request")
    @Operation(
            summary = "Solicita revisão humana",
            description = "Registra o pedido de revisão humana do candidato (LGPD art. 20). Uma pessoa "
                    + "decide; este pedido fica na trilha append-only para o recrutador."
    )
    public ResponseEntity<Void> requestHumanReview(
            @PathVariable String attemptId,
            @Valid @RequestBody(required = false) ReviewRequest request
    ) {
        candidateReviewRequestService.register(attemptId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Registra a requisição de um direito do titular feita pelo candidato.
     *
     * <p>Direitos garantidos pela LGPD (art. 18): confirmação/acesso, correção,
     * anonimização/eliminação, portabilidade, informação sobre compartilhamento
     * e revogação do consentimento. O pedido fica registrado na trilha de
     * auditoria para que o controlador (empresa responsável) o atenda no prazo
     * legal.</p>
     *
     * @param attemptId identificador da participação do candidato
     * @param request direito solicitado e dados opcionais de contato/detalhe
     * @return confirmação sem conteúdo (apenas registra o pedido)
     */
    @PostMapping("/{attemptId}/data-request")
    @Operation(
            summary = "Solicita direito do titular",
            description = "Registra a requisição de um direito do titular pelo candidato (LGPD art. 18) na "
                    + "trilha append-only. O controlador (empresa) atende no prazo legal; o Práxis atua como operador."
    )
    public ResponseEntity<Void> requestDataSubjectRight(
            @PathVariable String attemptId,
            @Valid @RequestBody DataSubjectRequest request
    ) {
        candidateDataRequestService.register(attemptId, request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Registra o consentimento do participante para usar dados de saúde.
     *
     * <p>Na vertical de saúde, antes de iniciar a atividade o participante
     * precisa autorizar o tratamento de dados sensíveis (LGPD, arts. 11 e
     * 14). Esse consentimento fica guardado na trilha de auditoria.</p>
     *
     * @param attemptId identificador da participação do candidato
     * @param request dados do consentimento informado
     * @return confirmação sem conteúdo (apenas registra o consentimento)
     */
    @PostMapping("/{attemptId}/health-consent")
    @Operation(
            summary = "Registra consentimento de saúde do participante",
            description = "Na vertical de saúde, registra o consentimento do participante para tratamento "
                    + "de dado sensível (LGPD, arts. 11 e 14) na trilha append-only, antes de iniciar a atividade."
    )
    public ResponseEntity<Void> registerHealthConsent(
            @PathVariable String attemptId,
            @Valid @RequestBody HealthConsentRequest request
    ) {
        candidateHealthConsentService.register(attemptId, request);
        return ResponseEntity.noContent().build();
    }
}
