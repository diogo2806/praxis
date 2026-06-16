package br.com.iforce.praxis.auth.persistence.repository;

import br.com.iforce.praxis.auth.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {

    Optional<UserEntity> findFirstByEmail(String email);

    Optional<UserEntity> findFirstByEmailAndTenantId(String email, String tenantId);
}
