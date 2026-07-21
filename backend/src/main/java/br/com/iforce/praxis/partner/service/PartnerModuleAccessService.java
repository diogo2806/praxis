package br.com.iforce.praxis.partner.service;

import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import br.com.iforce.praxis.team.model.TeamProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Valida a habilitação comercial e a permissão administrativa do módulo de parceiros. */
@Service
public class PartnerModuleAccessService {

    private final UserRepository userRepository;
    private final boolean partnerModuleEnabled;

    public PartnerModuleAccessService(
            UserRepository userRepository,
            @Value("${praxis.partner.enabled:false}") boolean partnerModuleEnabled
    ) {
        this.userRepository = userRepository;
        this.partnerModuleEnabled = partnerModuleEnabled;
    }

    @Transactional(readOnly = true)
    public void requireAccess(String actorUserId, String empresaId) {
        if (!partnerModuleEnabled) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "O módulo de parceiros não está habilitado para esta empresa."
            );
        }

        try {
            Long userId = Long.parseLong(actorUserId);
            UserEntity actor = userRepository.findByIdAndEmpresaId(userId, empresaId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
            if (TeamProfile.fromRoles(actor.getRoles()) != TeamProfile.ADMINISTRADOR) {
                throw new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "O módulo de parceiros exige o perfil Administrador."
                );
            }
        } catch (NumberFormatException ignored) {
            // Usuário técnico usado apenas quando a segurança está desabilitada.
        }
    }
}
