package br.com.iforce.praxis.marketplace.service;

import br.com.iforce.praxis.marketplace.dto.MessageResponse;
import br.com.iforce.praxis.marketplace.dto.MessageThreadResponse;
import br.com.iforce.praxis.marketplace.dto.SendMessageRequest;
import br.com.iforce.praxis.marketplace.model.ListingStatus;
import br.com.iforce.praxis.marketplace.model.MessageSenderType;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceListingEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceMessageEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceMessageThreadEntity;
import br.com.iforce.praxis.marketplace.persistence.entity.MarketplaceProfessionalEntity;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceListingRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceMessageRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceMessageThreadRepository;
import br.com.iforce.praxis.marketplace.persistence.repository.MarketplaceProfessionalRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
/**
 * Controla a conversa entre empresa interessada e profissional dentro do marketplace.
 *
 * <p>Esse serviço representa o canal oficial de contato comercial antes ou durante a relação
 * de compra, preservando o contexto por thread e aplicando limites básicos de uso.</p>
 */
public class MarketplaceMessageService {

    private static final int MAX_MESSAGES_PER_THREAD_PER_HOUR = 20;

    private final MarketplaceMessageThreadRepository threadRepository;
    private final MarketplaceMessageRepository messageRepository;
    private final MarketplaceListingRepository listingRepository;
    private final MarketplaceProfessionalRepository professionalRepository;

    public MarketplaceMessageService(
            MarketplaceMessageThreadRepository threadRepository,
            MarketplaceMessageRepository messageRepository,
            MarketplaceListingRepository listingRepository,
            MarketplaceProfessionalRepository professionalRepository
    ) {
        this.threadRepository = threadRepository;
        this.messageRepository = messageRepository;
        this.listingRepository = listingRepository;
        this.professionalRepository = professionalRepository;
    }

    @Transactional
    /**
     * Envia uma mensagem em nome do tenant comprador ou potencial comprador.
     *
     * <p>O fluxo permite iniciar uma nova conversa sobre um item ou continuar uma thread existente
     * sem perder o histórico de interação entre as partes.</p>
     */
    public MessageThreadResponse sendAsTenant(String tenantId, Long userId, SendMessageRequest request) {
        MarketplaceMessageThreadEntity thread = resolveTenantThread(tenantId, request);
        appendMessage(thread.getId(), MessageSenderType.TENANT, userId, request.body());
        return toThreadResponse(thread);
    }

    @Transactional
    /**
     * Envia uma resposta do profissional em uma conversa já aberta.
     *
     * <p>Na visão do processo, esta é a continuidade do atendimento comercial dado pelo autor
     * do item anunciado.</p>
     */
    public MessageThreadResponse sendAsProfessional(String userId, SendMessageRequest request) {
        MarketplaceProfessionalEntity professional = loadProfessional(userId);
        if (request.threadId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "threadId e obrigatorio para resposta do profissional.");
        }
        MarketplaceMessageThreadEntity thread = threadRepository.findById(request.threadId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread nao encontrada."));
        if (!thread.getProfessionalId().equals(professional.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Thread pertence a outro profissional.");
        }
        appendMessage(thread.getId(), MessageSenderType.PROFESSIONAL, professional.getId(), request.body());
        return toThreadResponse(thread);
    }

    @Transactional(readOnly = true)
    /**
     * Lista todas as conversas do tenant com profissionais do marketplace.
     */
    public List<MessageThreadResponse> listForTenant(String tenantId) {
        return threadRepository.findByRequesterTenantIdOrderByCreatedAtDesc(tenantId)
                .stream()
                .map(this::toThreadResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    /**
     * Lista todas as conversas recebidas pelo profissional.
     */
    public List<MessageThreadResponse> listForProfessional(String userId) {
        MarketplaceProfessionalEntity professional = loadProfessional(userId);
        return threadRepository.findByProfessionalIdOrderByCreatedAtDesc(professional.getId())
                .stream()
                .map(this::toThreadResponse)
                .toList();
    }

    private MarketplaceMessageThreadEntity resolveTenantThread(String tenantId, SendMessageRequest request) {
        if (request.threadId() != null) {
            MarketplaceMessageThreadEntity thread = threadRepository.findById(request.threadId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread nao encontrada."));
            if (!thread.getRequesterTenantId().equals(tenantId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Thread pertence a outro tenant.");
            }
            return thread;
        }
        if (request.listingId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "listingId ou threadId e obrigatorio.");
        }
        return threadRepository.findByListingIdAndRequesterTenantId(request.listingId(), tenantId)
                .orElseGet(() -> createThread(tenantId, request.listingId()));
    }

    private MarketplaceMessageThreadEntity createThread(String tenantId, Long listingId) {
        MarketplaceListingEntity listing = listingRepository.findByIdAndStatus(listingId, ListingStatus.APPROVED)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Listing nao encontrado."));
        MarketplaceMessageThreadEntity thread = new MarketplaceMessageThreadEntity();
        thread.setListingId(listing.getId());
        thread.setProfessionalId(listing.getProfessionalId());
        thread.setRequesterTenantId(tenantId);
        return threadRepository.save(thread);
    }

    private void appendMessage(Long threadId, MessageSenderType senderType, Long senderId, String body) {
        long recentMessages = messageRepository.countByThreadIdAndCreatedAtAfter(
                threadId,
                Instant.now().minus(1, ChronoUnit.HOURS)
        );
        if (recentMessages >= MAX_MESSAGES_PER_THREAD_PER_HOUR) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Limite de mensagens por hora atingido.");
        }
        MarketplaceMessageEntity message = new MarketplaceMessageEntity();
        message.setThreadId(threadId);
        message.setSenderType(senderType);
        message.setSenderId(senderId);
        message.setBody(body.trim());
        messageRepository.save(message);
    }

    private MessageThreadResponse toThreadResponse(MarketplaceMessageThreadEntity thread) {
        return new MessageThreadResponse(
                thread.getId(),
                thread.getListingId(),
                thread.getProfessionalId(),
                thread.getRequesterTenantId(),
                thread.getCreatedAt(),
                messageRepository.findByThreadIdOrderByCreatedAtAsc(thread.getId())
                        .stream()
                        .map(this::toMessageResponse)
                        .toList()
        );
    }

    private MessageResponse toMessageResponse(MarketplaceMessageEntity message) {
        return new MessageResponse(
                message.getId(),
                message.getThreadId(),
                message.getSenderType(),
                message.getSenderId(),
                message.getBody(),
                message.getCreatedAt()
        );
    }

    private MarketplaceProfessionalEntity loadProfessional(String userId) {
        Long id;
        try {
            id = Long.valueOf(userId);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Sessao invalida.");
        }
        return professionalRepository.findByUserId(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profissional nao encontrado."));
    }
}
