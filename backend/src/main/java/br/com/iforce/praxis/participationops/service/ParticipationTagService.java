package br.com.iforce.praxis.participationops.service;

import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.ParticipationRef;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.ParticipationTagsResponse;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.TagRequest;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.TagResponse;
import br.com.iforce.praxis.participationops.persistence.entity.ParticipationTagAssignmentEntity;
import br.com.iforce.praxis.participationops.persistence.entity.ParticipationTagEntity;
import br.com.iforce.praxis.participationops.persistence.repository.ParticipationTagAssignmentRepository;
import br.com.iforce.praxis.participationops.persistence.repository.ParticipationTagRepository;
import br.com.iforce.praxis.shared.security.EmpresaSecurity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ParticipationTagService {

    private final ParticipationTagRepository tagRepository;
    private final ParticipationTagAssignmentRepository assignmentRepository;

    public ParticipationTagService(
            ParticipationTagRepository tagRepository,
            ParticipationTagAssignmentRepository assignmentRepository
    ) {
        this.tagRepository = tagRepository;
        this.assignmentRepository = assignmentRepository;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list() {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        return tagRepository.findByEmpresaIdOrderByNameAsc(empresaId).stream()
                .map(ParticipationTagService::response)
                .toList();
    }

    @Transactional
    public TagResponse create(String userId, TagRequest request) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        String name = request.name().trim();
        if (tagRepository.existsByEmpresaIdAndNameIgnoreCase(empresaId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma tag com este nome.");
        }
        Instant now = Instant.now();
        ParticipationTagEntity entity = new ParticipationTagEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setEmpresaId(empresaId);
        entity.setCreatedBy(userId);
        entity.setCreatedAt(now);
        apply(entity, request, now);
        try {
            return response(tagRepository.save(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma tag com este nome.", exception);
        }
    }

    @Transactional
    public TagResponse update(String tagId, TagRequest request) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        ParticipationTagEntity entity = required(empresaId, tagId);
        apply(entity, request, Instant.now());
        try {
            return response(tagRepository.save(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma tag com este nome.", exception);
        }
    }

    @Transactional
    public void delete(String tagId) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        ParticipationTagEntity entity = required(empresaId, tagId);
        assignmentRepository.deleteByEmpresaIdAndTagId(empresaId, tagId);
        tagRepository.delete(entity);
    }

    @Transactional
    public void add(String userId, ParticipationRef ref, String tagId) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        required(empresaId, tagId);
        if (assignmentRepository
                .findByEmpresaIdAndParticipationTypeAndParticipationIdAndTagId(
                        empresaId,
                        ref.type(),
                        ref.id(),
                        tagId
                )
                .isPresent()) {
            return;
        }
        ParticipationTagAssignmentEntity entity = new ParticipationTagAssignmentEntity();
        entity.setEmpresaId(empresaId);
        entity.setParticipationType(ref.type());
        entity.setParticipationId(ref.id());
        entity.setTagId(tagId);
        entity.setCreatedBy(userId);
        entity.setCreatedAt(Instant.now());
        try {
            assignmentRepository.save(entity);
        } catch (DataIntegrityViolationException ignored) {
            // Operação idempotente: atribuição concorrente já concluída.
        }
    }

    @Transactional
    public void remove(ParticipationRef ref, String tagId) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        assignmentRepository
                .findByEmpresaIdAndParticipationTypeAndParticipationIdAndTagId(
                        empresaId,
                        ref.type(),
                        ref.id(),
                        tagId
                )
                .ifPresent(assignmentRepository::delete);
    }

    @Transactional(readOnly = true)
    public List<ParticipationTagsResponse> tagsFor(List<ParticipationRef> refs) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        Map<String, ParticipationTagEntity> tags = tagRepository.findByEmpresaIdOrderByNameAsc(empresaId).stream()
                .collect(Collectors.toMap(ParticipationTagEntity::getId, Function.identity()));
        return refs.stream().map(ref -> {
            List<TagResponse> assigned = assignmentRepository
                    .findByEmpresaIdAndParticipationTypeAndParticipationId(
                            empresaId,
                            ref.type(),
                            ref.id()
                    )
                    .stream()
                    .map(ParticipationTagAssignmentEntity::getTagId)
                    .map(tags::get)
                    .filter(tag -> tag != null)
                    .map(ParticipationTagService::response)
                    .toList();
            return new ParticipationTagsResponse(ref.type(), ref.id(), assigned);
        }).toList();
    }

    private static void apply(ParticipationTagEntity entity, TagRequest request, Instant now) {
        entity.setName(request.name().trim());
        entity.setColor(request.color().toUpperCase());
        entity.setDescription(trimToNull(request.description()));
        entity.setUpdatedAt(now);
    }

    private ParticipationTagEntity required(String empresaId, String tagId) {
        return tagRepository.findByEmpresaIdAndId(empresaId, tagId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag não encontrada."));
    }

    private static TagResponse response(ParticipationTagEntity entity) {
        return new TagResponse(
                entity.getId(),
                entity.getName(),
                entity.getColor(),
                entity.getDescription(),
                entity.getCreatedBy(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
