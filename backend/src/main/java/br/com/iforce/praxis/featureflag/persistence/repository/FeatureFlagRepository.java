package br.com.iforce.praxis.featureflag.persistence.repository;

import br.com.iforce.praxis.featureflag.persistence.entity.FeatureFlagEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FeatureFlagRepository extends JpaRepository<FeatureFlagEntity, String> {

    Optional<FeatureFlagEntity> findByKey(String key);

    boolean existsByKey(String key);

    List<FeatureFlagEntity> findAllByOrderByKeyAsc();
}
