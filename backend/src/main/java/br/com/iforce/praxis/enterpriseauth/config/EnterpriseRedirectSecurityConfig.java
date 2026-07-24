package br.com.iforce.praxis.enterpriseauth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
public class EnterpriseRedirectSecurityConfig {

    @Bean
    @Order(0)
    public SecurityFilterChain enterpriseRedirectEndpoint(HttpSecurity http) throws Exception {
        http.securityMatcher(new AntPathRequestMatcher(
                        "/api/v1/enterprise-auth/callback/redirect",
                        "GET"
                ))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
