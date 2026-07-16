package br.com.iforce.praxis.simulation.persistence.entity;

import br.com.iforce.praxis.shared.jpa.EmpresaAwareEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.text.Normalizer;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "simulations")
public class SimulationEntity implements EmpresaAwareEntity {

    @Id
    @Column(name = "id", nullable = false, length = 120)
    private String id;

    @Column(name = "empresa_id", nullable = false, length = 120)
    private String empresaId;

    @Column(name = "name", nullable = false, length = 180)
    private String name;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "critical_situation", length = 1200)
    private String criticalSituation;

    @Column(name = "result_use", length = 120)
    private String resultUse;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SimulationVersionEntity> versions = new LinkedHashSet<>();

    /**
     * Versões antigas do assistente serializavam cargo, situação, competências
     * e uso do resultado em {@code description}. Esses dados já possuem campos
     * próprios no domínio. Antes de gravar, convertemos somente esse formato
     * reconhecido em uma descrição pública simples; textos livres são mantidos.
     */
    @PrePersist
    @PreUpdate
    void normalizeLegacyPlanningDescription() {
        if (!isLegacyPlanningDescription(description)) {
            return;
        }

        String normalizedName = name == null ? "" : name.trim();
        String publicDescription = normalizedName.isBlank()
                ? "Avaliação situacional estruturada."
                : "Avaliação situacional estruturada para " + normalizedName + ".";
        description = truncate(publicDescription);
    }

    private boolean isLegacyPlanningDescription(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }

        int recognizedLines = 0;
        int nonBlankLines = 0;
        boolean singleLinePlanningSummary = false;
        for (String rawLine : value.split("\\R")) {
            String line = rawLine.trim();
            if (line.isBlank()) {
                continue;
            }
            nonBlankLines++;
            int separator = line.indexOf(':');
            if (separator <= 0) {
                return false;
            }

            String label = normalizeLabel(line.substring(0, separator));
            if (!isPlanningLabel(label)) {
                return false;
            }
            recognizedLines++;

            if (isRoleLabel(label) && line.contains(" - ")) {
                singleLinePlanningSummary = true;
            }
        }

        return recognizedLines >= 2
                || (recognizedLines == 1 && nonBlankLines == 1 && singleLinePlanningSummary);
    }

    private boolean isPlanningLabel(String label) {
        return isRoleLabel(label)
                || label.contains("competenc")
                || (containsAny(label, "situacao", "situation", "situacion")
                    && containsAny(label, "critica", "critical"))
                || (containsAny(label, "uso", "use") && label.contains("result"));
    }

    private boolean isRoleLabel(String label) {
        return "cargo".equals(label) || "role".equals(label) || "puesto".equals(label);
    }

    private boolean containsAny(String value, String... fragments) {
        for (String fragment : fragments) {
            if (value.contains(fragment)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeLabel(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String truncate(String value) {
        return value.length() <= 1000 ? value : value.substring(0, 997).stripTrailing() + "...";
    }
}
