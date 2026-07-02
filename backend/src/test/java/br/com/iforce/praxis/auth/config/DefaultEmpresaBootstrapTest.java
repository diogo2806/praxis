package br.com.iforce.praxis.auth.config;

import br.com.iforce.praxis.admin.model.EmpresaStatus;

import br.com.iforce.praxis.auth.persistence.entity.EmpresaEntity;

import br.com.iforce.praxis.auth.persistence.repository.EmpresaRepository;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.ArgumentCaptor;

import org.mockito.Mock;

import org.mockito.junit.jupiter.MockitoExtension;


import java.util.Optional;


import static org.assertj.core.api.Assertions.assertThat;

import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.never;

import static org.mockito.Mockito.verify;

import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class DefaultEmpresaBootstrapTest {

    @Mock
    private EmpresaRepository empresaRepository;

    @Test
    void naoCriaEmpresaQuandoSegurancaEstaAtivada() {
        DefaultEmpresaBootstrap bootstrap =
                new DefaultEmpresaBootstrap(empresaRepository, true, "empresa-1");

        bootstrap.run(null);

        verify(empresaRepository, never()).save(any());
    }

    @Test
    void naoCriaEmpresaQuandoEmpresaPadraoJaExiste() {
        when(empresaRepository.existsById("empresa-1")).thenReturn(true);
        DefaultEmpresaBootstrap bootstrap =
                new DefaultEmpresaBootstrap(empresaRepository, false, "empresa-1");

        bootstrap.run(null);

        verify(empresaRepository, never()).save(any());
    }

    @Test
    void criaEmpresaPadraoAtivaQuandoAusente() {
        when(empresaRepository.existsById("tenant-1")).thenReturn(false);
        when(empresaRepository.findFirstByCompanyId("tenant-1")).thenReturn(Optional.empty());
        DefaultEmpresaBootstrap bootstrap =
                new DefaultEmpresaBootstrap(empresaRepository, false, "tenant-1");

        bootstrap.run(null);

        ArgumentCaptor<EmpresaEntity> captor = ArgumentCaptor.forClass(EmpresaEntity.class);
        verify(empresaRepository).save(captor.capture());
        EmpresaEntity saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("tenant-1");
        assertThat(saved.getCompanyId()).isEqualTo("tenant-1");
        assertThat(saved.getStatus()).isEqualTo(EmpresaStatus.ATIVO);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    void desambiguaCompanyIdQuandoJaEstaEmUsoPorOutraEmpresa() {
        when(empresaRepository.existsById("tenant-1")).thenReturn(false);
        when(empresaRepository.findFirstByCompanyId("tenant-1"))
                .thenReturn(Optional.of(new EmpresaEntity()));
        DefaultEmpresaBootstrap bootstrap =
                new DefaultEmpresaBootstrap(empresaRepository, false, "tenant-1");

        bootstrap.run(null);

        ArgumentCaptor<EmpresaEntity> captor = ArgumentCaptor.forClass(EmpresaEntity.class);
        verify(empresaRepository).save(captor.capture());
        assertThat(captor.getValue().getCompanyId()).isEqualTo("tenant-1-default");
    }
}
