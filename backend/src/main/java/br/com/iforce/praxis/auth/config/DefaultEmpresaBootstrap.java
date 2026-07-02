package br.com.iforce.praxis.auth.config;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import org.slf4j.Logger;

import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.boot.ApplicationArguments;

import org.springframework.boot.ApplicationRunner;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;


import java.time.Instant;


/**
 * Garante a existência da empresa padrão quando a segurança está desativada.
 *
 * <p>Com {@code praxis.security.enabled=false}, todas as escritas usam
 * {@code praxis.default-empresa-id} como empresa atual. Se essa linha não existir
 * na tabela {@code empresas} (banco antigo semeado com outro id, id customizado
 * via variável de ambiente ou linha removida manualmente), qualquer escrita
 * falha na foreign key de empresa. Este bootstrap é idempotente e recria a
 * empresa padrão no startup para evitar esse estado.</p>
 */
@Component
public class DefaultEmpresaBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultEmpresaBootstrap.class);

    private final EmpresaRepository empresaRepository;
    private final boolean securityEnabled;
    private final String defaultEmpresaId;

    public DefaultEmpresaBootstrap(
            EmpresaRepository empresaRepository,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled,
            @Value("${praxis.default-empresa-id:empresa-1}") String defaultEmpresaId
    ) {
        this.empresaRepository = empresaRepository;
        this.securityEnabled = securityEnabled;
        this.defaultEmpresaId = defaultEmpresaId;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (securityEnabled || defaultEmpresaId == null || defaultEmpresaId.isBlank()) {
            return;
        }
        if (empresaRepository.existsById(defaultEmpresaId)) {
            return;
        }

        Instant now = Instant.now();
        EmpresaEntity empresa = new EmpresaEntity();
        empresa.setId(defaultEmpresaId);
        empresa.setName("Empresa padrão (" + defaultEmpresaId + ")");
        empresa.setCompanyId(resolveAvailableCompanyId());
        empresa.setStatus(EmpresaStatus.ATIVO);
        empresa.setCreatedAt(now);
        empresa.setUpdatedAt(now);
        empresaRepository.save(empresa);

        log.warn(
                "Empresa padrão '{}' não existia e foi criada automaticamente "
                        + "(praxis.security.enabled=false).",
                defaultEmpresaId
        );
    }

    /** O company_id é único; usa o próprio id da empresa e desambigua se já estiver em uso. */
    private String resolveAvailableCompanyId() {
        if (empresaRepository.findFirstByCompanyId(defaultEmpresaId).isEmpty()) {
            return defaultEmpresaId;
        }
        return defaultEmpresaId + "-default";
    }
}
