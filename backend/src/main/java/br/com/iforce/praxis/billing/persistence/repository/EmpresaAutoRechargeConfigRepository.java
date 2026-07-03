package br.com.iforce.praxis.billing.persistence.repository;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaAutoRechargeConfigEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;


import java.util.Optional;


public interface EmpresaAutoRechargeConfigRepository extends JpaRepository<EmpresaAutoRechargeConfigEntity, String> {

    /**
     * Trava a linha de configuração para serializar disparos de recarga concorrentes do mesmo
     * cliente. É esta trava que garante, junto com o estado {@code PENDING}, que uma rajada de
     * avaliações concluídas ao mesmo tempo não gere várias cobranças para o mesmo cartão.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM EmpresaAutoRechargeConfigEntity c WHERE c.empresaId = :empresaId")
    Optional<EmpresaAutoRechargeConfigEntity> findByEmpresaIdForUpdate(@Param("empresaId") String empresaId);
}
