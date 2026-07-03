package br.com.iforce.praxis.admin.service;

import br.com.iforce.praxis.admin.dto.AdminAuditEventResponse;

import br.com.iforce.praxis.audit.model.AuditEventType;

import br.com.iforce.praxis.audit.persistence.entity.AuditEventEntity;

import br.com.iforce.praxis.audit.persistence.repository.AuditEventRepository;

import br.com.iforce.praxis.audit.service.AuditEventService;

import br.com.iforce.praxis.audit.service.AuditMetadata;

import org.springframework.data.domain.PageRequest;

import org.springframework.http.HttpStatus;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.server.ResponseStatusException;


import java.util.List;


/**
 * Leitura da trilha de auditoria pelo painel ADMIN. A trilha é append-only: este serviço
 * apenas consulta eventos; não há caminho para editar, excluir ou ocultar registros.
 */
@Service
public class AdminAuditService {

    private final AuditEventRepository auditEventRepository;
    private final AuditEventService auditEventService;
    private final AuditMetadata auditMetadata;

    public AdminAuditService(
            AuditEventRepository auditEventRepository,
            AuditEventService auditEventService,
            AuditMetadata auditMetadata
    ) {
        this.auditEventRepository = auditEventRepository;
        this.auditEventService = auditEventService;
        this.auditMetadata = auditMetadata;
    }

    /**
     * Mostra o histórico de ações ligadas a um cliente específico.
     *
     * <p>Na visão do processo: é o "extrato de acontecimentos" daquele cliente — quem fez
     * o quê e quando (criação, suspensão, convites, consultas de uso, etc.), do mais
     * recente para o mais antigo. Serve para o operador entender o que aconteceu com um
     * cliente ou prestar contas. É só leitura: nada aqui pode ser alterado ou apagado.</p>
     *
     * @param empresaId identificador do cliente
     * @param limit quantidade máxima de eventos a trazer
     * @return os eventos do cliente, do mais recente para o mais antigo
     */
    @Transactional(readOnly = true)
    public List<AdminAuditEventResponse> listForEmpresa(String empresaId, int limit) {
        return auditEventRepository
                .findByEmpresaIdOrderByCreatedAtDesc(empresaId, PageRequest.of(0, limit)).stream()
                .map(AdminAuditService::toResponse)
                .toList();
    }

    /**
     * Mostra o histórico geral de ações de toda a plataforma.
     *
     * <p>Na visão do processo: é o "diário de bordo" completo — os últimos acontecimentos
     * de todos os clientes juntos, do mais recente para o mais antigo. Útil para uma visão
     * panorâmica de tudo o que está sendo feito na operação. Também é só leitura.</p>
     *
     * @param limit quantidade máxima de eventos a trazer
     * @return os eventos de toda a plataforma, do mais recente para o mais antigo
     */
    @Transactional(readOnly = true)
    public List<AdminAuditEventResponse> listAll(int limit) {
        return auditEventRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).stream()
                .map(AdminAuditService::toResponse)
                .toList();
    }

    /**
     * Abre um evento de auditoria específico para ver todos os seus detalhes.
     *
     * <p>Na visão do processo: é o "zoom" em um acontecimento do histórico — o operador
     * clica em uma linha do extrato e vê o registro completo (ator, cliente, tipo de ação,
     * descrição e os detalhes adicionais). Se o evento não existir, o sistema avisa que não
     * foi encontrado.</p>
     *
     * @param eventId identificador do evento de auditoria
     * @return o detalhe completo do evento
     */
    @Transactional(readOnly = true)
    public AdminAuditEventResponse getById(Long eventId) {
        return auditEventRepository.findById(eventId)
                .map(AdminAuditService::toResponse)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Evento de auditoria não encontrado."));
    }

    /**
     * Deixa registrado que o operador consultou o uso de um cliente.
     *
     * <p>Na visão do processo: consultar o consumo de um cliente é, por si só, uma ação que
     * precisa ficar registrada — afinal, envolve olhar dados de negócio de terceiros. Por
     * isso, toda vez que o uso de um cliente é aberto, grava-se um evento na trilha em nome
     * do operador, garantindo transparência sobre quem acessou o quê.</p>
     *
     * @param actorUserId identificador do operador ADMIN que consultou
     * @param empresaId identificador do cliente consultado
     */
    @Transactional
    public void recordUsageViewed(String actorUserId, String empresaId) {
        auditEventService.auditAdminAction(
                actorUserId, empresaId, AuditEventType.ADMIN_USAGE_VIEWED,
                "Uso do cliente consultado.",
                auditMetadata.of("empresaId", empresaId));
    }

    private static AdminAuditEventResponse toResponse(AuditEventEntity entity) {
        return new AdminAuditEventResponse(
                entity.getId(),
                entity.getActorUserId(),
                entity.getEmpresaId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getMessage(),
                entity.getMetadata(),
                entity.getCreatedAt()
        );
    }
}
