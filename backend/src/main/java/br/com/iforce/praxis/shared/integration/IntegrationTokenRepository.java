package br.com.iforce.praxis.shared.integration;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IntegrationTokenRepository extends JpaRepository<IntegrationTokenEntity, Long> {

    Optional<IntegrationTokenEntity> findFirstByProviderAndTokenHash(String provider, String tokenHash);
}
