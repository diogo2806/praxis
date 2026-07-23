package br.com.iforce.praxis.featureflag.persistence.repository;

import br.com.iforce.praxis.featureflag.persistence.entity.FeatureFlagMetricEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagMetricRepository extends JpaRepository<FeatureFlagMetricEntity, Long> {

    Optional<FeatureFlagMetricEntity> findByFlagKeyAndVariantAndMetric(
            String flagKey,
            String variant,
            String metric
    );

    List<FeatureFlagMetricEntity> findByFlagKeyOrderByMetricAscVariantAsc(String flagKey);
}
