package br.com.iforce.praxis.team.service;

import br.com.iforce.praxis.admin.model.UserStatus;
import br.com.iforce.praxis.audit.model.AuditEventType;
import br.com.iforce.praxis.audit.service.AuditEventService;
import br.com.iforce.praxis.audit.service.AuditMetadata;
import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import br.com.iforce.praxis.auth.persistence.repository.UserRepository;
import br.com.iforce.praxis.team.dto.InviteTeamUserRequest;
import br.com.iforce.praxis.team.dto.InviteTeamUserResponse;
import br.com.iforce.praxis.team.dto.TeamUserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Gerência dos usuários da equipe do próprio cliente (a empresa logada).
 *
 * <p>Na visão do processo, é aqui que o cliente administra quem, dentro da sua
 * empresa, pode usar o sistema: ele vê a lista de colegas cadastrados, convida
 * novas pessoas por e-mail, reenvia convites que ainda não foram aceitos e
 * bloqueia ou desbloqueia o acesso de alguém quando necessário.</p>
 *
 * <p>Toda operação fica automaticamente restrita à empresa do usuário logado —
 * um cliente nunca enxerga nem altera usuários de outra empresa. Além disso,
 * cada ação importante (convite, reenvio, bloqueio e desbloqueio) é registrada
 * em auditoria, deixando o histórico de quem fez o quê e quando.</p>
 */
@Service
public class TeamService {

    private static final String EMPRESA_ROLE = "EMPRESA";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;
    private final String publicBaseUrl;
    private final int inviteTtlHours;

    public TeamService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata,
            @Value("${praxis.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${praxis.admin.invite-ttl-hours:168}") int inviteTtlHours
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
        this.publicBaseUrl = publicBaseUrl;
        this.inviteTtlHours = inviteTtlHours;
    }

    /**
     * Lista todos os usuários da equipe da empresa logada.
     *
     * <p>Serve para preencher a tela de "Equipe": devolve os colegas
     * cadastrados na empresa, do mais antigo ao mais recente, com nome, e-mail,
     * perfis de acesso e situação (ativo, convidado ou bloqueado). É apenas
     * consulta, não altera nada.</p>
     *
     * @param tenantId identificador da empresa logada, cujos usuários serão listados
     * @return a lista de usuários da equipe, ordenada pela data de cadastro
     */
    @Transactional(readOnly = true)
    public List<TeamUserResponse> listUsers(String tenantId) {
        return userRepository.findByEmpresaIdOrderByCreatedAtAsc(tenantId).stream()
                .map(TeamService::toResponse)
                .toList();
    }

    /**
     * Convida uma nova pessoa para a equipe da empresa.
     *
     * <p>Fluxo do processo: o cliente informa o nome e o e-mail de quem quer
     * convidar. O sistema primeiro confere se já não existe alguém com aquele
     * e-mail na empresa, para evitar cadastro duplicado. Não havendo, cria o
     * usuário com o perfil de acesso padrão de empresa, marca-o como "convidado"
     * e gera um link de convite com validade limitada, por onde a pessoa
     * definirá a própria senha e entrará pela primeira vez. O convite é
     * registrado em auditoria e o link é devolvido para ser enviado ao
     * convidado.</p>
     *
     * @param actorUserId identificador de quem está fazendo o convite (para auditoria)
     * @param tenantId identificador da empresa que está convidando
     * @param request nome e e-mail da pessoa a convidar
     * @return os dados do usuário recém-criado junto com o link de convite
     * @throws ResponseStatusException se já existir um usuário com o mesmo e-mail na empresa
     */
    @Transactional
    public InviteTeamUserResponse inviteUser(String actorUserId, String tenantId, InviteTeamUserRequest request) {
        if (userRepository.existsByEmpresaIdAndEmail(tenantId, request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe um usuário com este e-mail nesta empresa.");
        }

        UserEntity user = new UserEntity();
        user.setEmpresaId(tenantId);
        user.setName(request.name().trim());
        user.setEmail(request.email().trim());
        user.setRoles(Set.of(EMPRESA_ROLE));
        user.setCreatedAt(Instant.now());
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));

        String token = generateInviteToken(user);
        user.setStatus(UserStatus.CONVIDADO);
        userRepository.save(user);

        auditEventService.auditAdminAction(
                actorUserId, tenantId, AuditEventType.TEAM_USER_INVITED,
                "Usuário convidado pela empresa: " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail(),
                        "role", EMPRESA_ROLE));

        return new InviteTeamUserResponse(toResponse(user), inviteUrl(token));
    }

    /**
     * Reenvia o convite de uma pessoa que ainda não entrou.
     *
     * <p>Fluxo do processo: quando o primeiro convite expira ou se perde, o
     * cliente pode gerar um novo link para a mesma pessoa. O sistema só permite
     * isso enquanto o usuário continua na situação "convidado" (ainda não
     * aceitou o convite); gera então um novo link com nova validade, registra o
     * reenvio em auditoria e devolve o link atualizado para ser enviado de
     * novo.</p>
     *
     * @param actorUserId identificador de quem está reenviando o convite (para auditoria)
     * @param tenantId identificador da empresa dona do usuário
     * @param userId identificador do usuário que receberá o novo convite
     * @return os dados do usuário junto com o novo link de convite
     * @throws ResponseStatusException se o usuário não for encontrado na empresa
     *         ou se ele já não estiver mais na situação "convidado"
     */
    @Transactional
    public InviteTeamUserResponse resendInvite(String actorUserId, String tenantId, Long userId) {
        UserEntity user = requireTenantUser(tenantId, userId);

        if (user.getStatus() != UserStatus.CONVIDADO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Só é possível reenviar convite para usuário que ainda está como CONVIDADO.");
        }

        String token = generateInviteToken(user);

        auditEventService.auditAdminAction(
                actorUserId, tenantId, AuditEventType.TEAM_USER_INVITE_RESENT,
                "Convite reenviado pela empresa para " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail()));

        return new InviteTeamUserResponse(toResponse(user), inviteUrl(token));
    }

    /**
     * Bloqueia o acesso de um usuário da equipe.
     *
     * <p>Fluxo do processo: usado quando alguém não deve mais entrar no sistema
     * (por exemplo, uma pessoa que saiu da empresa). O sistema impede que o
     * usuário bloqueie a si mesmo, marca a pessoa como "bloqueada" — o que passa
     * a barrar o login dela — e registra a ação em auditoria. O cadastro é
     * mantido; apenas o acesso fica suspenso, podendo ser reativado depois.</p>
     *
     * @param actorUserId identificador de quem está bloqueando (para auditoria e para impedir o autobloqueio)
     * @param tenantId identificador da empresa dona do usuário
     * @param userId identificador do usuário a ser bloqueado
     * @return os dados atualizados do usuário, já com a situação "bloqueado"
     * @throws ResponseStatusException se o usuário tentar bloquear a si mesmo
     *         ou se o usuário não for encontrado na empresa
     */
    @Transactional
    public TeamUserResponse blockUser(String actorUserId, String tenantId, Long userId) {
        try {
            Long actorId = Long.parseLong(actorUserId);
            if (actorId.equals(userId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível bloquear o próprio usuário.");
            }
        } catch (NumberFormatException ignored) {
            // actorUserId não numérico (ex: dev-user), ignora a verificação
        }

        UserEntity user = requireTenantUser(tenantId, userId);
        user.setStatus(UserStatus.BLOQUEADO);

        auditEventService.auditAdminAction(
                actorUserId, tenantId, AuditEventType.TEAM_USER_BLOCKED,
                "Usuário bloqueado pela empresa: " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail()));

        return toResponse(user);
    }

    /**
     * Reativa o acesso de um usuário que estava bloqueado.
     *
     * <p>Fluxo do processo: é o inverso do bloqueio. Volta o usuário para a
     * situação "ativo", liberando novamente o login, e registra a ação em
     * auditoria.</p>
     *
     * @param actorUserId identificador de quem está desbloqueando (para auditoria)
     * @param tenantId identificador da empresa dona do usuário
     * @param userId identificador do usuário a ser desbloqueado
     * @return os dados atualizados do usuário, já com a situação "ativo"
     * @throws ResponseStatusException se o usuário não for encontrado na empresa
     */
    @Transactional
    public TeamUserResponse unblockUser(String actorUserId, String tenantId, Long userId) {
        UserEntity user = requireTenantUser(tenantId, userId);
        user.setStatus(UserStatus.ATIVO);

        auditEventService.auditAdminAction(
                actorUserId, tenantId, AuditEventType.TEAM_USER_UNBLOCKED,
                "Usuário desbloqueado pela empresa: " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail()));

        return toResponse(user);
    }

    /**
     * Localiza, com segurança, um usuário garantindo que ele pertença à empresa
     * informada. É o que impede uma empresa de agir sobre usuários de outra.
     * Uso interno.
     */
    private UserEntity requireTenantUser(String tenantId, Long userId) {
        return userRepository.findByIdAndEmpresaId(userId, tenantId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    /**
     * Gera um novo convite para o usuário: cria um código aleatório, guarda
     * apenas a sua versão protegida (para conferência posterior) e define quando
     * o convite deixa de valer. Uso interno.
     */
    private String generateInviteToken(UserEntity user) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = URL_ENCODER.encodeToString(bytes);
        user.setInviteTokenHash(passwordEncoder.encode(token));
        user.setInvitedAt(Instant.now());
        user.setInviteExpiresAt(Instant.now().plus(inviteTtlHours, ChronoUnit.HOURS));
        return token;
    }

    /**
     * Monta o link de convite completo, juntando o endereço público do sistema
     * ao código gerado. É esse link que a pessoa convidada acessa para entrar
     * pela primeira vez. Uso interno.
     */
    private String inviteUrl(String token) {
        return publicBaseUrl + "/convite/" + token;
    }

    /**
     * Converte o cadastro interno do usuário no formato simplificado que é
     * devolvido para a tela. Uso interno.
     */
    private static TeamUserResponse toResponse(UserEntity user) {
        return new TeamUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}
