package br.com.iforce.praxis.admin.controller;

import br.com.iforce.praxis.admin.dto.AdminActionReasonRequest;

import br.com.iforce.praxis.admin.dto.CreateEmpresaAdminRequest;

import br.com.iforce.praxis.admin.dto.CreateEmpresaAdminResponse;

import br.com.iforce.praxis.admin.dto.ReactivateEmpresaAdminRequest;

import br.com.iforce.praxis.admin.dto.EmpresaAdminDetailResponse;

import br.com.iforce.praxis.admin.dto.EmpresaAdminSummaryResponse;

import br.com.iforce.praxis.admin.dto.EmpresaHealthResponse;

import br.com.iforce.praxis.admin.dto.EmpresaUsageResponse;

import br.com.iforce.praxis.admin.dto.GrantCreditsAdminRequest;

import br.com.iforce.praxis.admin.dto.UpdateEmpresaAdminRequest;

import br.com.iforce.praxis.admin.model.CommercialPlanType;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.admin.service.AdminAuditService;

import br.com.iforce.praxis.admin.service.AdminEmpresaService;

import br.com.iforce.praxis.admin.service.AdminUsageService;

import br.com.iforce.praxis.admin.service.CustomerHealthService;

import br.com.iforce.praxis.auth.service.CurrentUserService;

import io.swagger.v3.oas.annotations.Operation;

import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;

import org.springframework.web.bind.annotation.PatchMapping;

import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.web.bind.annotation.PostMapping;

import org.springframework.web.bind.annotation.RequestBody;

import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.RestController;


import java.time.Instant;

import java.util.List;


/**
 * API administrativa de clientes (empresas). Exige papel {@code ADMIN} e não depende do empresa
 * do usuário logado: o cliente alvo vem sempre explícito na rota.
 */
@RestController
@RequestMapping("/api/admin/empresas")
@Tag(name = "Admin · Clientes", description = "Cadastro e governança de clientes (empresas) pelo operador ADMIN.")
public class AdminEmpresaController {

    private final AdminEmpresaService adminEmpresaService;
    private final AdminUsageService adminUsageService;
    private final AdminAuditService adminAuditService;
    private final CustomerHealthService customerHealthService;
    private final CurrentUserService currentUserService;

    public AdminEmpresaController(
            AdminEmpresaService adminEmpresaService,
            AdminUsageService adminUsageService,
            AdminAuditService adminAuditService,
            CustomerHealthService customerHealthService,
            CurrentUserService currentUserService
    ) {
        this.adminEmpresaService = adminEmpresaService;
        this.adminUsageService = adminUsageService;
        this.adminAuditService = adminAuditService;
        this.customerHealthService = customerHealthService;
        this.currentUserService = currentUserService;
    }

    /**
     * Lista os clientes da plataforma, com filtros de busca, situação, plano e período.
     *
     * <p>Na visão do processo: é a tela inicial de gestão de clientes. Devolve, para cada
     * cliente, o resumo já com uso no período e saldo de créditos.</p>
     *
     * @param search trecho do nome para procurar (opcional)
     * @param status situação do cliente para filtrar (opcional)
     * @param plan tipo de plano comercial para filtrar (opcional)
     * @param periodStart início do período de uso, em formato de data/hora ISO (opcional)
     * @param periodEnd fim do período de uso, em formato de data/hora ISO (opcional)
     * @return a lista de clientes que atendem aos filtros
     */
    @GetMapping
    @Operation(summary = "Lista clientes", description = "Filtra por busca livre, status, plano comercial e período de uso.")
    public ResponseEntity<List<EmpresaAdminSummaryResponse>> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EmpresaStatus status,
            @RequestParam(required = false) CommercialPlanType plan,
            @RequestParam(required = false) String periodStart,
            @RequestParam(required = false) String periodEnd
    ) {
        return ResponseEntity.ok(adminEmpresaService.list(
                search, status, plan, parseInstant(periodStart), parseInstant(periodEnd)));
    }

    @GetMapping("/at-risk")
    @Operation(
            summary = "Fila de atuação de Customer Success",
            description = "Clientes ativos cuja utilização caiu além do limite configurado (por padrão, mais de 30%) "
                    + "comparando os últimos 30 dias com o período anterior. Base da atuação proativa de retenção.")
    public ResponseEntity<List<EmpresaHealthResponse>> atRisk() {
        return ResponseEntity.ok(customerHealthService.atRiskEmpresas(Instant.now()));
    }

    /**
     * Cadastra um cliente novo e o seu primeiro usuário responsável.
     *
     * <p>Na visão do processo: é a "abertura de conta" acionada pelo operador logado, cujo
     * nome fica registrado como autor da criação.</p>
     *
     * @param request dados da empresa e do responsável a convidar
     * @return a ficha do cliente criado, com o id do responsável e o link de convite
     */
    @PostMapping
    @Operation(summary = "Cadastra cliente", description = "Cria o empresa e o primeiro usuário responsável com papel EMPRESA.")
    public ResponseEntity<CreateEmpresaAdminResponse> create(@Valid @RequestBody CreateEmpresaAdminRequest request) {
        return ResponseEntity.ok(adminEmpresaService.create(currentUserService.requiredUserId(), request));
    }

    /**
     * Abre a ficha completa de um cliente.
     *
     * <p>Na visão do processo: é o "prontuário" do cliente — cadastro, plano, situação,
     * uso, saldo e usuários de acesso.</p>
     *
     * @param empresaId identificador do cliente
     * @return a ficha completa do cliente
     */
    @GetMapping("/{empresaId}")
    @Operation(summary = "Detalha cliente")
    public ResponseEntity<EmpresaAdminDetailResponse> detail(@PathVariable String empresaId) {
        return ResponseEntity.ok(adminEmpresaService.detail(empresaId));
    }

    /**
     * Atualiza o cadastro de um cliente (dados, plano e condição comercial).
     *
     * <p>Na visão do processo: é a "edição do cadastro". Só altera o que foi enviado;
     * trocas de plano e de condição comercial ficam destacadas na auditoria.</p>
     *
     * @param empresaId identificador do cliente
     * @param request campos a atualizar (apenas os preenchidos são aplicados)
     * @return a ficha atualizada do cliente
     */
    @PatchMapping("/{empresaId}")
    @Operation(summary = "Atualiza dados do cliente")
    public ResponseEntity<EmpresaAdminDetailResponse> update(
            @PathVariable String empresaId,
            @Valid @RequestBody UpdateEmpresaAdminRequest request
    ) {
        return ResponseEntity.ok(adminEmpresaService.update(
                currentUserService.requiredUserId(), empresaId, request));
    }

    /**
     * Suspende um cliente, pausando o acesso dele à plataforma.
     *
     * <p>Na visão do processo: é o "pausar" — o cliente para de conseguir usar o sistema,
     * mas nada é apagado. Exige motivo, que fica registrado na auditoria.</p>
     *
     * @param empresaId identificador do cliente
     * @param request motivo da suspensão
     * @return a ficha atualizada do cliente
     */
    @PostMapping("/{empresaId}/suspend")
    @Operation(summary = "Suspende cliente", description = "Exige motivo. Cliente suspenso não autentica nem consome APIs protegidas.")
    public ResponseEntity<EmpresaAdminDetailResponse> suspend(
            @PathVariable String empresaId,
            @Valid @RequestBody AdminActionReasonRequest request
    ) {
        return ResponseEntity.ok(adminEmpresaService.suspend(
                currentUserService.requiredUserId(), empresaId, request.reason()));
    }

    /**
     * Reativa um cliente suspenso ou cancelado, devolvendo o acesso.
     *
     * <p>Na visão do processo: é o "religar" — o operador escolhe se o cliente volta como
     * ativo ou em teste. Exige motivo, registrado na auditoria.</p>
     *
     * @param empresaId identificador do cliente
     * @param request situação alvo (ativo ou em teste) e motivo
     * @return a ficha atualizada do cliente
     */
    @PostMapping("/{empresaId}/reactivate")
    @Operation(summary = "Reativa cliente", description = "Exige motivo. Status alvo ATIVO ou EM_TESTE.")
    public ResponseEntity<EmpresaAdminDetailResponse> reactivate(
            @PathVariable String empresaId,
            @Valid @RequestBody ReactivateEmpresaAdminRequest request
    ) {
        return ResponseEntity.ok(adminEmpresaService.reactivate(
                currentUserService.requiredUserId(), empresaId, request));
    }

    /**
     * Cancela um cliente, encerrando a relação comercial.
     *
     * <p>Na visão do processo: é o "encerramento" — diferente da suspensão, marca o fim da
     * relação. Todo o histórico é preservado. Exige motivo, registrado na auditoria.</p>
     *
     * @param empresaId identificador do cliente
     * @param request motivo do cancelamento
     * @return a ficha atualizada do cliente
     */
    @PostMapping("/{empresaId}/cancel")
    @Operation(summary = "Cancela cliente", description = "Exige motivo. Preserva todo o histórico.")
    public ResponseEntity<EmpresaAdminDetailResponse> cancel(
            @PathVariable String empresaId,
            @Valid @RequestBody AdminActionReasonRequest request
    ) {
        return ResponseEntity.ok(adminEmpresaService.cancel(
                currentUserService.requiredUserId(), empresaId, request.reason()));
    }

    /**
     * Concede créditos de cortesia a um cliente para liberar avaliações.
     *
     * <p>Na visão do processo: é o "cortesia na conta" — o operador soma créditos ao saldo
     * do cliente (por exemplo, para um cliente pré-pago voltar a rodar provas). Não muda o
     * plano comercial e fica registrado na auditoria.</p>
     *
     * @param empresaId identificador do cliente
     * @param request quantidade de créditos e observação opcional
     * @return a ficha atualizada do cliente, já com o novo saldo
     */
    @PostMapping("/{empresaId}/credits")
    @Operation(summary = "Concede créditos de cortesia", description = "Adiciona créditos ao saldo do cliente para liberar testes. Registra evento de auditoria.")
    public ResponseEntity<EmpresaAdminDetailResponse> grantCredits(
            @PathVariable String empresaId,
            @Valid @RequestBody GrantCreditsAdminRequest request
    ) {
        return ResponseEntity.ok(adminEmpresaService.grantCredits(
                currentUserService.requiredUserId(), empresaId, request.amount(), request.note()));
    }

    /**
     * Mostra o consumo (avaliações concluídas) de um cliente no período.
     *
     * <p>Na visão do processo: é a aba "Uso" do cliente. Se o período não for informado,
     * assume-se os últimos 30 dias. Consultar o uso é, por si, uma ação registrada na
     * auditoria antes de os números serem devolvidos.</p>
     *
     * @param empresaId identificador do cliente
     * @param periodStart início do período, em data/hora ISO (opcional; padrão: 30 dias atrás)
     * @param periodEnd fim do período, em data/hora ISO (opcional; padrão: agora)
     * @return o panorama de consumo do cliente
     */
    @GetMapping("/{empresaId}/usage")
    @Operation(summary = "Uso do cliente", description = "Avaliações concluídas no período. Registra evento de auditoria.")
    public ResponseEntity<EmpresaUsageResponse> usage(
            @PathVariable String empresaId,
            @RequestParam(required = false) String periodStart,
            @RequestParam(required = false) String periodEnd
    ) {
        Instant end = periodEnd == null ? Instant.now() : parseInstant(periodEnd);
        Instant start = periodStart == null ? end.minusSeconds(30L * 24 * 60 * 60) : parseInstant(periodStart);
        adminAuditService.recordUsageViewed(currentUserService.requiredUserId(), empresaId);
        return ResponseEntity.ok(adminUsageService.usage(empresaId, start, end));
    }

    /**
     * Lista o histórico de ações ligadas a um cliente.
     *
     * <p>Na visão do processo: é o "extrato de acontecimentos" do cliente — só leitura,
     * do mais recente para o mais antigo.</p>
     *
     * @param empresaId identificador do cliente
     * @param limit quantidade máxima de eventos a trazer (padrão: 200)
     * @return os eventos de auditoria do cliente
     */
    @GetMapping("/{empresaId}/audit")
    @Operation(summary = "Auditoria do cliente", description = "Eventos append-only ligados ao empresa (somente leitura).")
    public ResponseEntity<?> audit(
            @PathVariable String empresaId,
            @RequestParam(required = false, defaultValue = "200") int limit
    ) {
        return ResponseEntity.ok(adminAuditService.listForEmpresa(empresaId, limit));
    }

    private static Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Instant.parse(value);
    }
}
