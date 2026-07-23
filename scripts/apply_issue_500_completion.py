from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def write(path: str, content: str) -> None:
    target = ROOT / path
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(content, encoding="utf-8")


def replace_once(path: str, old: str, new: str) -> None:
    content = read(path)
    if old not in content:
        raise RuntimeError(f"Padrão não encontrado em {path}: {old[:120]!r}")
    write(path, content.replace(old, new, 1))


def replace_all(path: str, old: str, new: str) -> None:
    content = read(path)
    if old not in content:
        raise RuntimeError(f"Padrão não encontrado em {path}: {old[:120]!r}")
    write(path, content.replace(old, new))


# Telemetria técnica de reprodução.
replace_once(
    "backend/src/main/java/br/com/iforce/praxis/integrity/model/IntegrityEventType.java",
    "    STIMULUS_STARTED,\n    RESPONSE_SELECTED,",
    "    STIMULUS_STARTED,\n"
    "    VIDEO_PLAYBACK_STARTED,\n"
    "    VIDEO_PLAYBACK_PAUSED,\n"
    "    VIDEO_PLAYBACK_COMPLETED,\n"
    "    VIDEO_PLAYBACK_ERROR,\n"
    "    RESPONSE_SELECTED,",
)

for service in [
    "backend/src/main/java/br/com/iforce/praxis/integrity/service/CandidateIntegrityService.java",
    "backend/src/main/java/br/com/iforce/praxis/integrity/service/AtomicCandidateIntegrityService.java",
]:
    replace_once(service, 'Set.of("IMAGE", "AUDIO", "OTHER")', 'Set.of("IMAGE", "AUDIO", "VIDEO", "OTHER")')
    replace_once(
        service,
        "            IntegrityEventType.STIMULUS_STARTED,\n            IntegrityEventType.RESPONSE_SELECTED,",
        "            IntegrityEventType.STIMULUS_STARTED,\n"
        "            IntegrityEventType.VIDEO_PLAYBACK_STARTED,\n"
        "            IntegrityEventType.VIDEO_PLAYBACK_PAUSED,\n"
        "            IntegrityEventType.VIDEO_PLAYBACK_COMPLETED,\n"
        "            IntegrityEventType.VIDEO_PLAYBACK_ERROR,\n"
        "            IntegrityEventType.RESPONSE_SELECTED,",
    )
    replace_once(
        service,
        "        if (eventType != IntegrityEventType.ASSET_LOADED && eventType != IntegrityEventType.STIMULUS_STARTED) {",
        "        if (eventType != IntegrityEventType.ASSET_LOADED\n"
        "                && eventType != IntegrityEventType.STIMULUS_STARTED\n"
        "                && eventType != IntegrityEventType.VIDEO_PLAYBACK_STARTED\n"
        "                && eventType != IntegrityEventType.VIDEO_PLAYBACK_PAUSED\n"
        "                && eventType != IntegrityEventType.VIDEO_PLAYBACK_COMPLETED\n"
        "                && eventType != IntegrityEventType.VIDEO_PLAYBACK_ERROR) {",
    )

replace_once(
    "frontend/src/lib/api/candidate-integrity.ts",
    '  | "STIMULUS_STARTED"\n  | "RESPONSE_SELECTED"',
    '  | "STIMULUS_STARTED"\n'
    '  | "VIDEO_PLAYBACK_STARTED"\n'
    '  | "VIDEO_PLAYBACK_PAUSED"\n'
    '  | "VIDEO_PLAYBACK_COMPLETED"\n'
    '  | "VIDEO_PLAYBACK_ERROR"\n'
    '  | "RESPONSE_SELECTED"',
)
replace_once(
    "frontend/src/lib/api/candidate-integrity.ts",
    '  detail?: "IMAGE" | "AUDIO" | "OTHER" | null;',
    '  detail?: "IMAGE" | "AUDIO" | "VIDEO" | "OTHER" | null;',
)

boundary = "frontend/src/components/candidate-integrity-boundary.tsx"
replace_once(
    boundary,
    '      detail?: "IMAGE" | "AUDIO" | "OTHER",',
    '      detail?: "IMAGE" | "AUDIO" | "VIDEO" | "OTHER",',
)
replace_once(
    boundary,
    '      if (target instanceof HTMLImageElement) record("ASSET_LOADED", "IMAGE");\n'
    '      if (target instanceof HTMLAudioElement) record("ASSET_LOADED", "AUDIO");',
    '      if (target instanceof HTMLImageElement) record("ASSET_LOADED", "IMAGE");\n'
    '      if (target instanceof HTMLAudioElement) record("ASSET_LOADED", "AUDIO");\n'
    '      if (target instanceof HTMLVideoElement) record("ASSET_LOADED", "VIDEO");',
)
replace_once(
    boundary,
    '    const handleStimulusStarted = (event: Event) => {\n'
    '      if (event.target instanceof HTMLAudioElement) record("STIMULUS_STARTED", "AUDIO");\n'
    '    };',
    '    const handleStimulusStarted = (event: Event) => {\n'
    '      if (event.target instanceof HTMLAudioElement) record("STIMULUS_STARTED", "AUDIO");\n'
    '      if (event.target instanceof HTMLVideoElement) {\n'
    '        record("STIMULUS_STARTED", "VIDEO");\n'
    '        record("VIDEO_PLAYBACK_STARTED", "VIDEO");\n'
    '      }\n'
    '    };\n'
    '    const handleVideoPaused = (event: Event) => {\n'
    '      if (event.target instanceof HTMLVideoElement && !event.target.ended) {\n'
    '        record("VIDEO_PLAYBACK_PAUSED", "VIDEO");\n'
    '      }\n'
    '    };\n'
    '    const handleVideoCompleted = (event: Event) => {\n'
    '      if (event.target instanceof HTMLVideoElement) record("VIDEO_PLAYBACK_COMPLETED", "VIDEO");\n'
    '    };\n'
    '    const handleVideoError = (event: Event) => {\n'
    '      if (event.target instanceof HTMLVideoElement) record("VIDEO_PLAYBACK_ERROR", "VIDEO");\n'
    '    };',
)
replace_once(
    boundary,
    '        window.addEventListener("play", handleStimulusStarted, true);\n'
    '        window.addEventListener("click", handleCandidateClick, true);',
    '        window.addEventListener("play", handleStimulusStarted, true);\n'
    '        window.addEventListener("pause", handleVideoPaused, true);\n'
    '        window.addEventListener("ended", handleVideoCompleted, true);\n'
    '        window.addEventListener("error", handleVideoError, true);\n'
    '        window.addEventListener("click", handleCandidateClick, true);',
)
replace_once(
    boundary,
    '      window.removeEventListener("play", handleStimulusStarted, true);\n'
    '      window.removeEventListener("click", handleCandidateClick, true);',
    '      window.removeEventListener("play", handleStimulusStarted, true);\n'
    '      window.removeEventListener("pause", handleVideoPaused, true);\n'
    '      window.removeEventListener("ended", handleVideoCompleted, true);\n'
    '      window.removeEventListener("error", handleVideoError, true);\n'
    '      window.removeEventListener("click", handleCandidateClick, true);',
)

# Validação de duração antes de enviar o vídeo ao storage. Upload concluído confirma disponibilidade do objeto.
editor = "frontend/src/routes/nova.dialogo.tsx"
replace_once(
    editor,
    'function MediaAttachment({',
    'const MAX_VIDEO_DURATION_SECONDS = 10 * 60;\n\nfunction MediaAttachment({',
)
replace_once(
    editor,
    '    setError(null);\n    setUploading(true);',
    '    if (file.type.startsWith("video/")) {\n'
    '      try {\n'
    '        const durationSeconds = await readVideoDuration(file);\n'
    '        if (durationSeconds > MAX_VIDEO_DURATION_SECONDS) {\n'
    '          setError("O vídeo deve ter no máximo 10 minutos.");\n'
    '          return;\n'
    '        }\n'
    '      } catch {\n'
    '        setError("Não foi possível validar a duração do vídeo.");\n'
    '        return;\n'
    '      }\n'
    '    }\n'
    '    setError(null);\n    setUploading(true);',
)
replace_once(
    editor,
    'function MediaPreview({ mediaUrl, mediaType }: { mediaUrl: string; mediaType: MediaType | null }) {',
    'function readVideoDuration(file: File): Promise<number> {\n'
    '  return new Promise((resolve, reject) => {\n'
    '    const objectUrl = URL.createObjectURL(file);\n'
    '    const video = document.createElement("video");\n'
    '    video.preload = "metadata";\n'
    '    video.onloadedmetadata = () => {\n'
    '      const duration = video.duration;\n'
    '      URL.revokeObjectURL(objectUrl);\n'
    '      video.remove();\n'
    '      Number.isFinite(duration) && duration > 0 ? resolve(duration) : reject(new Error("Duração inválida"));\n'
    '    };\n'
    '    video.onerror = () => {\n'
    '      URL.revokeObjectURL(objectUrl);\n'
    '      video.remove();\n'
    '      reject(new Error("Metadados indisponíveis"));\n'
    '    };\n'
    '    video.src = objectUrl;\n'
    '  });\n'
    '}\n\n'
    'function MediaPreview({ mediaUrl, mediaType }: { mediaUrl: string; mediaType: MediaType | null }) {',
)

# DTO analítico agrupado por formato e versão, sem misturar amostras incompatíveis.
write(
    "backend/src/main/java/br/com/iforce/praxis/dashboard/dto/DashboardAnalyticsResponse.java",
    '''package br.com.iforce.praxis.dashboard.dto;

import br.com.iforce.praxis.shared.model.MediaType;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Indicadores analíticos usados exclusivamente pelo dashboard da empresa.
 */
public record DashboardAnalyticsResponse(
        Instant generatedAt,
        int periodDays,
        ParticipationSummary participations,
        List<ActivityPoint> activity,
        List<MediaQualityComparison> mediaQualityComparisons
) {

    public record ParticipationSummary(
            long total,
            long started,
            long notStarted,
            long inProgress,
            long completed,
            long abandoned,
            long expired,
            double completionRatePercent,
            double dropOffRatePercent,
            Double averageScoreLast30Days
    ) {
    }

    public record ActivityPoint(
            LocalDate date,
            long created,
            long completed,
            long abandoned
    ) {
    }

    /**
     * Comparação observada para uma versão exata de mídia. Versões diferentes nunca são agregadas.
     */
    public record MediaQualityComparison(
            MediaType mediaType,
            String mediaVersion,
            long sampleSize,
            long completed,
            double completionRatePercent,
            Double averageDurationSeconds,
            List<ResponseDistribution> responseDistribution
    ) {
    }

    public record ResponseDistribution(
            String responseId,
            long count,
            double percentage
    ) {
    }
}
''',
)

write(
    "backend/src/main/java/br/com/iforce/praxis/dashboard/service/DashboardAnalyticsService.java",
    '''package br.com.iforce.praxis.dashboard.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.dashboard.dto.DashboardAnalyticsResponse;
import br.com.iforce.praxis.dashboard.dto.DashboardAnalyticsResponse.ActivityPoint;
import br.com.iforce.praxis.dashboard.dto.DashboardAnalyticsResponse.MediaQualityComparison;
import br.com.iforce.praxis.dashboard.dto.DashboardAnalyticsResponse.ParticipationSummary;
import br.com.iforce.praxis.dashboard.dto.DashboardAnalyticsResponse.ResponseDistribution;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.AttemptAnswerEntity;
import br.com.iforce.praxis.gupy.persistence.entity.AttemptNodeServeEntity;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.model.MediaType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Calcula os indicadores visuais do dashboard sem expor registros identificáveis de candidatos.
 */
@Service
public class DashboardAnalyticsService {

    private static final int PERIOD_DAYS = 30;
    private static final ZoneId DASHBOARD_ZONE = ZoneOffset.UTC;
    private static final String UNVERSIONED_MEDIA = "SEM_VERSAO";

    private final CurrentEmpresaService currentEmpresaService;
    private final CandidateAttemptRepository candidateAttemptRepository;

    public DashboardAnalyticsService(
            CurrentEmpresaService currentEmpresaService,
            CandidateAttemptRepository candidateAttemptRepository
    ) {
        this.currentEmpresaService = currentEmpresaService;
        this.candidateAttemptRepository = candidateAttemptRepository;
    }

    @Transactional(readOnly = true)
    public DashboardAnalyticsResponse getAnalytics() {
        String empresaId = currentEmpresaService.requiredEmpresaId();
        Instant generatedAt = Instant.now();
        LocalDate today = LocalDate.ofInstant(generatedAt, DASHBOARD_ZONE);
        LocalDate firstDate = today.minusDays(PERIOD_DAYS - 1L);
        Instant periodStart = firstDate.atStartOfDay(DASHBOARD_ZONE).toInstant();

        Specification<CandidateAttemptEntity> companyScope = belongsToEmpresa(empresaId);

        long total = candidateAttemptRepository.count(companyScope);
        long notStarted = candidateAttemptRepository.countByEmpresaIdAndStatus(empresaId, AttemptStatus.NOT_STARTED);
        long inProgress = candidateAttemptRepository.countByEmpresaIdAndStatus(empresaId, AttemptStatus.IN_PROGRESS);
        long completed = candidateAttemptRepository.countByEmpresaIdAndStatus(empresaId, AttemptStatus.COMPLETED);
        long abandoned = candidateAttemptRepository.countByEmpresaIdAndStatus(empresaId, AttemptStatus.ABANDONED);
        long expired = candidateAttemptRepository.countByEmpresaIdAndStatus(empresaId, AttemptStatus.EXPIRED);

        long startedFromTimestamp = candidateAttemptRepository.count(companyScope.and(startedAtIsPresent()));
        long started = Math.max(startedFromTimestamp, completed + abandoned);
        long resolved = completed + abandoned + expired;

        List<CandidateAttemptEntity> createdInPeriod = candidateAttemptRepository.findAll(
                companyScope.and(createdAtOnOrAfter(periodStart)),
                Sort.by(Sort.Direction.ASC, "createdAt")
        );
        List<CandidateAttemptEntity> finishedInPeriod = candidateAttemptRepository.findAll(
                companyScope.and(finishedAtOnOrAfter(periodStart)),
                Sort.by(Sort.Direction.ASC, "finishedAt")
        );

        Map<LocalDate, ActivityAccumulator> activityByDate = initializeActivity(firstDate);
        createdInPeriod.forEach(attempt -> incrementCreated(activityByDate, attempt));
        finishedInPeriod.forEach(attempt -> incrementFinished(activityByDate, attempt));

        ParticipationSummary participations = new ParticipationSummary(
                total,
                started,
                notStarted,
                inProgress,
                completed,
                abandoned,
                expired,
                percentage(completed, resolved),
                percentage(abandoned + expired, resolved),
                averageCompletedScore(finishedInPeriod)
        );

        List<ActivityPoint> activity = activityByDate.entrySet().stream()
                .map(entry -> new ActivityPoint(
                        entry.getKey(),
                        entry.getValue().created,
                        entry.getValue().completed,
                        entry.getValue().abandoned
                ))
                .toList();

        return new DashboardAnalyticsResponse(
                generatedAt,
                PERIOD_DAYS,
                participations,
                activity,
                mediaQuality(combineAttempts(createdInPeriod, finishedInPeriod))
        );
    }

    private List<MediaQualityComparison> mediaQuality(List<CandidateAttemptEntity> attempts) {
        Map<String, MediaAccumulator> groups = new LinkedHashMap<>();
        for (CandidateAttemptEntity attempt : attempts) {
            Map<String, AttemptAnswerEntity> answersByNode = new LinkedHashMap<>();
            for (AttemptAnswerEntity answer : attempt.getAnswers()) {
                answersByNode.put(answer.getNodeId(), answer);
            }
            for (AttemptNodeServeEntity serve : attempt.getNodeServes()) {
                if (serve.getMediaType() == null) {
                    continue;
                }
                String version = normalizeVersion(serve.getMediaVersion());
                String key = serve.getMediaType().name() + "|" + version;
                MediaAccumulator accumulator = groups.computeIfAbsent(
                        key,
                        ignored -> new MediaAccumulator(serve.getMediaType(), version)
                );
                accumulator.attemptIds.add(attempt.getId());
                if (attempt.getStatus() == AttemptStatus.COMPLETED) {
                    accumulator.completedAttemptIds.add(attempt.getId());
                }
                accumulator.addDuration(attempt);

                AttemptAnswerEntity answer = answersByNode.get(serve.getNodeId());
                if (answer != null) {
                    String responseId = answer.isTimedOut() || answer.getOptionId() == null
                            ? "TEMPO_ESGOTADO"
                            : answer.getOptionId();
                    accumulator.responses.merge(responseId, 1L, Long::sum);
                }
            }
        }

        return groups.values().stream()
                .sorted(Comparator.comparing((MediaAccumulator item) -> item.mediaType.name())
                        .thenComparing(item -> item.mediaVersion))
                .map(MediaAccumulator::toResponse)
                .toList();
    }

    private List<CandidateAttemptEntity> combineAttempts(
            List<CandidateAttemptEntity> created,
            List<CandidateAttemptEntity> finished
    ) {
        Map<String, CandidateAttemptEntity> unique = new LinkedHashMap<>();
        created.forEach(attempt -> unique.put(attempt.getId(), attempt));
        finished.forEach(attempt -> unique.put(attempt.getId(), attempt));
        return new ArrayList<>(unique.values());
    }

    private String normalizeVersion(String version) {
        return version == null || version.isBlank() ? UNVERSIONED_MEDIA : version.trim();
    }

    private static Specification<CandidateAttemptEntity> belongsToEmpresa(String empresaId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("empresaId"), empresaId);
    }

    private static Specification<CandidateAttemptEntity> startedAtIsPresent() {
        return (root, query, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get("startedAt"));
    }

    private static Specification<CandidateAttemptEntity> createdAtOnOrAfter(Instant periodStart) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(
                root.<Instant>get("createdAt"), periodStart
        );
    }

    private static Specification<CandidateAttemptEntity> finishedAtOnOrAfter(Instant periodStart) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(
                root.<Instant>get("finishedAt"), periodStart
        );
    }

    private static Map<LocalDate, ActivityAccumulator> initializeActivity(LocalDate firstDate) {
        Map<LocalDate, ActivityAccumulator> activity = new LinkedHashMap<>();
        for (int offset = 0; offset < PERIOD_DAYS; offset++) {
            activity.put(firstDate.plusDays(offset), new ActivityAccumulator());
        }
        return activity;
    }

    private static void incrementCreated(Map<LocalDate, ActivityAccumulator> activityByDate, CandidateAttemptEntity attempt) {
        LocalDate date = LocalDate.ofInstant(attempt.getCreatedAt(), DASHBOARD_ZONE);
        ActivityAccumulator accumulator = activityByDate.get(date);
        if (accumulator != null) {
            accumulator.created++;
        }
    }

    private static void incrementFinished(Map<LocalDate, ActivityAccumulator> activityByDate, CandidateAttemptEntity attempt) {
        if (attempt.getFinishedAt() == null) {
            return;
        }
        LocalDate date = LocalDate.ofInstant(attempt.getFinishedAt(), DASHBOARD_ZONE);
        ActivityAccumulator accumulator = activityByDate.get(date);
        if (accumulator == null) {
            return;
        }
        if (attempt.getStatus() == AttemptStatus.COMPLETED) {
            accumulator.completed++;
        } else if (attempt.getStatus() == AttemptStatus.ABANDONED || attempt.getStatus() == AttemptStatus.EXPIRED) {
            accumulator.abandoned++;
        }
    }

    private static Double averageCompletedScore(List<CandidateAttemptEntity> attempts) {
        return attempts.stream()
                .filter(attempt -> attempt.getStatus() == AttemptStatus.COMPLETED)
                .map(CandidateAttemptEntity::getScore)
                .filter(score -> score != null)
                .mapToInt(Integer::intValue)
                .average()
                .stream()
                .boxed()
                .findFirst()
                .orElse(null);
    }

    private static double percentage(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0.0;
        }
        return Math.round((numerator * 1000.0) / denominator) / 10.0;
    }

    private static final class ActivityAccumulator {
        private long created;
        private long completed;
        private long abandoned;
    }

    private static final class MediaAccumulator {
        private final MediaType mediaType;
        private final String mediaVersion;
        private final Set<String> attemptIds = new LinkedHashSet<>();
        private final Set<String> completedAttemptIds = new LinkedHashSet<>();
        private final Set<String> durationAttemptIds = new LinkedHashSet<>();
        private final Map<String, Long> responses = new LinkedHashMap<>();
        private long durationSeconds;

        private MediaAccumulator(MediaType mediaType, String mediaVersion) {
            this.mediaType = mediaType;
            this.mediaVersion = mediaVersion;
        }

        private void addDuration(CandidateAttemptEntity attempt) {
            if (!durationAttemptIds.add(attempt.getId())
                    || attempt.getStartedAt() == null
                    || attempt.getFinishedAt() == null) {
                return;
            }
            long seconds = Duration.between(attempt.getStartedAt(), attempt.getFinishedAt()).getSeconds();
            durationSeconds += Math.max(0, seconds);
        }

        private MediaQualityComparison toResponse() {
            long responseTotal = responses.values().stream().mapToLong(Long::longValue).sum();
            List<ResponseDistribution> distribution = responses.entrySet().stream()
                    .map(entry -> new ResponseDistribution(
                            entry.getKey(),
                            entry.getValue(),
                            percentage(entry.getValue(), responseTotal)
                    ))
                    .sorted(Comparator.comparingLong(ResponseDistribution::count).reversed()
                            .thenComparing(ResponseDistribution::responseId))
                    .toList();
            Double averageDuration = durationAttemptIds.isEmpty()
                    ? null
                    : Math.round((durationSeconds * 10.0) / durationAttemptIds.size()) / 10.0;
            return new MediaQualityComparison(
                    mediaType,
                    mediaVersion,
                    attemptIds.size(),
                    completedAttemptIds.size(),
                    percentage(completedAttemptIds.size(), attemptIds.size()),
                    averageDuration,
                    distribution
            );
        }
    }
}
''',
)

write(
    "frontend/src/lib/api/dashboard-analytics.ts",
    '''import { apiRequest } from "@/lib/api/http";
import { PraxisApiError } from "@/lib/api/praxis-legacy";

export interface DashboardParticipationSummary {
  total: number;
  started: number;
  notStarted: number;
  inProgress: number;
  completed: number;
  abandoned: number;
  expired: number;
  completionRatePercent: number;
  dropOffRatePercent: number;
  averageScoreLast30Days: number | null;
}

export interface DashboardActivityPoint {
  date: string;
  created: number;
  completed: number;
  abandoned: number;
}

export interface DashboardResponseDistribution {
  responseId: string;
  count: number;
  percentage: number;
}

export interface DashboardMediaQualityComparison {
  mediaType: "IMAGE" | "AUDIO" | "VIDEO";
  mediaVersion: string;
  sampleSize: number;
  completed: number;
  completionRatePercent: number;
  averageDurationSeconds: number | null;
  responseDistribution: DashboardResponseDistribution[];
}

export interface DashboardAnalyticsResponse {
  generatedAt: string;
  periodDays: number;
  participations: DashboardParticipationSummary;
  activity: DashboardActivityPoint[];
  mediaQualityComparisons: DashboardMediaQualityComparison[];
}

export async function getDashboardAnalytics(): Promise<DashboardAnalyticsResponse | null> {
  try {
    return await apiRequest<DashboardAnalyticsResponse>("/api/v1/dashboard/analytics");
  } catch (error) {
    if (error instanceof PraxisApiError && error.status === 404) {
      return null;
    }
    throw error;
  }
}
''',
)

dashboard = "frontend/src/routes/dashboard.tsx"
replace_once(
    dashboard,
    "  type DashboardAnalyticsResponse,\n  type DashboardParticipationSummary,",
    "  type DashboardAnalyticsResponse,\n  type DashboardMediaQualityComparison,\n  type DashboardParticipationSummary,",
)
replace_once(
    dashboard,
    "      <PeriodQuality summary={analytics.participations} />\n    </section>",
    "      <PeriodQuality summary={analytics.participations} />\n"
    "      <MediaQualityTable comparisons={analytics.mediaQualityComparisons} />\n"
    "    </section>",
)
replace_once(
    dashboard,
    "function ActivityChart({ activity }: { activity: DashboardActivityPoint[] }) {",
    '''function MediaQualityTable({ comparisons }: { comparisons: DashboardMediaQualityComparison[] }) {
  return (
    <section className="rounded-md border border-border bg-card p-4 xl:col-span-2" aria-labelledby="media-quality-title">
      <h2 id="media-quality-title" className="text-lg font-semibold text-foreground">Qualidade por formato e versão de mídia</h2>
      <p className="mt-1 text-xs text-muted-foreground">
        Dados observados nos últimos 30 dias. Cada versão é analisada separadamente para evitar misturar estímulos incompatíveis.
      </p>
      {comparisons.length === 0 ? (
        <p className="mt-4 text-sm text-muted-foreground">Ainda não há amostra de mídia versionada no período.</p>
      ) : (
        <div className="mt-4 overflow-x-auto">
          <table className="w-full min-w-[760px] text-left text-sm">
            <thead className="border-b border-border text-xs uppercase text-muted-foreground">
              <tr>
                <th className="px-3 py-2">Formato</th>
                <th className="px-3 py-2">Versão</th>
                <th className="px-3 py-2 text-right">Amostra</th>
                <th className="px-3 py-2 text-right">Conclusão</th>
                <th className="px-3 py-2 text-right">Tempo médio</th>
                <th className="px-3 py-2">Respostas observadas</th>
              </tr>
            </thead>
            <tbody>
              {comparisons.map((item) => (
                <tr key={`${item.mediaType}-${item.mediaVersion}`} className="border-b border-border/70 last:border-0">
                  <td className="px-3 py-3 font-medium">{formatMediaType(item.mediaType)}</td>
                  <td className="px-3 py-3 font-mono text-xs">{item.mediaVersion}</td>
                  <td className="px-3 py-3 text-right tabular-nums">{item.sampleSize}</td>
                  <td className="px-3 py-3 text-right tabular-nums">
                    {item.completed}/{item.sampleSize} · {formatPercent(item.completionRatePercent)}
                  </td>
                  <td className="px-3 py-3 text-right tabular-nums">{formatDuration(item.averageDurationSeconds)}</td>
                  <td className="px-3 py-3 text-xs text-muted-foreground">
                    {item.responseDistribution.length === 0
                      ? "Sem respostas"
                      : item.responseDistribution.map((response) => `${response.responseId}: ${formatPercent(response.percentage)}`).join(" · ")}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </section>
  );
}

function formatMediaType(mediaType: DashboardMediaQualityComparison["mediaType"]): string {
  if (mediaType === "VIDEO") return "Vídeo";
  if (mediaType === "AUDIO") return "Áudio";
  return "Imagem";
}

function formatDuration(seconds: number | null): string {
  if (seconds === null) return "—";
  if (seconds < 60) return `${Math.round(seconds)}s`;
  return `${Math.floor(seconds / 60)}min ${Math.round(seconds % 60)}s`;
}

function ActivityChart({ activity }: { activity: DashboardActivityPoint[] }) {''',
)

# Manual contextual completo da tela alterada.
manual = "frontend/src/lib/screen-manual-overrides.ts"
replace_once(
    manual,
    "export const SCREEN_MANUAL_OVERRIDES: ScreenManualDefinition[] = [",
    '''export const SCREEN_MANUAL_OVERRIDES: ScreenManualDefinition[] = [
  {
    id: "dashboard-qualidade-midia",
    title: "Dashboard operacional e qualidade de mídia",
    purpose: "Acompanhar a operação e comparar resultados observados por formato e versão de mídia sem misturar amostras incompatíveis.",
    flow: ["Revise os indicadores principais.", "Observe movimentação, situação e funil.", "Compare formato e versão na tabela de qualidade.", "Investigue diferenças relevantes antes de considerar formatos equivalentes."],
    fields: [
      { name: "Formato", description: "Imagem, áudio ou vídeo efetivamente apresentado." },
      { name: "Versão", description: "Identificador imutável usado para separar conteúdos incompatíveis." },
      { name: "Amostra", description: "Quantidade de tentativas distintas expostas à versão." },
      { name: "Conclusão", description: "Tentativas concluídas e percentual observado." },
      { name: "Tempo médio", description: "Duração média entre início e conclusão para a amostra observável." },
      { name: "Respostas", description: "Distribuição das alternativas escolhidas nos nós associados à mídia." },
    ],
    permissions: ["Perfil EMPRESA com acesso aos indicadores agregados da própria organização."],
    states: ["Carregando", "Sem amostra", "Com dados", "Indicadores indisponíveis", "Erro"],
    blocks: ["Usuário sem acesso à empresa.", "Falha ao carregar analytics.", "Amostra sem versão de mídia.", "Dados insuficientes para interpretar diferenças."],
    examples: ["Comparar vídeo v2 com áudio v1 sem combinar versões.", "Detectar aumento de abandono após uma mudança de estímulo."],
    shortcuts: ["Use Atualizar para recalcular os dados.", "Use Tab para navegar pela tabela.", "Não trate diferença observada como causalidade sem amostra e validação.", "Consulte o processo completo na Central de manuais."],
    matches: (pathname) => pathname === "/dashboard",
  },''',
)

# Teste backend do agrupamento por mídia.
write(
    "backend/src/test/java/br/com/iforce/praxis/dashboard/service/DashboardAnalyticsServiceTest.java",
    '''package br.com.iforce.praxis.dashboard.service;

import br.com.iforce.praxis.auth.service.CurrentEmpresaService;
import br.com.iforce.praxis.dashboard.dto.DashboardAnalyticsResponse;
import br.com.iforce.praxis.gupy.model.AttemptStatus;
import br.com.iforce.praxis.gupy.persistence.entity.AttemptAnswerEntity;
import br.com.iforce.praxis.gupy.persistence.entity.AttemptNodeServeEntity;
import br.com.iforce.praxis.gupy.persistence.entity.CandidateAttemptEntity;
import br.com.iforce.praxis.gupy.persistence.repository.CandidateAttemptRepository;
import br.com.iforce.praxis.shared.model.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardAnalyticsServiceTest {

    @Mock
    private CurrentEmpresaService currentEmpresaService;
    @Mock
    private CandidateAttemptRepository candidateAttemptRepository;

    private DashboardAnalyticsService service;

    @BeforeEach
    void setUp() {
        service = new DashboardAnalyticsService(currentEmpresaService, candidateAttemptRepository);
    }

    @Test
    @SuppressWarnings("unchecked")
    void getAnalyticsBuildsFunnelRatesActivityAndMediaQualityWithoutMixingVersions() {
        Instant yesterday = Instant.now().minus(1, ChronoUnit.DAYS);
        CandidateAttemptEntity created = attempt("created", AttemptStatus.IN_PROGRESS, yesterday, null, null);
        CandidateAttemptEntity completed = attempt(
                "completed",
                AttemptStatus.COMPLETED,
                yesterday.minusSeconds(60),
                yesterday,
                80
        );
        CandidateAttemptEntity abandoned = attempt("abandoned", AttemptStatus.ABANDONED, yesterday, yesterday, null);
        addMedia(created, "node-1", MediaType.VIDEO, "video-v1", null);
        addMedia(completed, "node-1", MediaType.VIDEO, "video-v1", "A");
        addMedia(abandoned, "node-2", MediaType.VIDEO, "video-v2", "B");

        when(currentEmpresaService.requiredEmpresaId()).thenReturn("empresa-1");
        when(candidateAttemptRepository.count(any(Specification.class))).thenReturn(10L, 8L);
        when(candidateAttemptRepository.countByEmpresaIdAndStatus("empresa-1", AttemptStatus.NOT_STARTED)).thenReturn(2L);
        when(candidateAttemptRepository.countByEmpresaIdAndStatus("empresa-1", AttemptStatus.IN_PROGRESS)).thenReturn(2L);
        when(candidateAttemptRepository.countByEmpresaIdAndStatus("empresa-1", AttemptStatus.COMPLETED)).thenReturn(4L);
        when(candidateAttemptRepository.countByEmpresaIdAndStatus("empresa-1", AttemptStatus.ABANDONED)).thenReturn(1L);
        when(candidateAttemptRepository.countByEmpresaIdAndStatus("empresa-1", AttemptStatus.EXPIRED)).thenReturn(1L);
        when(candidateAttemptRepository.findAll(any(Specification.class), any(Sort.class)))
                .thenReturn(List.of(created), List.of(completed, abandoned));

        DashboardAnalyticsResponse response = service.getAnalytics();

        assertThat(response.periodDays()).isEqualTo(30);
        assertThat(response.activity()).hasSize(30);
        assertThat(response.participations().total()).isEqualTo(10);
        assertThat(response.participations().started()).isEqualTo(8);
        assertThat(response.participations().completionRatePercent()).isEqualTo(66.7);
        assertThat(response.participations().dropOffRatePercent()).isEqualTo(33.3);
        assertThat(response.participations().averageScoreLast30Days()).isEqualTo(80.0);
        assertThat(response.mediaQualityComparisons()).hasSize(2);
        DashboardAnalyticsResponse.MediaQualityComparison videoV1 = response.mediaQualityComparisons().getFirst();
        assertThat(videoV1.mediaVersion()).isEqualTo("video-v1");
        assertThat(videoV1.sampleSize()).isEqualTo(2);
        assertThat(videoV1.completed()).isEqualTo(1);
        assertThat(videoV1.completionRatePercent()).isEqualTo(50.0);
        assertThat(videoV1.averageDurationSeconds()).isEqualTo(60.0);
        assertThat(videoV1.responseDistribution()).singleElement().satisfies(item -> {
            assertThat(item.responseId()).isEqualTo("A");
            assertThat(item.percentage()).isEqualTo(100.0);
        });
    }

    private static CandidateAttemptEntity attempt(
            String id,
            AttemptStatus status,
            Instant createdAt,
            Instant finishedAt,
            Integer score
    ) {
        CandidateAttemptEntity attempt = new CandidateAttemptEntity();
        attempt.setId(id);
        attempt.setEmpresaId("empresa-1");
        attempt.setStatus(status);
        attempt.setCreatedAt(createdAt);
        attempt.setStartedAt(createdAt);
        attempt.setFinishedAt(finishedAt);
        attempt.setScore(score);
        return attempt;
    }

    private static void addMedia(
            CandidateAttemptEntity attempt,
            String nodeId,
            MediaType mediaType,
            String mediaVersion,
            String optionId
    ) {
        AttemptNodeServeEntity serve = new AttemptNodeServeEntity();
        serve.setCandidateAttempt(attempt);
        serve.setNodeId(nodeId);
        serve.setServedAt(attempt.getCreatedAt());
        serve.setMediaType(mediaType);
        serve.setMediaVersion(mediaVersion);
        attempt.getNodeServes().add(serve);

        if (optionId != null) {
            AttemptAnswerEntity answer = new AttemptAnswerEntity();
            answer.setCandidateAttempt(attempt);
            answer.setNodeId(nodeId);
            answer.setOptionId(optionId);
            answer.setTimedOut(false);
            answer.setAnsweredAt(attempt.getCreatedAt());
            answer.setReceivedAt(attempt.getCreatedAt());
            attempt.getAnswers().add(answer);
        }
    }
}
''',
)

# Amplia o teste de contrato já existente.
test_path = "frontend/scripts/test-accessible-video.mjs"
replace_once(
    test_path,
    "const editor = fs.readFileSync(new URL('../src/routes/nova.dialogo.tsx', import.meta.url), 'utf8');",
    "const editor = fs.readFileSync(new URL('../src/routes/nova.dialogo.tsx', import.meta.url), 'utf8');\n"
    "const integrity = fs.readFileSync(new URL('../src/components/candidate-integrity-boundary.tsx', import.meta.url), 'utf8');\n"
    "const dashboard = fs.readFileSync(new URL('../src/routes/dashboard.tsx', import.meta.url), 'utf8');",
)
replace_once(
    test_path,
    "if (candidate.includes('autoPlay')) throw new Error('Vídeo não pode iniciar automaticamente.');",
    "for (const token of ['MAX_VIDEO_DURATION_SECONDS', 'O vídeo deve ter no máximo 10 minutos']) {\n"
    "  if (!editor.includes(token)) throw new Error(`Validação de duração ausente: ${token}`);\n"
    "}\n"
    "for (const token of ['VIDEO_PLAYBACK_STARTED', 'VIDEO_PLAYBACK_PAUSED', 'VIDEO_PLAYBACK_COMPLETED', 'VIDEO_PLAYBACK_ERROR']) {\n"
    "  if (!integrity.includes(token)) throw new Error(`Telemetria de vídeo ausente: ${token}`);\n"
    "}\n"
    "if (!dashboard.includes('mediaQualityComparisons')) throw new Error('Comparação de qualidade por mídia ausente.');\n"
    "if (candidate.includes('autoPlay')) throw new Error('Vídeo não pode iniciar automaticamente.');",
)

replace_once(
    "docs/arquitetura/midia-acessivel.md",
    "Vídeos usam controles nativos acessíveis por teclado, tela cheia, volume, pausa e controle adicional de velocidade. Reprodução automática com som não é utilizada. Uploads são verificados pelo conteúdo real com Apache Tika; imagens e áudios aceitam até 10 MB e vídeos até 100 MB nos formatos MP4, WebM, Ogg ou QuickTime.",
    "Vídeos usam controles nativos acessíveis por teclado, tela cheia, volume, pausa e controle adicional de velocidade. Reprodução automática com som não é utilizada. Uploads são verificados pelo conteúdo real com Apache Tika; imagens e áudios aceitam até 10 MB e vídeos até 100 MB nos formatos MP4, WebM, Ogg ou QuickTime. A autoria valida metadados e limita vídeos a 10 minutos antes do upload; a gravação bem-sucedida no storage confirma a disponibilidade do objeto. Eventos técnicos de início, pausa, conclusão e erro são registrados sem interferir na pontuação. O dashboard compara conclusão, duração e respostas por `mediaType + mediaVersion`, mantendo versões incompatíveis em grupos separados.",
)

print("Conclusão da issue #500 aplicada")
