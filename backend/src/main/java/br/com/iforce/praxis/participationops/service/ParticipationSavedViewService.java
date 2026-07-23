package br.com.iforce.praxis.participationops.service;

import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.SavedViewRequest;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.SavedViewResponse;
import br.com.iforce.praxis.participationops.persistence.entity.ParticipationSavedViewEntity;
import br.com.iforce.praxis.participationops.persistence.repository.ParticipationSavedViewRepository;
import br.com.iforce.praxis.shared.security.EmpresaSecurity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ParticipationSavedViewService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<String>> LIST_TYPE = new TypeReference<>() {
    };

    private final ParticipationSavedViewRepository repository;
    private final ObjectMapper objectMapper;

    public ParticipationSavedViewService(
            ParticipationSavedViewRepository repository,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<SavedViewResponse> list(String userId) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        return repository
                .findByEmpresaIdAndOwnerUserIdOrEmpresaIdAndSharedTrueOrderByNameAsc(
                        empresaId,
                        userId,
                        empresaId
                )
                .stream()
                .map(this::response)
                .toList();
    }

    @Transactional
    public SavedViewResponse create(String userId, SavedViewRequest request) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        String name = request.name().trim();
        if (repository.existsByEmpresaIdAndOwnerUserIdAndNameIgnoreCase(empresaId, userId, name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já possui uma visão com este nome.");
        }

        Instant now = Instant.now();
        ParticipationSavedViewEntity entity = new ParticipationSavedViewEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setEmpresaId(empresaId);
        entity.setOwnerUserId(userId);
        entity.setCreatedAt(now);
        apply(entity, request, now);
        try {
            return response(repository.save(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já possui uma visão com este nome.", exception);
        }
    }

    @Transactional
    public SavedViewResponse update(String userId, String viewId, SavedViewRequest request) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        ParticipationSavedViewEntity entity = required(empresaId, viewId);
        assertOwner(userId, entity);
        apply(entity, request, Instant.now());
        try {
            return response(repository.save(entity));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Você já possui uma visão com este nome.", exception);
        }
    }

    @Transactional
    public void delete(String userId, String viewId) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        ParticipationSavedViewEntity entity = required(empresaId, viewId);
        assertOwner(userId, entity);
        repository.delete(entity);
    }

    private void apply(ParticipationSavedViewEntity entity, SavedViewRequest request, Instant now) {
        entity.setName(request.name().trim());
        entity.setShared(request.shared());
        entity.setFiltersJson(write(request.filters()));
        entity.setSortJson(write(request.sort() == null ? Map.of() : request.sort()));
        entity.setColumnsJson(write(request.columns() == null ? List.of() : request.columns()));
        entity.setUpdatedAt(now);
    }

    private ParticipationSavedViewEntity required(String empresaId, String viewId) {
        return repository.findByEmpresaIdAndId(empresaId, viewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Visão salva não encontrada."));
    }

    private static void assertOwner(String userId, ParticipationSavedViewEntity entity) {
        if (!entity.getOwnerUserId().equals(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Somente o proprietário pode alterar ou excluir esta visão."
            );
        }
    }

    private SavedViewResponse response(ParticipationSavedViewEntity entity) {
        return new SavedViewResponse(
                entity.getId(),
                entity.getOwnerUserId(),
                entity.getName(),
                entity.isShared(),
                readMap(entity.getFiltersJson()),
                readMap(entity.getSortJson()),
                readList(entity.getColumnsJson()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getVersion()
        );
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível salvar a configuração.", exception);
        }
    }

    private Map<String, Object> readMap(String value) {
        if (value == null || value.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Visão salva inválida.", exception);
        }
    }

    private List<String> readList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(value, LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Colunas da visão inválidas.", exception);
        }
    }
}
