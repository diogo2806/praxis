package br.com.iforce.praxis.partner.service;

import br.com.iforce.praxis.gupy.service.SimulationCatalogService;
import br.com.iforce.praxis.partner.persistence.entity.PartnerClientEntity;
import br.com.iforce.praxis.partner.persistence.repository.PartnerCatalogAccessRepository;
import br.com.iforce.praxis.partner.persistence.repository.PartnerClientRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PartnerIntegrationCatalogServiceTest {

    private final PartnerClientRepository clientRepository = mock(PartnerClientRepository.class);
    private final PartnerCatalogAccessRepository accessRepository = mock(PartnerCatalogAccessRepository.class);
    private final SimulationCatalogService simulationCatalogService = mock(SimulationCatalogService.class);
    private final PartnerIntegrationCatalogService service = new PartnerIntegrationCatalogService(
            clientRepository,
            accessRepository,
            simulationCatalogService
    );

    @Test
    void permiteTesteExplicitamenteLiberado() {
        PartnerClientEntity client = activeClient();
        when(clientRepository.findByIdAndEmpresaId("cliente-1", "parceiro-1"))
                .thenReturn(Optional.of(client));
        when(accessRepository.existsByEmpresaIdAndPartnerClientIdAndSimulationIdAndActiveTrue(
                "parceiro-1",
                "cliente-1",
                "teste-1"
        )).thenReturn(true);

        assertThatCode(() -> service.assertAccess("parceiro-1", "cliente-1", "teste-1"))
                .doesNotThrowAnyException();
    }

    @Test
    void bloqueiaTesteForaDoCatalogo() {
        PartnerClientEntity client = activeClient();
        when(clientRepository.findByIdAndEmpresaId("cliente-1", "parceiro-1"))
                .thenReturn(Optional.of(client));
        when(accessRepository.existsByEmpresaIdAndPartnerClientIdAndSimulationIdAndActiveTrue(
                "parceiro-1",
                "cliente-1",
                "teste-nao-liberado"
        )).thenReturn(false);

        assertThatThrownBy(() -> service.assertAccess(
                "parceiro-1",
                "cliente-1",
                "teste-nao-liberado"
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Teste não liberado");
    }

    @Test
    void bloqueiaClienteDesativado() {
        PartnerClientEntity client = activeClient();
        client.setActive(false);
        when(clientRepository.findByIdAndEmpresaId("cliente-1", "parceiro-1"))
                .thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.assertAccess("parceiro-1", "cliente-1", "teste-1"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("desativado");
    }

    private PartnerClientEntity activeClient() {
        PartnerClientEntity client = new PartnerClientEntity();
        client.setId("cliente-1");
        client.setEmpresaId("parceiro-1");
        client.setActive(true);
        return client;
    }
}
