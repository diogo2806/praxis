package br.com.iforce.praxis.billing.persistence.repository;

import br.com.iforce.praxis.billing.persistence.entity.EmpresaPlanChangeRequestEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommercialRequestRepository extends JpaRepository<EmpresaPlanChangeRequestEntity, Long> {

    List<EmpresaPlanChangeRequestEntity> findByEmpresaIdOrderByCreatedAtDesc(String empresaId, Pageable pageable);

    boolean existsByEmpresaIdAndStatus(String empresaId, String status);
}
