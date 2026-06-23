package br.com.iforce.praxis.auth.persistence.repository;

import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findFirstByEmailAndTenantId(String email, String tenantId);

    Optional<UserEntity> findFirstByTenantId(String tenantId);

    @Query("select distinct u from UserEntity u join u.roles r where u.tenantId = :tenantId and r = :role")
    List<UserEntity> findByTenantIdAndRole(@Param("tenantId") String tenantId, @Param("role") String role);
}
