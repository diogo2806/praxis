package br.com.iforce.praxis.participationops.service;

import br.com.iforce.praxis.candidate.dto.ParticipationMonitoringPageResponse;
import br.com.iforce.praxis.candidate.dto.ParticipationMonitoringResponse;
import br.com.iforce.praxis.candidate.service.ParticipationMonitoringQueryService;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkExcludedItem;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkFilter;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkItemResponse;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkJobResponse;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkPreviewRequest;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkPreviewResponse;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.BulkRequest;
import br.com.iforce.praxis.participationops.dto.ParticipationOperationsContracts.ParticipationRef;
import br.com.iforce.praxis.participationops.persistence.entity.ParticipationBulkItemEntity;
import br.com.iforce.praxis.participationops.persistence.entity.ParticipationBulkJobEntity;
import br.com.iforce.praxis.participationops.persistence.repository.ParticipationBulkItemRepository;
import br.com.iforce.praxis.participationops.persistence.repository.ParticipationBulkJobRepository;
import br.com.iforce.praxis.participationops.persistence.repository.ParticipationTagRepository;
import br.com.iforce.praxis.shared.security.EmpresaSecurity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ParticipationBulkService {

    private static final int PAGE_SIZE = 100;
    private static final int MAX_FILTER_SELECTION = 5_000;
    private static final int MAX_EXPLICIT_SELECTION = 500;

    private final ParticipationMonitoringQueryService queryService;
    private final ParticipationBulkJobRepository jobRepository;
    private final ParticipationBulkItemRepository itemRepository;
    private final ParticipationTagRepository tagRepository;
    private final ParticipationBulkWorker worker;
    private final ParticipationBulkAuditService auditService;
    private final ObjectMapper objectMapper;

    public ParticipationBulkService(
            ParticipationMonitoringQueryService queryService,
            ParticipationBulkJobRepository jobRepository,
            ParticipationBulkItemRepository itemRepository,
            ParticipationTagRepository tagRepository,
            ParticipationBulkWorker worker,
            ParticipationBulkAuditService auditService,
            ObjectMapper objectMapper
    ) {
        this.queryService = queryService;
        this.jobRepository = jobRepository;
        this.itemRepository = itemRepository;
        this.tagRepository = tagRepository;
        this.worker = worker;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public BulkPreviewResponse preview(BulkPreviewRequest request) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        SelectionEvaluation evaluation = evaluate(
                empresaId,
                request.action(),
                request.selectionMode(),
                request.selected(),
                request.filter(),
                request.additionalDays(),
                request.tagId(),
                request.justification()
        );
        return new BulkPreviewResponse(
                evaluation.refs().size(),
                evaluation.eligibleCount(),
                evaluation.excludedReasons().size(),
                normalizeUpper(request.action()),
                impact(normalizeUpper(request.action()), evaluation.eligibleCount(), request.additionalDays()),
                evaluation.excludedReasons().entrySet().stream()
                        .map(entry -> new BulkExcludedItem(
                                entry.getKey().type(),
                                entry.getKey().id(),
                                entry.getValue()
                        ))
                        .toList()
        );
    }

    @Transactional
    public BulkJobResponse create(String userId, BulkRequest request) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        String idempotencyKey = request.idempotencyKey().trim();
        ParticipationBulkJobEntity existing = jobRepository
                .findByEmpresaIdAndIdempotencyKey(empresaId, idempotencyKey)
                .orElse(null);
        if (existing != null) {
            return response(existing, true);
        }

        SelectionEvaluation evaluation = evaluate(
                empresaId,
                request.action(),
                request.selectionMode(),
                request.selected(),
                request.filter(),
                request.additionalDays(),
                request.tagId(),
                request.justification()
        );
        if (evaluation.refs().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nenhuma participação corresponde à seleção.");
        }

        Instant now = Instant.now();
        ParticipationBulkJobEntity job = new ParticipationBulkJobEntity();
        job.setId(UUID.randomUUID().toString());
        job.setEmpresaId(empresaId);
        job.setRequestedBy(userId);
        job.setAction(normalizeUpper(request.action()));
        job.setSelectionMode(normalizeUpper(request.selectionMode()));
        job.setFilterJson(writeJson(request.filter()));
        job.setPayloadJson(writeJson(Map.of(
                "additionalDays", request.additionalDays() == null ? 0 : request.additionalDays(),
                "tagId", request.tagId() == null ? "" : request.tagId()
        )));
        job.setIdempotencyKey(idempotencyKey);
        job.setJustification(trimToNull(request.justification()));
        job.setStatus(evaluation.eligibleCount() == 0 ? "COMPLETED" : "PENDING");
        job.setTotalItems(evaluation.refs().size());
        job.setProcessedItems(evaluation.excludedReasons().size());
        job.setSucceededItems(0);
        job.setSkippedItems(evaluation.excludedReasons().size());
        job.setFailedItems(0);
        job.setCreatedAt(now);
        if (evaluation.eligibleCount() == 0) {
            job.setStartedAt(now);
            job.setCompletedAt(now);
        }

        try {
            jobRepository.saveAndFlush(job);
        } catch (DataIntegrityViolationException exception) {
            return jobRepository.findByEmpresaIdAndIdempotencyKey(empresaId, idempotencyKey)
                    .map(found -> response(found, true))
                    .orElseThrow(() -> exception);
        }

        List<ParticipationBulkItemEntity> items = evaluation.refs().stream().map(ref -> {
            ParticipationBulkItemEntity item = new ParticipationBulkItemEntity();
            item.setJobId(job.getId());
            item.setParticipationType(ref.type());
            item.setParticipationId(ref.id());
            String reason = evaluation.excludedReasons().get(ref);
            item.setStatus(reason == null ? "PENDING" : "SKIPPED");
            item.setReason(reason);
            item.setProcessedAt(reason == null ? null : now);
            return item;
        }).toList();
        itemRepository.saveAll(items);
        auditService.recordCreated(job);

        if (evaluation.eligibleCount() > 0) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    worker.process(job.getId());
                }
            });
        } else {
            auditService.recordCompleted(job);
        }
        return response(job, true);
    }

    @Transactional(readOnly = true)
    public List<BulkJobResponse> list() {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        return jobRepository.findTop50ByEmpresaIdOrderByCreatedAtDesc(empresaId).stream()
                .map(job -> response(job, false))
                .toList();
    }

    @Transactional(readOnly = true)
    public BulkJobResponse get(String jobId) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        return response(required(empresaId, jobId), true);
    }

    @Transactional(readOnly = true)
    public byte[] report(String jobId) {
        String empresaId = EmpresaSecurity.requiredEmpresa();
        ParticipationBulkJobEntity job = required(empresaId, jobId);
        StringBuilder csv = new StringBuilder();
        csv.append("job_id,action,participation_type,participation_id,status,reason,processed_at\n");
        for (ParticipationBulkItemEntity item : itemRepository.findByJobIdOrderByIdAsc(job.getId())) {
            csv.append(csv(job.getId())).append(',')
                    .append(csv(job.getAction())).append(',')
                    .append(csv(item.getParticipationType())).append(',')
                    .append(csv(item.getParticipationId())).append(',')
                    .append(csv(item.getStatus())).append(',')
                    .append(csv(item.getReason())).append(',')
                    .append(csv(item.getProcessedAt() == null ? null : item.getProcessedAt().toString()))
                    .append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private SelectionEvaluation evaluate(
            String empresaId,
            String actionValue,
            String selectionModeValue,
            List<ParticipationRef> selected,
            BulkFilter filter,
            Integer additionalDays,
            String tagId,
            String justification
    ) {
        String action = normalizeUpper(actionValue);
        String selectionMode = normalizeUpper(selectionModeValue);
        validateActionPayload(empresaId, action, additionalDays, tagId, justification);

        List<ParticipationRef> refs;
        Map<ParticipationRef, ParticipationMonitoringResponse> byRef;
        if ("EXPLICIT".equals(selectionMode)) {
            refs = normalizeExplicitSelection(selected);
            byRef = findExplicitParticipations(filter, refs);
        } else if ("FILTER".equals(selectionMode)) {
            List<ParticipationMonitoringResponse> filtered = findFilteredParticipations(filter);
            refs = filtered.stream()
                    .map(item -> new ParticipationRef(item.participationType(), item.participationId()))
                    .toList();
            byRef = filtered.stream()
                    .collect(Collectors.toMap(
                            item -> new ParticipationRef(item.participationType(), item.participationId()),
                            Function.identity(),
                            (left, right) -> left,
                            LinkedHashMap::new
                    ));
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Modo de seleção inválido.");
        }

        Map<ParticipationRef, String> excluded = new LinkedHashMap<>();
        for (ParticipationRef ref : refs) {
            ParticipationMonitoringResponse item = byRef.get(ref);
            if (item == null) {
                excluded.put(ref, "Participação não encontrada ou fora da empresa atual.");
                continue;
            }
            String reason = incompatibility(action, item, additionalDays, justification);
            if (reason != null) {
                excluded.put(ref, reason);
            }
        }
        return new SelectionEvaluation(refs, Map.copyOf(excluded));
    }

    private static List<ParticipationRef> normalizeExplicitSelection(List<ParticipationRef> selected) {
        if (selected == null || selected.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione ao menos uma participação.");
        }
        if (selected.size() > MAX_EXPLICIT_SELECTION) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A seleção explícita permite no máximo " + MAX_EXPLICIT_SELECTION + " itens."
            );
        }
        return selected.stream()
                .map(ref -> new ParticipationRef(normalizeLower(ref.type()), ref.id().trim()))
                .distinct()
                .toList();
    }

    private Map<ParticipationRef, ParticipationMonitoringResponse> findExplicitParticipations(
            BulkFilter filter,
            List<ParticipationRef> refs
    ) {
        Set<ParticipationRef> pending = new LinkedHashSet<>(refs);
        Map<ParticipationRef, ParticipationMonitoringResponse> found = new LinkedHashMap<>();
        int page = 0;
        int totalPages;
        do {
            ParticipationMonitoringPageResponse response = searchPage(page, filter);
            for (ParticipationMonitoringResponse item : response.items()) {
                ParticipationRef ref = new ParticipationRef(item.participationType(), item.participationId());
                if (pending.remove(ref)) {
                    found.put(ref, item);
                }
            }
            totalPages = response.totalPages();
            page++;
        } while (page < totalPages && !pending.isEmpty());
        return found;
    }

    private List<ParticipationMonitoringResponse> findFilteredParticipations(BulkFilter filter) {
        Map<ParticipationRef, ParticipationMonitoringResponse> found = new LinkedHashMap<>();
        int page = 0;
        int totalPages;
        do {
            ParticipationMonitoringPageResponse response = searchPage(page, filter);
            for (ParticipationMonitoringResponse item : response.items()) {
                if (!matchesFilter(item, filter)) {
                    continue;
                }
                ParticipationRef ref = new ParticipationRef(item.participationType(), item.participationId());
                found.putIfAbsent(ref, item);
                if (found.size() > MAX_FILTER_SELECTION) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "O filtro retorna mais de " + MAX_FILTER_SELECTION
                                    + " participações. Refine os filtros antes de criar o lote."
                    );
                }
            }
            totalPages = response.totalPages();
            page++;
        } while (page < totalPages);
        return new ArrayList<>(found.values());
    }

    private ParticipationMonitoringPageResponse searchPage(int page, BulkFilter filter) {
        String simulationId = filter == null ? null : filter.simulationId();
        String candidate = filter == null ? null : filter.candidate();
        return queryService.search(page, PAGE_SIZE, simulationId, candidate);
    }

    private static boolean matchesFilter(ParticipationMonitoringResponse item, BulkFilter filter) {
        if (filter == null) {
            return true;
        }
        String processStatus = normalizeLower(filter.processStatus());
        if (processStatus != null && !matchesProcessStatus(item, processStatus)) {
            return false;
        }
        String linkStatus = normalizeLower(filter.linkStatus());
        if (linkStatus != null && !linkStatus.equals(normalizeLower(item.linkStatus()))) {
            return false;
        }
        if (Boolean.TRUE.equals(filter.attention()) && !needsAttention(item)) {
            return false;
        }
        return true;
    }

    private static boolean matchesProcessStatus(ParticipationMonitoringResponse item, String filter) {
        String status = normalizeLower(item.status());
        return switch (filter) {
            case "waiting" -> "notstarted".equals(status);
            case "active" -> "inprogress".equals(status) && item.active();
            case "completed" -> "completed".equals(status);
            case "attention" -> needsAttention(item);
            default -> filter.equals(status);
        };
    }

    private static boolean needsAttention(ParticipationMonitoringResponse item) {
        String status = normalizeLower(item.status());
        String linkStatus = normalizeLower(item.linkStatus());
        return "abandoned".equals(status)
                || "expired".equals(status)
                || "expired".equals(linkStatus)
                || "canceled".equals(linkStatus)
                || ("inprogress".equals(status) && !item.active());
    }

    private static String incompatibility(
            String action,
            ParticipationMonitoringResponse item,
            Integer additionalDays,
            String justification
    ) {
        return switch (action) {
            case "RESEND" -> item.canResend() ? null : "O convite não pode ser reenviado no estado atual.";
            case "EXTEND" -> {
                if (additionalDays == null) {
                    yield "Informe a quantidade de dias para extensão.";
                }
                yield item.canExtend() ? null : "A validade não pode ser alterada no estado atual.";
            }
            case "CANCEL" -> {
                if (!"journey".equals(item.participationType())) {
                    yield "Cancelamento em lote é permitido somente para participações por jornada.";
                }
                if (trimToNull(justification) == null) {
                    yield "Cancelamento em lote exige justificativa.";
                }
                yield item.canCancel() ? null : "A participação não pode ser cancelada no estado atual.";
            }
            case "ADD_TAG", "REMOVE_TAG", "EXPORT" -> null;
            default -> "Ação não suportada.";
        };
    }

    private void validateActionPayload(
            String empresaId,
            String action,
            Integer additionalDays,
            String tagId,
            String justification
    ) {
        Set<String> actions = Set.of("RESEND", "EXTEND", "CANCEL", "ADD_TAG", "REMOVE_TAG", "EXPORT");
        if (!actions.contains(action)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ação de lote inválida.");
        }
        if ("EXTEND".equals(action) && (additionalDays == null || additionalDays < 1 || additionalDays > 365)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe de 1 a 365 dias para extensão.");
        }
        if ("CANCEL".equals(action) && trimToNull(justification) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cancelamento em lote exige justificativa.");
        }
        if (Set.of("ADD_TAG", "REMOVE_TAG").contains(action)) {
            if (trimToNull(tagId) == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione uma tag.");
            }
            tagRepository.findByEmpresaIdAndId(empresaId, tagId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tag não encontrada."));
        }
    }

    private BulkJobResponse response(ParticipationBulkJobEntity job, boolean includeItems) {
        List<BulkItemResponse> items = includeItems
                ? itemRepository.findByJobIdOrderByIdAsc(job.getId()).stream()
                        .map(item -> new BulkItemResponse(
                                item.getParticipationType(),
                                item.getParticipationId(),
                                item.getStatus(),
                                item.getReason(),
                                item.getProcessedAt()
                        ))
                        .toList()
                : List.of();
        int progress = job.getTotalItems() == 0
                ? 100
                : Math.min(100, Math.round(job.getProcessedItems() * 100f / job.getTotalItems()));
        return new BulkJobResponse(
                job.getId(),
                job.getAction(),
                job.getSelectionMode(),
                job.getStatus(),
                job.getTotalItems(),
                job.getProcessedItems(),
                job.getSucceededItems(),
                job.getSkippedItems(),
                job.getFailedItems(),
                progress,
                job.getJustification(),
                job.getCreatedAt(),
                job.getStartedAt(),
                job.getCompletedAt(),
                items
        );
    }

    private ParticipationBulkJobEntity required(String empresaId, String jobId) {
        return jobRepository.findByEmpresaIdAndId(empresaId, jobId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Lote não encontrado."));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        } catch (JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não foi possível registrar o lote.", exception);
        }
    }

    private static String impact(String action, int eligibleCount, Integer additionalDays) {
        return switch (action) {
            case "RESEND" -> eligibleCount + " convite(s) serão reenviados sem criar novas participações.";
            case "EXTEND" -> eligibleCount + " participação(ões) receberão " + additionalDays + " dia(s) adicionais.";
            case "CANCEL" -> eligibleCount + " participação(ões) por jornada serão canceladas.";
            case "ADD_TAG" -> eligibleCount + " participação(ões) receberão a tag selecionada.";
            case "REMOVE_TAG" -> eligibleCount + " participação(ões) terão a tag removida.";
            case "EXPORT" -> eligibleCount + " participação(ões) serão incluídas no relatório minimizado.";
            default -> eligibleCount + " participação(ões) serão processadas.";
        };
    }

    private static String csv(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private static String normalizeUpper(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeLower(String value) {
        String normalized = trimToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private record SelectionEvaluation(
            List<ParticipationRef> refs,
            Map<ParticipationRef, String> excludedReasons
    ) {
        private int eligibleCount() {
            return refs.size() - excludedReasons.size();
        }
    }
}
