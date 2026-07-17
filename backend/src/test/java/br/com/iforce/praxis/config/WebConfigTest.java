package br.com.iforce.praxis.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebConfigTest {

    @Test
    void keepsCorsClosedWhenNoOriginIsConfiguredOutsideLocalProfiles() {
        WebConfig config = new WebConfig("", new MockEnvironment());

        assertThat(allowedOrigins(config)).isEmpty();
    }

    @Test
    void enablesOnlyLocalDefaultsInTestProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");

        WebConfig config = new WebConfig("", environment);

        assertThat(allowedOrigins(config))
                .containsExactly(
                        "http://localhost:3000",
                        "http://localhost:5173",
                        "http://127.0.0.1:3000",
                        "http://127.0.0.1:5173"
                );
    }

    @Test
    void acceptsConfiguredHttpsOrigins() {
        WebConfig config = new WebConfig(
                "https://praxis.iforce.com.br, https://admin.iforce.com.br",
                new MockEnvironment()
        );

        assertThat(allowedOrigins(config))
                .containsExactly("https://praxis.iforce.com.br", "https://admin.iforce.com.br");
    }

    @Test
    void rejectsWildcardOrigins() {
        assertThatThrownBy(() -> new WebConfig("https://*.iforce.com.br", new MockEnvironment()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("curinga");
    }

    @Test
    void rejectsInsecureExternalOrigins() {
        assertThatThrownBy(() -> new WebConfig("http://praxis.iforce.com.br", new MockEnvironment()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }

    private String[] allowedOrigins(WebConfig config) {
        return (String[]) ReflectionTestUtils.getField(config, "allowedOrigins");
    }
}
