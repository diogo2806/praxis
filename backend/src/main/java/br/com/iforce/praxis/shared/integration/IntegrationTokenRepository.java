package br.com.iforce.praxis.shared.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IntegrationTokenRepository extends JpaRepository<IntegrationTokenEntity, Long> {

    @Query("""
            select token
            from IntegrationTokenEntity token
            join fetch token.empresa
            where token.provider = :provider and token.tokenHash = :tokenHash
            """)
    Optional<IntegrationTokenEntity> findFirstByProviderAndTokenHash(
            @Param("provider") String provider,
            @Param("tokenHash") String tokenHash
    );

    List<IntegrationTokenEntity> findByEmpresaIdOrderByProviderAsc(String empresaId);

    List<IntegrationTokenEntity> findByEmpresaIdAndPartnerClientIdIsNullOrderByProviderAsc(String empresaId);

    Optional<IntegrationTokenEntity> findFirstByEmpresaIdAndProvider(String empresaId, String provider);

    Optional<IntegrationTokenEntity> findFirstByEmpresaIdAndProviderAndPartnerClientIdIsNull(
            String empresaId,
            String provider
    );

    Optional<IntegrationTokenEntity> findFirstByEmpresaIdAndPartnerClientIdAndProvider(
            String empresaId,
            String partnerClientId,
            String provider
    );

    boolean existsByEmpresaIdAndPartnerClientIdAndProvider(
            String empresaId,
            String partnerClientId,
            String provider
    );

    void deleteByEmpresaIdAndProvider(String empresaId, String provider);

    void deleteByEmpresaIdAndProviderAndPartnerClientIdIsNull(String empresaId, String provider);

    void deleteByEmpresaIdAndPartnerClientIdAndProvider(
            String empresaId,
            String partnerClientId,
            String provider
    );

    void deleteByPartnerClientId(String partnerClientId);
}
