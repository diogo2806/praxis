package br.com.iforce.praxis.shared.integration;

import br.com.iforce.praxis.shared.integration.dto.IntegrationTokenResponse;
import br.com.iforce.praxis.shared.integration.dto.RotateIntegrationTokenResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Porta de entrada (API) para administrar os tokens de integração.
 *
 * <p>Na visão do processo, é o painel onde a empresa gerencia as "chaves de
 * acesso" que as plataformas parceiras (Gupy, Recrutei) usam para conversar
 * com a Práxis: listar as chaves existentes, gerar uma nova chave (rotação) e
 * revogar uma chave. Rotacionar ou revogar são ações sensíveis, pois afetam o
 * acesso das integrações.</p>
 */
@RestController
@RequestMapping("/api/v1/integrations/tokens")
public class IntegrationTokenAdminController {

    private final IntegrationTokenAdminService integrationTokenAdminService;

    public IntegrationTokenAdminController(IntegrationTokenAdminService integrationTokenAdminService) {
        this.integrationTokenAdminService = integrationTokenAdminService;
    }

    /**
     * Lista as chaves de integração existentes da empresa.
     *
     * <p>Mostra quais integrações têm chave ativa, sem revelar o valor
     * secreto da chave em si.</p>
     *
     * @return as chaves de integração cadastradas
     */
    @GetMapping
    public ResponseEntity<List<IntegrationTokenResponse>> listTokens() {
        return ResponseEntity.ok(integrationTokenAdminService.listTokens());
    }

    /**
     * Gera uma nova chave de integração para um parceiro (rotação).
     *
     * <p>Substitui a chave anterior por uma nova e a devolve uma única vez.
     * A partir daí, a integração precisa passar a usar a nova chave.</p>
     *
     * @param provider o parceiro da integração (ex.: gupy, recrutei)
     * @return a nova chave gerada
     */
    @PostMapping("/{provider}/rotate")
    public ResponseEntity<RotateIntegrationTokenResponse> rotateToken(@PathVariable String provider) {
        return ResponseEntity.ok(integrationTokenAdminService.rotateToken(provider));
    }

    /**
     * Revoga (desativa) a chave de integração de um parceiro.
     *
     * <p>Após a revogação, aquela integração deixa de ter acesso até que uma
     * nova chave seja gerada.</p>
     *
     * @param provider o parceiro da integração (ex.: gupy, recrutei)
     * @return confirmação sem conteúdo
     */
    @DeleteMapping("/{provider}")
    public ResponseEntity<Void> revokeToken(@PathVariable String provider) {
        integrationTokenAdminService.revokeToken(provider);
        return ResponseEntity.noContent().build();
    }
}
