package br.com.iforce.praxis.auth.persistence.repository;

import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Ponto de consulta dos usuarios da plataforma.
 *
 * <p>Os metodos abaixo apoiam processos de login, convite, reset de senha,
 * administracao de acessos e seguranca por empresa.</p>
 */
public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findFirstByEmailAndEmpresaId(String email, String empresaId);

    Optional<UserEntity> findFirstByEmpresaId(String empresaId);

    @Query("select distinct u from UserEntity u join u.roles r where u.empresaId = :empresaId and r = :role")
    List<UserEntity> findByEmpresaIdAndRole(@Param("empresaId") String empresaId, @Param("role") String role);

    List<UserEntity> findByEmpresaIdOrderByCreatedAtAsc(String empresaId);

    Optional<UserEntity> findByIdAndEmpresaId(Long id, String empresaId);

    boolean existsByEmpresaIdAndEmail(String empresaId, String email);

    /** Localiza diretamente um convite pelo SHA-256 indexado do token. */
    Optional<UserEntity> findFirstByInviteTokenLookupHash(String inviteTokenLookupHash);

    /** Localiza diretamente uma solicitação de redefinicao pelo SHA-256 indexado do token. */
    Optional<UserEntity> findFirstByPasswordResetTokenLookupHash(String passwordResetTokenLookupHash);
}
