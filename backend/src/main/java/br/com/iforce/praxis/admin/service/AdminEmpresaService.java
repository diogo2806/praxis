package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.AdminUserResponse;

import br.com.iforce.praxis.admin.dto.CreateEmpresaAdminRequest;

import br.com.iforce.praxis.admin.dto.CreateEmpresaAdminResponse;

import br.com.iforce.praxis.admin.dto.InviteUserAdminRequest;

import br.com.iforce.praxis.admin.dto.InviteUserAdminResponse;

import br.com.iforce.praxis.admin.dto.ReactivateEmpresaAdminRequest;

import br.com.iforce.praxis.admin.dto.EmpresaAdminDetailResponse;

import br.com.iforce.praxis.admin.dto.EmpresaAdminSummaryResponse;

import br.com.iforce.praxis.admin.dto.UpdateEmpresaAdminRequest;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.admin.model.UserStatus;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.audit.service.AuditMetadata;

import br.com.iforce.praxis.billing.service.CreditService;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.entity.UserEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import br.com.iforce.praxis.auth.persistence.repository.UserRepository;

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
 * Orquestra o ciclo de vida dos clientes (empresas) a partir do painel ADMIN.
 *
 * <p>O ADMIN é um operador da plataforma e não pertence ao fluxo de empresa cliente: por isso
 * todas as operações recebem o empresa alvo explicitamente, sem depender do
 * {@code CurrentEmpresaService}. Cada ação sensível exige motivo e gera evento de auditoria
 * append-only, registrando o operador (ator) e o empresa alvo.</p>
 */
@Service
public class AdminEmpresaService {

    /** Papel do usuário responsável do cliente. Nunca ADMIN. */
    public static final String EMPRESA_ROLE = "EMPRESA";

    private static final String PLATFORM_EMPRESA_ID = "PLATFORM";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final EmpresaRepository empresaRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;
    private final AdminUsageService adminUsageService;
    private final CreditService creditService;
    private final String publicBaseUrl;
    private final int inviteTtlHours;
    private final int usagePeriodDays;

    public AdminEmpresaService(
            EmpresaRepository empresaRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata,
            AdminUsageService adminUsageService,
            CreditService creditService,
            @Value("${praxis.public-base-url:http://localhost:8080}") String publicBaseUrl,
            @Value("${praxis.admin.invite-ttl-hours:168}") int inviteTtlHours,
            @Value("${praxis.admin.usage-period-days:30}") int usagePeriodDays
    ) {
        this.empresaRepository = empresaRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
        this.adminUsageService = adminUsageService;
        this.creditService = creditService;
        this.publicBaseUrl = publicBaseUrl;
        this.inviteTtlHours = inviteTtlHours;
        this.usagePeriodDays = usagePeriodDays;
    }

    // ------------------------------------------------------------------
    // Listagem e detalhe
    // ------------------------------------------------------------------

    /**
     * Monta a lista de clientes que o operador vê na tela inicial de gestão.
     *
     * <p>Na visão do processo: é a "agenda de clientes" da plataforma. O operador pode
     * digitar um trecho do nome para procurar e filtrar por situação (em teste, ativo,
     * suspenso, cancelado) e por tipo de plano comercial. Para cada cliente, a linha já
     * traz o quanto ele usou (avaliações concluídas) dentro do período escolhido e o saldo
     * de créditos, para o operador ter a fotografia do cliente sem precisar abrir o detalhe.</p>
     *
     * @param search trecho do nome para procurar; em branco lista todos
     * @param status situação do cliente para filtrar; nulo não filtra por situação
     * @param plan tipo de plano comercial para filtrar; nulo não filtra por plano
     * @param periodStart início do período de uso a considerar; nulo usa o período padrão
     * @param periodEnd fim do período de uso a considerar; nulo considera até agora
     * @return o resumo de cada cliente encontrado
     */
    @Transactional(readOnly = true)
    public List<EmpresaAdminSummaryResponse> list(
            String search,
            EmpresaStatus status,
            CommercialPlanType plan,
            Instant periodStart,
            Instant periodEnd
    ) {
        Period period = resolvePeriod(periodStart, periodEnd);
        String normalizedSearch = (search == null || search.isBlank())
                ? null
                : "%" + search.toLowerCase().trim() + "%";

        return empresaRepository.search(normalizedSearch, status, plan).stream()
                .map(empresa -> toSummary(empresa, period))
                .toList();
    }

    /**
     * Abre a ficha completa de um cliente.
     *
     * <p>Na visão do processo: é o "prontuário" do cliente — dados cadastrais, plano e
     * condição comercial, situação atual, uso no período padrão, saldo de créditos e a
     * lista de usuários de acesso da empresa. É o que o operador vê ao clicar em um cliente
     * na lista.</p>
     *
     * @param empresaId identificador do cliente
     * @return a ficha completa do cliente
     */
    @Transactional(readOnly = true)
    public EmpresaAdminDetailResponse detail(String empresaId) {
        EmpresaEntity empresa = requireClient(empresaId);
        return toDetail(empresa, resolvePeriod(null, null));
    }

    // ------------------------------------------------------------------
    // Cadastro
    // ------------------------------------------------------------------

    /**
     * Cadastra um cliente novo e já cria o seu primeiro usuário responsável.
     *
     * <p>Na visão do processo: é a "abertura de conta" de um cliente na plataforma. Além
     * de gravar os dados da empresa (nome, documento, contatos, plano e condição
     * comercial), o processo já provisiona a pessoa responsável — normalmente enviando um
     * convite por link para ela criar a própria senha. Se o operador não informar uma
     * situação inicial, o cliente entra "em teste". A criação e o convite ficam registrados
     * na trilha de auditoria, em nome do operador que os executou.</p>
     *
     * @param actorUserId identificador do operador ADMIN que está cadastrando
     * @param request dados da empresa e do responsável a convidar
     * @return a ficha do cliente recém-criado, o id do usuário responsável e o link de convite (quando houver)
     */
    @Transactional
    public CreateEmpresaAdminResponse create(String actorUserId, CreateEmpresaAdminRequest request) {
        Instant now = Instant.now();

        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(generateEmpresaId());
        empresa.setName(request.name().trim());
        empresa.setTradeName(blankToNull(request.tradeName()));
        empresa.setLegalName(blankToNull(request.legalName()));
        empresa.setTaxId(blankToNull(request.taxId()));
        empresa.setCorporateEmail(blankToNull(request.corporateEmail()));
        empresa.setPhone(blankToNull(request.phone()));
        empresa.setWebsite(blankToNull(request.website()));
        empresa.setHealthVertical(request.healthVertical());
        empresa.setCompanyId(resolveCompanyId(request.companyId()));
        empresa.setCommercialPlanType(request.commercialPlanType());
        empresa.setCommercialCondition(blankToNull(request.commercialCondition()));
        empresa.setStatus(request.initialStatus() == null ? EmpresaStatus.EM_TESTE : request.initialStatus());
        empresa.setCreatedAt(now);
        empresa.setUpdatedAt(now);
        empresaRepository.save(empresa);

        auditEventService.auditAdminAction(
                actorUserId,
                empresa.getId(),
                AuditEventType.ADMIN_EMPRESA_CREATED,
                "Cliente criado: " + empresa.getName(),
                auditMetadata.of(
                        "name", empresa.getName(),
                        "commercialPlanType", empresa.getCommercialPlanType().name(),
                        "status", empresa.getStatus().name(),
                        "healthVertical", empresa.isHealthVertical(),
                        "companyId", empresa.getCompanyId()
                )
        );

        InvitedUser invited = provisionResponsibleUser(
                actorUserId, empresa.getId(), request.responsibleName(), request.responsibleEmail(),
                request.sendInvite(), now);

        return new CreateEmpresaAdminResponse(
                toDetail(empresa, resolvePeriod(null, null)),
                invited.user().getId(),
                invited.inviteUrl()
        );
    }

    // ------------------------------------------------------------------
    // Edição de dados / plano / condição comercial
    // ------------------------------------------------------------------

    /**
     * Atualiza os dados de um cliente, inclusive plano e condição comercial.
     *
     * <p>Na visão do processo: é a "edição do cadastro". Só muda o que foi informado — os
     * campos deixados de fora permanecem como estavam. Duas mudanças têm peso especial e
     * ganham registro próprio na trilha de auditoria: a troca do plano comercial (guardando
     * de qual para qual plano) e a alteração da condição comercial. No fim, a própria
     * atualização de cadastro também é registrada, sempre em nome do operador.</p>
     *
     * @param actorUserId identificador do operador ADMIN que está editando
     * @param empresaId identificador do cliente
     * @param request campos a atualizar (apenas os preenchidos são aplicados)
     * @return a ficha atualizada do cliente
     */
    @Transactional
    public EmpresaAdminDetailResponse update(
            String actorUserId,
            String empresaId,
            UpdateEmpresaAdminRequest request
    ) {
        EmpresaEntity empresa = requireClient(empresaId);

        if (request.name() != null && !request.name().isBlank()) {
            empresa.setName(request.name().trim());
        }
        if (request.tradeName() != null) {
            empresa.setTradeName(blankToNull(request.tradeName()));
        }
        if (request.legalName() != null) {
            empresa.setLegalName(blankToNull(request.legalName()));
        }
        if (request.taxId() != null) {
            empresa.setTaxId(blankToNull(request.taxId()));
        }
        if (request.corporateEmail() != null) {
            empresa.setCorporateEmail(blankToNull(request.corporateEmail()));
        }
        if (request.phone() != null) {
            empresa.setPhone(blankToNull(request.phone()));
        }
        if (request.website() != null) {
            empresa.setWebsite(blankToNull(request.website()));
        }
        if (request.healthVertical() != null) {
            empresa.setHealthVertical(request.healthVertical());
        }

        CommercialPlanType previousPlan = empresa.getCommercialPlanType();
        if (request.commercialPlanType() != null && request.commercialPlanType() != previousPlan) {
            empresa.setCommercialPlanType(request.commercialPlanType());
            auditEventService.auditAdminAction(
                    actorUserId, empresa.getId(), AuditEventType.ADMIN_COMMERCIAL_PLAN_CHANGED,
                    "Plano comercial alterado de " + previousPlan + " para " + request.commercialPlanType(),
                    auditMetadata.of("from", previousPlan.name(), "to", request.commercialPlanType().name()));
        }

        if (request.commercialCondition() != null
                && !request.commercialCondition().equals(empresa.getCommercialCondition())) {
            empresa.setCommercialCondition(blankToNull(request.commercialCondition()));
            auditEventService.auditAdminAction(
                    actorUserId, empresa.getId(), AuditEventType.ADMIN_COMMERCIAL_CONDITION_CHANGED,
                    "Condição comercial atualizada.",
                    auditMetadata.of("commercialCondition", empresa.getCommercialCondition()));
        }

        empresa.setUpdatedAt(Instant.now());

        auditEventService.auditAdminAction(
                actorUserId, empresa.getId(), AuditEventType.ADMIN_EMPRESA_UPDATED,
                "Dados do cliente atualizados.",
                auditMetadata.of("empresaId", empresa.getId()));

        return toDetail(empresa, resolvePeriod(null, null));
    }

    // ------------------------------------------------------------------
    // Suspensão / reativação / cancelamento
    // ------------------------------------------------------------------

    /**
     * Suspende um cliente, cortando temporariamente o acesso dele à plataforma.
     *
     * <p>Na visão do processo: é o "pausar" de um cliente — usado, por exemplo, em atraso
     * de pagamento ou uso indevido. Enquanto suspenso, o cliente não consegue mais entrar
     * nem usar os recursos protegidos, mas nada é apagado: é uma pausa, não um encerramento.
     * O motivo é obrigatório e fica guardado na trilha de auditoria junto com a situação
     * anterior, para que a decisão tenha dono e justificativa.</p>
     *
     * @param actorUserId identificador do operador ADMIN que está suspendendo
     * @param empresaId identificador do cliente
     * @param reason motivo da suspensão (registrado na auditoria)
     * @return a ficha atualizada do cliente
     */
    @Transactional
    public EmpresaAdminDetailResponse suspend(String actorUserId, String empresaId, String reason) {
        EmpresaEntity empresa = requireClient(empresaId);
        EmpresaStatus previous = empresa.getStatus();
        empresa.setStatus(EmpresaStatus.SUSPENSO);
        empresa.setUpdatedAt(Instant.now());

        auditEventService.auditAdminAction(
                actorUserId, empresa.getId(), AuditEventType.ADMIN_EMPRESA_SUSPENDED,
                "Cliente suspenso. Motivo: " + reason,
                auditMetadata.of("reason", reason, "previousStatus", previous.name()));

        return toDetail(empresa, resolvePeriod(null, null));
    }

    /**
     * Reativa um cliente que estava suspenso ou cancelado, devolvendo o acesso.
     *
     * <p>Na visão do processo: é o "religar" de um cliente. O operador escolhe para qual
     * situação o cliente volta — pode voltar como ativo (operação normal) ou como em teste
     * (período de avaliação); nenhuma outra situação é aceita nesta ação. Como toda decisão
     * sensível, exige motivo e registra na trilha de auditoria a situação anterior e a nova.</p>
     *
     * @param actorUserId identificador do operador ADMIN que está reativando
     * @param empresaId identificador do cliente
     * @param request situação alvo (ativo ou em teste) e motivo da reativação
     * @return a ficha atualizada do cliente
     */
    @Transactional
    public EmpresaAdminDetailResponse reactivate(
            String actorUserId,
            String empresaId,
            ReactivateEmpresaAdminRequest request
    ) {
        EmpresaStatus target = request.targetStatus() == null ? EmpresaStatus.ATIVO : request.targetStatus();
        if (target != EmpresaStatus.ATIVO && target != EmpresaStatus.EM_TESTE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Reativação só aceita os status ATIVO ou EM_TESTE.");
        }

        EmpresaEntity empresa = requireClient(empresaId);
        EmpresaStatus previous = empresa.getStatus();
        empresa.setStatus(target);
        empresa.setUpdatedAt(Instant.now());

        auditEventService.auditAdminAction(
                actorUserId, empresa.getId(), AuditEventType.ADMIN_EMPRESA_REACTIVATED,
                "Cliente reativado para " + target + ". Motivo: " + request.reason(),
                auditMetadata.of("reason", request.reason(), "previousStatus", previous.name(),
                        "targetStatus", target.name()));

        return toDetail(empresa, resolvePeriod(null, null));
    }

    /**
     * Cancela um cliente, encerrando a relação comercial, mas preservando o histórico.
     *
     * <p>Na visão do processo: é o "encerramento de contrato". Diferente da suspensão
     * (que é uma pausa), o cancelamento marca o fim da relação com o cliente. Ainda assim,
     * nada é apagado: todo o histórico de avaliações, usuários e auditoria continua
     * disponível para consulta. O motivo é obrigatório e a situação anterior é registrada
     * na trilha de auditoria.</p>
     *
     * @param actorUserId identificador do operador ADMIN que está cancelando
     * @param empresaId identificador do cliente
     * @param reason motivo do cancelamento (registrado na auditoria)
     * @return a ficha atualizada do cliente
     */
    @Transactional
    public EmpresaAdminDetailResponse cancel(String actorUserId, String empresaId, String reason) {
        EmpresaEntity empresa = requireClient(empresaId);
        EmpresaStatus previous = empresa.getStatus();
        empresa.setStatus(EmpresaStatus.CANCELADO);
        empresa.setUpdatedAt(Instant.now());

        auditEventService.auditAdminAction(
                actorUserId, empresa.getId(), AuditEventType.ADMIN_EMPRESA_CANCELED,
                "Cliente cancelado. Motivo: " + reason,
                auditMetadata.of("reason", reason, "previousStatus", previous.name()));

        return toDetail(empresa, resolvePeriod(null, null));
    }

    /**
     * Concede créditos de cortesia a um cliente para liberar testes. Soma ao saldo (ledger
     * append-only com motivo ADJUSTMENT), reativa o cliente se estava sem crédito e registra a
     * ação na trilha de auditoria. Não altera o plano comercial.
     */
    @Transactional
    public EmpresaAdminDetailResponse grantCredits(String actorUserId, String empresaId, int amount, String note) {
        EmpresaEntity empresa = requireClient(empresaId);
        String trimmedNote = blankToNull(note);
        String ledgerNote = "Crédito de cortesia concedido pelo ADMIN"
                + (trimmedNote == null ? "." : ": " + trimmedNote);
        int newBalance = creditService.grantAdjustmentCredits(empresa.getId(), amount, ledgerNote);

        auditEventService.auditAdminAction(
                actorUserId, empresa.getId(), AuditEventType.ADMIN_EMPRESA_CREDITS_GRANTED,
                "Concedidos " + amount + " credito(s) de cortesia. Saldo atual: " + newBalance + ".",
                auditMetadata.of("amount", String.valueOf(amount), "newBalance", String.valueOf(newBalance),
                        "note", trimmedNote == null ? "" : trimmedNote));

        return toDetail(empresa, resolvePeriod(null, null));
    }

    // ------------------------------------------------------------------
    // Usuários do cliente
    // ------------------------------------------------------------------

    /**
     * Lista as pessoas que têm acesso ao sistema em nome de um cliente.
     *
     * <p>Na visão do processo: mostra quem, dentro daquela empresa, pode entrar na
     * plataforma — com a situação de cada um (convidado, ativo, bloqueado) e quando fez o
     * último acesso. É a base da aba "Acessos" do cliente.</p>
     *
     * @param empresaId identificador do cliente
     * @return os usuários de acesso do cliente, do mais antigo para o mais novo
     */
    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers(String empresaId) {
        requireClient(empresaId);
        return userRepository.findByEmpresaIdOrderByCreatedAtAsc(empresaId).stream()
                .map(AdminEmpresaService::toUserResponse)
                .toList();
    }

    /**
     * Convida uma nova pessoa para acessar a plataforma em nome de um cliente.
     *
     * <p>Na visão do processo: adiciona mais um "acesso" à empresa. A pessoa recebe um
     * convite por link para criar a própria senha e entra sempre com o papel de usuário da
     * empresa (nunca como operador da plataforma). Se já existir alguém com o mesmo e-mail
     * naquele cliente, o convite é recusado, evitando acessos duplicados. A ação fica
     * registrada na auditoria.</p>
     *
     * @param actorUserId identificador do operador ADMIN que está convidando
     * @param empresaId identificador do cliente
     * @param request nome e e-mail da pessoa a convidar
     * @return os dados do usuário criado e o link de convite
     */
    @Transactional
    public InviteUserAdminResponse inviteUser(
            String actorUserId,
            String empresaId,
            InviteUserAdminRequest request
    ) {
        requireClient(empresaId);
        if (userRepository.existsByEmpresaIdAndEmail(empresaId, request.email())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Já existe um usuário com este e-mail neste cliente.");
        }

        InvitedUser invited = provisionResponsibleUser(
                actorUserId, empresaId, request.name(), request.email(), true, Instant.now());
        return new InviteUserAdminResponse(toUserResponse(invited.user()), invited.inviteUrl());
    }

    /**
     * Reenvia o convite de acesso para uma pessoa que ainda não entrou.
     *
     * <p>Na visão do processo: reemite o link de convite para quem foi convidado mas ainda
     * não criou a senha (por exemplo, se o e-mail se perdeu ou o prazo expirou). Por
     * segurança, só vale para quem ainda está como "convidado": não é possível reenviar
     * convite para um usuário já ativo, o que evitaria reabrir o acesso dele por um link.
     * O reenvio fica registrado na auditoria.</p>
     *
     * @param actorUserId identificador do operador ADMIN que está reenviando
     * @param empresaId identificador do cliente
     * @param userId identificador do usuário convidado
     * @return os dados do usuário e o novo link de convite
     */
    @Transactional
    public InviteUserAdminResponse resendInvite(String actorUserId, String empresaId, Long userId) {
        requireClient(empresaId);
        UserEntity user = userRepository.findByIdAndEmpresaId(userId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));

        // Reenviar convite não pode rebaixar um usuário já ativo de volta para CONVIDADO:
        // isso reabriria o login por senha conhecida durante a janela do convite.
        if (user.getStatus() != UserStatus.CONVIDADO) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Só é possível reenviar convite para usuário que ainda está como CONVIDADO."
            );
        }

        String token = generateInviteToken(user);
        user.setStatus(UserStatus.CONVIDADO);

        auditEventService.auditAdminAction(
                actorUserId, empresaId, AuditEventType.ADMIN_USER_INVITE_RESENT,
                "Convite reenviado para " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail()));

        return new InviteUserAdminResponse(toUserResponse(user), inviteUrl(token));
    }

    /**
     * Bloqueia o acesso de uma pessoa de um cliente, sem apagar o cadastro dela.
     *
     * <p>Na visão do processo: "tira a chave" de um usuário — ele deixa de conseguir
     * entrar, mas continua no cadastro e todo o seu histórico é preservado. É reversível
     * pelo desbloqueio. O bloqueio fica registrado na auditoria.</p>
     *
     * @param actorUserId identificador do operador ADMIN que está bloqueando
     * @param empresaId identificador do cliente
     * @param userId identificador do usuário a bloquear
     * @return os dados atualizados do usuário
     */
    @Transactional
    public AdminUserResponse blockUser(String actorUserId, String empresaId, Long userId) {
        UserEntity user = requireEmpresaUser(empresaId, userId);
        user.setStatus(UserStatus.BLOQUEADO);

        auditEventService.auditAdminAction(
                actorUserId, empresaId, AuditEventType.ADMIN_USER_BLOCKED,
                "Usuário bloqueado: " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail()));

        return toUserResponse(user);
    }

    /**
     * Desbloqueia uma pessoa que estava impedida de acessar, devolvendo o acesso.
     *
     * <p>Na visão do processo: "devolve a chave" de um usuário bloqueado, deixando-o ativo
     * novamente. É o oposto do bloqueio. A ação fica registrada na auditoria.</p>
     *
     * @param actorUserId identificador do operador ADMIN que está desbloqueando
     * @param empresaId identificador do cliente
     * @param userId identificador do usuário a desbloquear
     * @return os dados atualizados do usuário
     */
    @Transactional
    public AdminUserResponse unblockUser(String actorUserId, String empresaId, Long userId) {
        UserEntity user = requireEmpresaUser(empresaId, userId);
        user.setStatus(UserStatus.ATIVO);

        auditEventService.auditAdminAction(
                actorUserId, empresaId, AuditEventType.ADMIN_USER_UNBLOCKED,
                "Usuário desbloqueado: " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail()));

        return toUserResponse(user);
    }

    // ------------------------------------------------------------------
    // Internos
    // ------------------------------------------------------------------

    private InvitedUser provisionResponsibleUser(
            String actorUserId,
            String empresaId,
            String name,
            String email,
            boolean sendInvite,
            Instant now
    ) {
        UserEntity user = new UserEntity();
        user.setEmpresaId(empresaId);
        user.setName(name.trim());
        user.setEmail(email.trim());
        user.setRoles(Set.of(EMPRESA_ROLE));
        user.setCreatedAt(now);
        // Senha aleatória inutilizável: o acesso é estabelecido pelo convite.
        user.setPasswordHash(passwordEncoder.encode(UUID.randomUUID().toString()));

        String token = null;
        if (sendInvite) {
            token = generateInviteToken(user);
            user.setStatus(UserStatus.CONVIDADO);
        } else {
            user.setStatus(UserStatus.ATIVO);
        }
        userRepository.save(user);

        auditEventService.auditAdminAction(
                actorUserId, empresaId, AuditEventType.ADMIN_USER_INVITED,
                "Usuário EMPRESA criado: " + user.getEmail(),
                auditMetadata.of("userId", user.getId(), "email", user.getEmail(),
                        "role", EMPRESA_ROLE, "sendInvite", sendInvite));

        return new InvitedUser(user, sendInvite ? inviteUrl(token) : null);
    }

    private String generateInviteToken(UserEntity user) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String token = URL_ENCODER.encodeToString(bytes);
        user.setInviteTokenHash(passwordEncoder.encode(token));
        user.setInvitedAt(Instant.now());
        user.setInviteExpiresAt(Instant.now().plus(inviteTtlHours, ChronoUnit.HOURS));
        return token;
    }

    private String inviteUrl(String token) {
        if (token == null) {
            return null;
        }
        return publicBaseUrl + "/convite/" + token;
    }

    private EmpresaEntity requireClient(String empresaId) {
        if (PLATFORM_EMPRESA_ID.equals(empresaId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado.");
        }
        return empresaRepository.findById(empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
    }

    private UserEntity requireEmpresaUser(String empresaId, Long userId) {
        requireClient(empresaId);
        return userRepository.findByIdAndEmpresaId(userId, empresaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private String resolveCompanyId(String requested) {
        if (requested != null && !requested.isBlank()) {
            String candidate = requested.trim();
            if (empresaRepository.findFirstByCompanyId(candidate).isPresent()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT, "Já existe um cliente com este identificador de integração.");
            }
            return candidate;
        }
        String generated;
        do {
            generated = "emp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        } while (empresaRepository.findFirstByCompanyId(generated).isPresent());
        return generated;
    }

    private String generateEmpresaId() {
        String id;
        do {
            id = "tnt_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24);
        } while (empresaRepository.existsById(id));
        return id;
    }

    private EmpresaAdminSummaryResponse toSummary(EmpresaEntity empresa, Period period) {
        return new EmpresaAdminSummaryResponse(
                empresa.getId(),
                empresa.getName(),
                empresa.getTradeName(),
                empresa.getTaxId(),
                empresa.getCorporateEmail(),
                empresa.getCommercialPlanType(),
                empresa.getStatus(),
                adminUsageService.countCompletedInPeriod(empresa.getId(), period.start(), period.end()),
                creditService.getBalance(empresa.getId()),
                empresa.getCreatedAt()
        );
    }

    private EmpresaAdminDetailResponse toDetail(EmpresaEntity empresa, Period period) {
        List<AdminUserResponse> users = userRepository
                .findByEmpresaIdOrderByCreatedAtAsc(empresa.getId()).stream()
                .map(AdminEmpresaService::toUserResponse)
                .toList();
        return new EmpresaAdminDetailResponse(
                empresa.getId(),
                empresa.getName(),
                empresa.getTradeName(),
                empresa.getLegalName(),
                empresa.getTaxId(),
                empresa.getCorporateEmail(),
                empresa.getPhone(),
                empresa.getWebsite(),
                empresa.isHealthVertical(),
                empresa.getCommercialPlanType(),
                empresa.getCommercialCondition(),
                empresa.getStatus(),
                adminUsageService.countCompletedInPeriod(empresa.getId(), period.start(), period.end()),
                creditService.getBalance(empresa.getId()),
                users,
                empresa.getCreatedAt(),
                empresa.getUpdatedAt()
        );
    }

    private static AdminUserResponse toUserResponse(UserEntity user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRoles(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }

    private Period resolvePeriod(Instant periodStart, Instant periodEnd) {
        Instant end = periodEnd == null ? Instant.now() : periodEnd;
        Instant start = periodStart == null ? end.minus(usagePeriodDays, ChronoUnit.DAYS) : periodStart;
        return new Period(start, end);
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private record Period(Instant start, Instant end) {
    }

    private record InvitedUser(UserEntity user, String inviteUrl) {
    }
}
