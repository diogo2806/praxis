package br.com.iforce.praxis.candidate.service;

import br.com.iforce.praxis.auth.service.JwtService;
import br.com.iforce.praxis.candidate.dto.EtapaAtualResponse;
import br.com.iforce.praxis.candidate.dto.ParticipacaoResponse;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaRequest;
import br.com.iforce.praxis.candidate.dto.RegistrarRespostaResponse;
import br.com.iforce.praxis.candidate.dto.RespostaResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Component
public class PublicCandidateFlowSecurity {

    private final JwtService jwtService;
    private final boolean securityEnabled;

    public PublicCandidateFlowSecurity(
            JwtService jwtService,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled
    ) {
        this.jwtService = jwtService;
        this.securityEnabled = securityEnabled;
    }

    public void requireValidAttemptToken(String token) {
        if (token == null || token.isBlank()) {
            throw unauthorized();
        }
        try {
            jwtService.parseCandidateAttemptToken(token);
        } catch (RuntimeException exception) {
            // Compatibilidade exclusiva do modo local/teste sem segurança. Em
            // produção, IDs internos nunca são aceitos como credencial pública.
            if (!securityEnabled && isLegacyAttemptId(token)) {
                return;
            }
            throw unauthorized();
        }
    }

    public ParticipacaoResponse sanitize(String token, ParticipacaoResponse response) {
        if (allowLegacyResponse(token)) {
            return response;
        }
        return new ParticipacaoResponse(
                opaque(response.participacaoId()),
                response.avaliacaoNome(),
                response.status(),
                response.finalizado(),
                response.redirectUrl(),
                response.acaoSugeridaFrontend(),
                response.progresso(),
                sanitizeStage(token, response.etapaAtual()),
                response.verticalSaude()
        );
    }

    public RegistrarRespostaResponse sanitize(String token, RegistrarRespostaResponse response) {
        if (allowLegacyResponse(token)) {
            return response;
        }
        return new RegistrarRespostaResponse(
                opaque(response.participacaoId()),
                response.status(),
                response.repetida(),
                response.finalizado(),
                response.redirectUrl(),
                response.progresso(),
                sanitizeStage(token, response.etapaAtual())
        );
    }

    public RegistrarRespostaRequest sanitizeRequest(RegistrarRespostaRequest request) {
        if (!securityEnabled) {
            return request;
        }
        return new RegistrarRespostaRequest(
                null,
                request.respostaId(),
                request.etapaNumero(),
                request.respondidaEm(),
                request.tempoEsgotado()
        );
    }

    private boolean allowLegacyResponse(String token) {
        return !securityEnabled && isLegacyAttemptId(token);
    }

    private boolean isLegacyAttemptId(String value) {
        return value != null && value.matches("att_[A-Za-z0-9]{16,64}");
    }

    private EtapaAtualResponse sanitizeStage(String token, EtapaAtualResponse stage) {
        if (stage == null) {
            return null;
        }
        List<RespostaResponse> options = stage.alternativas().stream()
                .map(option -> new RespostaResponse(
                        option.id(),
                        option.texto(),
                        option.descricaoAcessivel(),
                        option.audioDescricaoUrl(),
                        option.midiaUrl(),
                        option.tipoMidia()
                ))
                .toList();
        return new EtapaAtualResponse(
                opaque(token + ":" + stage.id()),
                stage.numero(),
                stage.pessoa(),
                stage.descricao(),
                stage.descricaoAcessivel(),
                stage.tempoLimiteSegundos(),
                stage.tempoLimiteSegundosAcomodado(),
                stage.audioDescricaoUrl(),
                stage.midiaUrl(),
                stage.tipoMidia(),
                options
        );
    }

    private String opaque(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return "pub_" + HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível.", exception);
        }
    }

    private ResponseStatusException unauthorized() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Token da tentativa do candidato inválido ou expirado."
        );
    }
}
