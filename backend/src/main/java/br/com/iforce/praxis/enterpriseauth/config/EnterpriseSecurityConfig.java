package br.com.iforce.praxis.enterpriseauth.config;

import br.com.iforce.praxis.enterpriseauth.filter.EnterpriseJwtAuthenticationFilter;
import br.com.iforce.praxis.enterpriseauth.service.EnterpriseTokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
public class EnterpriseSecurityConfig {

    private static final String BEARER_PREFIX = "Bearer ";
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

    @Bean
    @Order(1)
    public SecurityFilterChain enterprisePublicEndpoints(HttpSecurity http) throws Exception {
        RequestMatcher publicEndpoints = new OrRequestMatcher(
                new AntPathRequestMatcher("/api/v1/enterprise-auth/discovery", HttpMethod.GET.name()),
                new AntPathRequestMatcher("/api/v1/enterprise-auth/providers/*/start", HttpMethod.POST.name()),
                new AntPathRequestMatcher("/api/v1/enterprise-auth/callback", HttpMethod.GET.name())
        );
        http.securityMatcher(publicEndpoints)
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain enterpriseBearerEndpoints(
            HttpSecurity http,
            EnterpriseTokenService tokenService,
            EnterpriseJwtAuthenticationFilter enterpriseJwtAuthenticationFilter
    ) throws Exception {
        http.securityMatcher(request -> isEnterpriseBearerRequest(request, tokenService))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/account/**").hasAnyRole(COMPANY_ROLES)
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
                        .hasAnyRole("TEAM_MANAGER", "ASSESSMENT_EDITOR", "RESULTS_ANALYST",
                                "OPERATIONS_MANAGER", "PARTNER_SPECIALIST")
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
                        .requestMatchers("/api/v1/results/**").hasAnyRole(ANALYSIS_ROLES)
                        .requestMatchers("/api/v1/notifications/**").hasAnyRole(OPERATIONS_ROLES)
                        .requestMatchers("/api/v1/audit/**").hasAnyRole(COMPANY_MANAGER_ROLES)
                        .requestMatchers("/api/v1/terms/**").hasAnyRole(COMPANY_MANAGER_ROLES)
                        .requestMatchers("/api/v1/billing", "/api/v1/billing/**")
                        .hasAnyRole(COMPANY_MANAGER_ROLES)
                        .requestMatchers("/api/v1/partners", "/api/v1/partners/**")
                        .hasAnyRole(COMPANY_MANAGER_ROLES)
                        .anyRequest().authenticated()
                )
                .addFilterBefore(enterpriseJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public FilterRegistrationBean<EnterpriseJwtAuthenticationFilter> enterpriseJwtFilterRegistration(
            EnterpriseJwtAuthenticationFilter filter
    ) {
        FilterRegistrationBean<EnterpriseJwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    private boolean isEnterpriseBearerRequest(
            HttpServletRequest request,
            EnterpriseTokenService tokenService
    ) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        return authorization != null
                && authorization.startsWith(BEARER_PREFIX)
                && tokenService.isEnterpriseToken(
                        authorization.substring(BEARER_PREFIX.length()).trim()
                );
    }
}
