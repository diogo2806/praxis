package br.com.iforce.praxis.integration.ats.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AdapterRegistryIntegrationTest {

    @Test
    void shouldRegisterAllAvailableAdapters() {
        GupyAdapter gupyAdapter = mock(GupyAdapter.class);
        WorkdayAdapter workdayAdapter = mock(WorkdayAdapter.class);
        GreenhouseAdapter greenhouseAdapter = mock(GreenhouseAdapter.class);

        List<ATSAdapter> adapters = List.of(gupyAdapter, workdayAdapter, greenhouseAdapter);
        AdapterRegistry registry = new AdapterRegistry(adapters);

        assertThat(registry.getSupportedPlatforms())
            .contains(
                ATSAdapter.ATSPlatform.GUPY,
                ATSAdapter.ATSPlatform.WORKDAY,
                ATSAdapter.ATSPlatform.GREENHOUSE
            )
            .hasSize(3);
    }

    @Test
    void shouldResolveGupyAdapter() {
        GupyAdapter gupyAdapter = new GupyAdapter(null);
        List<ATSAdapter> adapters = List.of(gupyAdapter);
        AdapterRegistry registry = new AdapterRegistry(adapters);

        ATSAdapter resolved = registry.getAdapter(ATSAdapter.ATSPlatform.GUPY);

        assertThat(resolved).isInstanceOf(GupyAdapter.class);
    }

    @Test
    void shouldResolveWorkdayAdapter() {
        WorkdayAdapter workdayAdapter = new WorkdayAdapter();
        List<ATSAdapter> adapters = List.of(workdayAdapter);
        AdapterRegistry registry = new AdapterRegistry(adapters);

        ATSAdapter resolved = registry.getAdapter(ATSAdapter.ATSPlatform.WORKDAY);

        assertThat(resolved).isInstanceOf(WorkdayAdapter.class);
    }

    @Test
    void shouldResolveGreenhouseAdapter() {
        GreenhouseAdapter greenhouseAdapter = new GreenhouseAdapter();
        List<ATSAdapter> adapters = List.of(greenhouseAdapter);
        AdapterRegistry registry = new AdapterRegistry(adapters);

        ATSAdapter resolved = registry.getAdapter(ATSAdapter.ATSPlatform.GREENHOUSE);

        assertThat(resolved).isInstanceOf(GreenhouseAdapter.class);
    }

    @Test
    void shouldThrowExceptionWhenAdapterNotRegistered() {
        List<ATSAdapter> adapters = List.of();
        AdapterRegistry registry = new AdapterRegistry(adapters);

        assertThatThrownBy(() -> registry.getAdapter(ATSAdapter.ATSPlatform.GUPY))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Nenhum adapter registrado para plataforma");
    }

    @Test
    void shouldProvideGupyAdapterAsDefault() {
        GupyAdapter gupyAdapter = new GupyAdapter(null);
        List<ATSAdapter> adapters = List.of(gupyAdapter);
        AdapterRegistry registry = new AdapterRegistry(adapters);

        ATSAdapter defaultAdapter = registry.getGupyAdapter();

        assertThat(defaultAdapter).isInstanceOf(GupyAdapter.class);
    }

    @Test
    void shouldSupportMultipleAdaptersSimultaneously() {
        GupyAdapter gupyAdapter = new GupyAdapter(null);
        WorkdayAdapter workdayAdapter = new WorkdayAdapter();
        GreenhouseAdapter greenhouseAdapter = new GreenhouseAdapter();

        List<ATSAdapter> adapters = List.of(gupyAdapter, workdayAdapter, greenhouseAdapter);
        AdapterRegistry registry = new AdapterRegistry(adapters);

        ATSAdapter gupy = registry.getAdapter(ATSAdapter.ATSPlatform.GUPY);
        ATSAdapter workday = registry.getAdapter(ATSAdapter.ATSPlatform.WORKDAY);
        ATSAdapter greenhouse = registry.getAdapter(ATSAdapter.ATSPlatform.GREENHOUSE);

        assertThat(gupy).isInstanceOf(GupyAdapter.class);
        assertThat(workday).isInstanceOf(WorkdayAdapter.class);
        assertThat(greenhouse).isInstanceOf(GreenhouseAdapter.class);
    }

    @Test
    void shouldAllowSwitchingBetweenAdapters() {
        GupyAdapter gupyAdapter = new GupyAdapter(null);
        WorkdayAdapter workdayAdapter = new WorkdayAdapter();

        List<ATSAdapter> adapters = List.of(gupyAdapter, workdayAdapter);
        AdapterRegistry registry = new AdapterRegistry(adapters);

        ATSAdapter firstAdapter = registry.getAdapter(ATSAdapter.ATSPlatform.GUPY);
        assertThat(firstAdapter.type()).isEqualTo(ATSAdapter.ATSPlatform.GUPY);

        ATSAdapter secondAdapter = registry.getAdapter(ATSAdapter.ATSPlatform.WORKDAY);
        assertThat(secondAdapter.type()).isEqualTo(ATSAdapter.ATSPlatform.WORKDAY);

        ATSAdapter backToFirst = registry.getAdapter(ATSAdapter.ATSPlatform.GUPY);
        assertThat(backToFirst.type()).isEqualTo(ATSAdapter.ATSPlatform.GUPY);
    }
}
