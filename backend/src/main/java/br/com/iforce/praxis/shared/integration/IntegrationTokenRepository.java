package br.com.iforce.praxis.shared.integration;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("""
            select token
            from IntegrationTokenEntity token
            where token.empresaId = :empresaId and token.partnerClientId is null
            order by token.provider asc
            """)
    List<IntegrationTokenEntity> findByEmpresaIdOrderByProviderAsc(@Param("empresaId") String empresaId);

    List<IntegrationTokenEntity> findByEmpresaIdAndPartnerClientIdIsNullOrderByProviderAsc(String empresaId);

    @Query("""
            select token
            from IntegrationTokenEntity token
            where token.empresaId = :empresaId
              and token.provider = :provider
              and token.partnerClientId is null
            """)
    Optional<IntegrationTokenEntity> findFirstByEmpresaIdAndProvider(
            @Param("empresaId") String empresaId,
            @Param("provider") String provider
    );

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

    @Modifying
    @Query("""
            delete from IntegrationTokenEntity token
            where token.empresaId = :empresaId
              and token.provider = :provider
              and token.partnerClientId is null
            """)
    void deleteByEmpresaIdAndProvider(
            @Param("empresaId") String empresaId,
            @Param("provider") String provider
    );

    void deleteByEmpresaIdAndProviderAndPartnerClientIdIsNull(String empresaId, String provider);

    void deleteByEmpresaIdAndPartnerClientIdAndProvider(
            String empresaId,
            String partnerClientId,
            String provider
    );

    void deleteByPartnerClientId(String partnerClientId);
}
