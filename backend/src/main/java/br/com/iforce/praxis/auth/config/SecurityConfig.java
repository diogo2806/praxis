package br.com.iforce.praxis.auth.config;

import br.com.iforce.praxis.auth.filter.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtFilter;
    private final boolean securityEnabled;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled
    ) {
        this.jwtFilter = jwtFilter;
        this.securityEnabled = securityEnabled;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!securityEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            http.headers(headers -> headers
                    .frameOptions(frame -> frame.deny())
                    .contentTypeOptions(contentType -> {})
            );
            return http.build();
        }

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/candidate/**",
                                "/candidato/**",
                                "/docs/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/actuator/health",
                                "/test",
                                "/test/**",
                                "/recrutei/test",
                                "/recrutei/test/**",
                                "/api/v1/auth/invite/**",
                                // Recuperação de senha: público, sem JWT. Não revela a
                                // existência de contas e é limitado por IP no controller.
                                "/api/v1/auth/password/**",
                                // Webhook do Mercado Pago (Parte B): público, validado por
                                // assinatura no próprio handler, sem JWT de usuário.
                                "/api/webhooks/mercado-pago/**"
                        ).permitAll()
                        // Painel administrativo da plataforma: exige operador ADMIN e não
                        // depende do tenant do usuário logado (tenant alvo vem na rota).
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/account/**").hasRole("EMPRESA")
                        .requestMatchers("/api/v1/company-profile/**").hasRole("EMPRESA")
                        .requestMatchers("/api/v1/integrations/**").hasRole("EMPRESA")
                        .requestMatchers("/api/v1/simulations/**").hasRole("EMPRESA")
                        .requestMatchers("/api/v1/media/**").hasRole("EMPRESA")
                        .requestMatchers("/api/v1/tenant-config/**").hasRole("EMPRESA")
                        .requestMatchers("/api/v1/gupy/result-deliveries/**").hasRole("EMPRESA")
                        .requestMatchers("/api/v1/notifications/**").hasRole("EMPRESA")
                        .requestMatchers("/api/v1/audit/**").hasRole("EMPRESA")
                        .requestMatchers("/api/v1/terms/**").hasRole("EMPRESA")
                        .requestMatchers("/api/v1/candidate-links", "/api/v1/candidate-links/**").hasRole("EMPRESA")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        http.headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(contentType -> {})
        );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager userDetailsService() {
        return new InMemoryUserDetailsManager();
    }

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
