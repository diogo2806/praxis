package br.com.iforce.praxis.config;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

public class DatabaseEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "praxisDatabaseEnvironment";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String dbHost = environment.getProperty("DB_HOST");
        if (!StringUtils.hasText(dbHost)) {
            return;
        }

        String datasourceUrl = environment.getProperty("spring.datasource.url");
        if (StringUtils.hasText(datasourceUrl) && !datasourceUrl.contains("${")) {
            return;
        }

        String dbPort = environment.getProperty("DB_PORT", "5432");
        String dbName = environment.getProperty("DB_NAME", "postgres");
        String dbSchema = environment.getProperty("DB_SCHEMA", "public");

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(
            "spring.datasource.url",
            "jdbc:postgresql://%s:%s/%s?currentSchema=%s".formatted(dbHost, dbPort, dbName, dbSchema)
        );
        putIfPresent(properties, environment, "spring.datasource.username", "DB_USER");
        putIfPresent(properties, environment, "spring.datasource.password", "DB_PASS");
        properties.put("spring.flyway.default-schema", dbSchema);
        properties.put("spring.flyway.schemas", dbSchema);
        properties.put("spring.jpa.properties.hibernate.default_schema", dbSchema);

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, properties));
    }

    private static void putIfPresent(
        Map<String, Object> properties,
        ConfigurableEnvironment environment,
        String propertyName,
        String environmentName
    ) {
        String value = environment.getProperty(environmentName);
        if (StringUtils.hasText(value)) {
            properties.put(propertyName, value);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
