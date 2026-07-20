package br.com.iforce.praxis.integrity.controller;

import br.com.iforce.praxis.candidate.service.PublicCandidateFlowSecurity;
import br.com.iforce.praxis.integrity.dto.CandidateIntegritySessionResponse;
import br.com.iforce.praxis.integrity.dto.CloseIntegritySessionRequest;
import br.com.iforce.praxis.integrity.dto.IntegrityEventRequest;
import br.com.iforce.praxis.integrity.dto.IntegrityHeartbeatRequest;
import br.com.iforce.praxis.integrity.dto.StartIntegritySessionRequest;
import br.com.iforce.praxis.integrity.service.CandidateIntegrityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/candidate/attempts/{attemptToken}/integrity")
@Tag(name = "Integridade da aplicação", description = "Sessão e telemetria técnica separadas da pontuação.")
public class CandidateIntegrityController {

    private final CandidateIntegrityService candidateIntegrityService;
    private final PublicCandidateFlowSecurity publicCandidateFlowSecurity;

    public CandidateIntegrityController(
            CandidateIntegrityService candidateIntegrityService,
            PublicCandidateFlowSecurity publicCandidateFlowSecurity
    ) {
        this.candidateIntegrityService = candidateIntegrityService;
        this.publicCandidateFlowSecurity = publicCandidateFlowSecurity;
    }

    @PostMapping("/session")
    @Operation(
            summary = "Abre ou retoma a sessão técnica",
            description = "Garante uma sessão ativa por tentativa, sem alterar respostas, score ou resultado."
    )
    public ResponseEntity<CandidateIntegritySessionResponse> startSession(
            @PathVariable String attemptToken,
            @Valid @RequestBody StartIntegritySessionRequest request,
            HttpServletRequest httpRequest
    ) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        CandidateIntegritySessionResponse response = candidateIntegrityService.startSession(
                attemptToken,
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.status(response.resumed() ? HttpStatus.OK : HttpStatus.CREATED).body(response);
    }

    @PostMapping("/heartbeat")
    @Operation(summary = "Mantém a sessão técnica ativa")
    public ResponseEntity<Void> heartbeat(
            @PathVariable String attemptToken,
            @Valid @RequestBody IntegrityHeartbeatRequest request
    ) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        candidateIntegrityService.heartbeat(attemptToken, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/events")
    @Operation(
            summary = "Registra evento técnico da aplicação",
            description = "Aceita apenas categorias técnicas permitidas e não recebe texto da resposta ou pontuação."
    )
    public ResponseEntity<Void> recordEvent(
            @PathVariable String attemptToken,
            @Valid @RequestBody IntegrityEventRequest request
    ) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        candidateIntegrityService.recordEvent(attemptToken, request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/close")
    @Operation(summary = "Encerra a sessão técnica")
    public ResponseEntity<Void> closeSession(
            @PathVariable String attemptToken,
            @Valid @RequestBody CloseIntegritySessionRequest request
    ) {
        publicCandidateFlowSecurity.requireValidAttemptToken(attemptToken);
        candidateIntegrityService.closeSession(attemptToken, request);
        return ResponseEntity.noContent().build();
    }
}
