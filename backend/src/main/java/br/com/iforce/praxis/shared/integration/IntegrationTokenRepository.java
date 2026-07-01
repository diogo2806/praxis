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

    Optional<IntegrationTokenEntity> findFirstByEmpresaIdAndProvider(String empresaId, String provider);

    void deleteByEmpresaIdAndProvider(String empresaId, String provider);
}
