package br.com.iforce.praxis.featureflag.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "feature_flag_metrics",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_feature_flag_metric_variant",
                columnNames = {"flag_key", "variant", "metric"}
        )
)
public class FeatureFlagMetricEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flag_key", nullable = false, length = 120)
    private String flagKey;

    @Column(name = "variant", nullable = false, length = 20)
    private String variant;

    @Column(name = "metric", nullable = false, length = 60)
    private String metric;

    @Column(name = "sample_count", nullable = false)
    private long sampleCount;

    @Column(name = "total_value", nullable = false)
    private double totalValue;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;
}
