package br.com.iforce.praxis.auth.config;

import br.com.iforce.praxis.auth.filter.JwtAuthenticationFilter;
import br.com.iforce.praxis.auth.filter.PartnerSpecialistAuthorizationFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

@Configuration
public class SecurityConfig {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String[] ACCOUNT_ROLES = {
            "ADMIN",
            "PARTNER_SPECIALIST",
            "TEAM_MANAGER",
            "PARTNER_MANAGER",
            "ASSESSMENT_EDITOR",
            "RESULTS_ANALYST",
            "OPERATIONS_MANAGER"
    };
    private static final String[] COMPANY_ROLES = {
            "TEAM_MANAGER",
            "PARTNER_MANAGER",
            "ASSESSMENT_EDITOR",
            "RESULTS_ANALYST",
            "OPERATIONS_MANAGER"
    };
    private static final String[] COMPANY_MANAGER_ROLES = {"TEAM_MANAGER", "PARTNER_MANAGER"};
    private static final String[] AUTHOR_ROLES = {"TEAM_MANAGER", "ASSESSMENT_EDITOR", "PARTNER_SPECIALIST"};
    private static final String[] ANALYSIS_ROLES = {"TEAM_MANAGER", "RESULTS_ANALYST"};
    private static final String[] OPERATIONS_ROLES = {"TEAM_MANAGER", "OPERATIONS_MANAGER"};
    private static final String[] INTEGRITY_REVIEW_ROLES = {
            "TEAM_MANAGER",
            "PARTNER_MANAGER",
            "OPERATIONS_MANAGER"
    };
    private static final String[] ANALYSIS_OR_OPERATIONS_ROLES = {
            "TEAM_MANAGER",
            "RESULTS_ANALYST",
            "OPERATIONS_MANAGER"
    };

    private final JwtAuthenticationFilter jwtFilter;
    private final PartnerSpecialistAuthorizationFilter specialistAuthorizationFilter;
    private final boolean securityEnabled;

    public SecurityConfig(
            JwtAuthenticationFilter jwtFilter,
            PartnerSpecialistAuthorizationFilter specialistAuthorizationFilter,
            @Value("${praxis.security.enabled:true}") boolean securityEnabled
    ) {
        this.jwtFilter = jwtFilter;
        this.specialistAuthorizationFilter = specialistAuthorizationFilter;
        this.securityEnabled = securityEnabled;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.ignoringRequestMatchers(this::isCsrfExemptRequest))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        if (!securityEnabled) {
            http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            applySecurityHeaders(http);
            return http.build();
        }

        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/login",
                                "/api/v1/runtime-config",
                                "/api/v1/candidate/*/realistic-preview/**",
                                "/candidate/**",
                                "/candidato/**",
                                "/actuator/health",
                                "/actuator/info",
                                "/test",
                                "/test/**",
                                "/recrutei/test",
                                "/recrutei/test/**",
                                "/api/v1/auth/invite/**",
                                "/api/v1/auth/password/**",
                                "/api/webhooks/mercado-pago/**"
                        ).permitAll()
                        .requestMatchers("/docs", "/docs/**", "/v3/api-docs/**", "/swagger-ui/**")
                        .hasRole("ADMIN")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/account/**").hasAnyRole(ACCOUNT_ROLES)
                        .requestMatchers("/api/v1/dashboard/**").hasAnyRole(COMPANY_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/v1/team", "/api/v1/team/**")
                        .hasAnyRole(COMPANY_ROLES)
                        .requestMatchers("/api/v1/team", "/api/v1/team/**")
                        .hasAnyRole(COMPANY_MANAGER_ROLES)
                        .requestMatchers("/api/v1/company-profile/**")
                        .hasAnyRole(COMPANY_MANAGER_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/v1/integrations/**")
                        .hasAnyRole(OPERATIONS_ROLES)
                        .requestMatchers("/api/v1/integrations/**")
                        .hasAnyRole(COMPANY_MANAGER_ROLES)
                        .requestMatchers("/api/v1/privacy/**")
                        .hasAnyRole(COMPANY_MANAGER_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/v1/simulations/**")
                        .hasAnyRole(
                                "TEAM_MANAGER",
                                "ASSESSMENT_EDITOR",
                                "RESULTS_ANALYST",
                                "OPERATIONS_MANAGER",
                                "PARTNER_SPECIALIST"
                        )
                        .requestMatchers("/api/v1/simulations/**").hasAnyRole(AUTHOR_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/v1/assessment-journeys/**")
                        .hasAnyRole(COMPANY_ROLES)
                        .requestMatchers("/api/v1/assessment-journeys/**")
                        .hasAnyRole(COMPANY_MANAGER_ROLES)
                        .requestMatchers("/api/v1/assessment-journey-attempts/**")
                        .hasAnyRole(OPERATIONS_ROLES)
                        .requestMatchers("/api/v1/media/**").hasAnyRole(AUTHOR_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/v1/empresa-config/**")
                        .hasAnyRole(AUTHOR_ROLES)
                        .requestMatchers("/api/v1/empresa-config/**")
                        .hasAnyRole(COMPANY_MANAGER_ROLES)
                        .requestMatchers("/api/v1/gupy/result-deliveries/**")
                        .hasAnyRole(OPERATIONS_ROLES)
                        .requestMatchers("/api/v1/integrity-reviews", "/api/v1/integrity-reviews/**")
                        .hasAnyRole(INTEGRITY_REVIEW_ROLES)
                        .requestMatchers("/api/v1/results/**").hasAnyRole(ANALYSIS_ROLES)
                        .requestMatchers("/api/v1/notifications/**").hasAnyRole(OPERATIONS_ROLES)
                        .requestMatchers("/api/v1/audit/**").hasAnyRole(COMPANY_MANAGER_ROLES)
                        .requestMatchers("/api/v1/terms/**").hasAnyRole(COMPANY_MANAGER_ROLES)
                        .requestMatchers(HttpMethod.GET, "/api/v1/candidate-links", "/api/v1/candidate-links/**")
                        .hasAnyRole(ANALYSIS_OR_OPERATIONS_ROLES)
                        .requestMatchers(HttpMethod.POST, "/api/v1/candidate-links/*/disposition")
                        .hasAnyRole(ANALYSIS_ROLES)
                        .requestMatchers("/api/v1/candidate-links", "/api/v1/candidate-links/**")
                        .hasAnyRole(OPERATIONS_ROLES)
                        .requestMatchers("/api/v1/billing", "/api/v1/billing/**")
                        .hasAnyRole(COMPANY_MANAGER_ROLES)
                        .requestMatchers("/api/v1/partners", "/api/v1/partners/**")
                        .hasAnyRole(COMPANY_MANAGER_ROLES)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(specialistAuthorizationFilter, JwtAuthenticationFilter.class);
        applySecurityHeaders(http);
        return http.build();
    }

    private void applySecurityHeaders(HttpSecurity http) throws Exception {
        http.headers(headers -> {
            headers.frameOptions(frame -> frame.deny());
            headers.contentTypeOptions(contentType -> { });
            headers.httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31_536_000));
            headers.referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER));
            headers.permissionsPolicyHeader(policy -> policy
                    .policy("camera=(), microphone=(), geolocation=(), payment=()"));
            headers.contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'self'"));
        });
    }

    private boolean isCsrfExemptRequest(HttpServletRequest request) {
        if (!securityEnabled) {
            return true;
        }

        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith(BEARER_PREFIX)) {
            return true;
        }

        String path = resolveRequestPath(request);
        return "/api/v1/auth/login".equals(path)
                || matchesPathOrDescendant(path, "/api/v1/auth/invite")
                || matchesPathOrDescendant(path, "/api/v1/auth/password")
                || matchesPathOrDescendant(path, "/api/v1/candidate")
                || matchesPathOrDescendant(path, "/candidate")
                || matchesPathOrDescendant(path, "/candidato")
                || matchesPathOrDescendant(path, "/test")
                || matchesPathOrDescendant(path, "/recrutei/test")
                || matchesPathOrDescendant(path, "/api/webhooks/mercado-pago");
    }

    private static String resolveRequestPath(HttpServletRequest request) {
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        return contextPath == null || contextPath.isBlank()
                ? requestUri
                : requestUri.substring(contextPath.length());
    }

    private static boolean matchesPathOrDescendant(String path, String rootPath) {
        return rootPath.equals(path) || path.startsWith(rootPath + "/");
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
    public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(
            JwtAuthenticationFilter filter
    ) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<PartnerSpecialistAuthorizationFilter> specialistFilterRegistration(
            PartnerSpecialistAuthorizationFilter filter
    ) {
        FilterRegistrationBean<PartnerSpecialistAuthorizationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
