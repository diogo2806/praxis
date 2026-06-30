package br.com.iforce.praxis.billing.persistence.repository;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaCreditBalanceEntity;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Lock;

import org.springframework.data.jpa.repository.Query;

import org.springframework.data.repository.query.Param;


import java.util.Optional;


public interface EmpresaCreditBalanceRepository extends JpaRepository<EmpresaCreditBalanceEntity, String> {

    /** Trava a linha de saldo para alterações concorrentes seguras (compra/consumo). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM EmpresaCreditBalanceEntity b WHERE b.empresaId = :empresaId")
    Optional<EmpresaCreditBalanceEntity> findByEmpresaIdForUpdate(@Param("empresaId") String empresaId);
}
