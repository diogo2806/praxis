package br.com.iforce.praxis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PraxisBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PraxisBackendApplication.class, args);
    }
}
