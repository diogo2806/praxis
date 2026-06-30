package br.com.iforce.praxis.tenantconfig.persistence.repository;

import br.com.iforce.praxis.tenantconfig.model.EmpresaConfigType;

import br.com.iforce.praxis.tenantconfig.persistence.entity.EmpresaConfigOptionEntity;

import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface EmpresaConfigOptionRepository extends JpaRepository<EmpresaConfigOptionEntity, Long> {

    List<EmpresaConfigOptionEntity> findByEmpresaIdAndConfigTypeOrderByDisplayOrderAscIdAsc(
            String empresaId,
            EmpresaConfigType configType
    );

    void deleteByEmpresaIdAndConfigType(String empresaId, EmpresaConfigType configType);
}
