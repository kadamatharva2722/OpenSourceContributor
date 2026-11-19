package com.osc.backend.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.osc.backend.admin.security.AdminAuthorizationFilter;
import com.osc.backend.common.dto.ErrorResponseDto;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.SessionManagementConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.session.SessionRegistry;
import com.osc.backend.user.repository.UserRepository;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.session.SessionInformationExpiredStrategy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Spring Security configuration for OpenSourceContributor.
 * Hardened for SPA browser sessions with CSRF protection, strict Actuator exposure,
 * GitHub OAuth 2.0 authentication, and configurable OpenAPI documentation.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final boolean openApiEnabled;
    private final boolean cookieSecure;
    private final GithubOAuth2SuccessHandler successHandler;
    private final GithubOAuth2FailureHandler failureHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final UserRepository userRepository;
    private final ClientIpResolver clientIpResolver;

    @Value("${app.security.rate-limit.oauth-initiation-max-attempts:15}")
    private int oauthInitiationMaxAttempts = 15;

    @Value("${app.security.rate-limit.oauth-initiation-window-seconds:900}")
    private long oauthInitiationWindowSeconds = 900;

    @Value("${app.security.rate-limit.oauth-callback-max-attempts:30}")
    private int oauthCallbackMaxAttempts = 30;

    @Value("${app.security.rate-limit.oauth-callback-window-seconds:900}")
    private long oauthCallbackWindowSeconds = 900;

    @Value("${app.security.admin.github-id:0}")
    private long adminGithubId = 0;

    @Value("${server.servlet.session.cookie.same-site:none}")
    private String cookieSameSite = "none";

    public SecurityConfig(
            ObjectMapper objectMapper,
            @Value("${app.openapi.enabled:true}") boolean openApiEnabled,
            @Value("${server.servlet.session.cookie.secure:true}") boolean cookieSecure,
            @Lazy @Autowired(required = false) GithubOAuth2SuccessHandler successHandler,
            @Autowired(required = false) GithubOAuth2FailureHandler failureHandler,
            @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepository,
            @Autowired(required = false) UserRepository userRepository,
            @Autowired(required = false) ClientIpResolver clientIpResolver
    ) {
        this.objectMapper = objectMapper;
        this.openApiEnabled = openApiEnabled;
        this.cookieSecure = cookieSecure;
        this.successHandler = successHandler;
        this.failureHandler = failureHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.userRepository = userRepository;
        this.clientIpResolver = clientIpResolver;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookiePath("/");
        csrfTokenRepository.setHeaderName("X-XSRF-TOKEN");
        csrfTokenRepository.setCookieName("XSRF-TOKEN");
        csrfTokenRepository.setCookieCustomizer(cookie -> cookie.secure(cookieSecure).sameSite(cookieSameSite));

        ClientIpResolver ipResolver = clientIpResolver != null ? clientIpResolver : new ClientIpResolver("");

        http
                // 1. Hardened Security HTTP Headers
                .headers(headers -> {
                    headers.frameOptions(frame -> frame.deny());
                    headers.contentTypeOptions(Customizer.withDefaults());
                    headers.referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN));
                    headers.permissionsPolicy(permissions -> permissions.policy("camera=(), microphone=(), geolocation=(), browsing-topics=()"));
                    headers.contentSecurityPolicy(csp -> csp
                            .policyDirectives("default-src 'self'; " +
                                    "script-src 'self'; " +
                                    "style-src 'self' 'unsafe-inline'; " +
                                    "font-src 'self' data:; " +
                                    "img-src 'self' data: https://avatars.githubusercontent.com https://*.githubusercontent.com https://github.com; " +
                                    "connect-src 'self'; " +
                                    "frame-ancestors 'none'; " +
                                    "object-src 'none'; " +
                                    "form-action 'self' https://github.com; " +
                                    "base-uri 'self';")
                    );
                })

                // 2. OAuth Login Rate Limiting (protects OAuth initiation and callback against abuse independently)
                .addFilterBefore(
                        new OAuthLoginRateLimitingFilter(
                                ipResolver,
                                objectMapper,
                                oauthInitiationMaxAttempts,
                                oauthInitiationWindowSeconds,
                                oauthCallbackMaxAttempts,
                                oauthCallbackWindowSeconds
                        ),
                        org.springframework.security.web.csrf.CsrfFilter.class
                )
                // 3. CSRF Protection for browser session-based authentication
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler())
                )
                .addFilterAfter(new CsrfCookieFilter(), org.springframework.security.web.csrf.CsrfFilter.class)

                // 3. CORS and Session Management
                .cors(Customizer.withDefaults())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        .sessionFixation(SessionManagementConfigurer.SessionFixationConfigurer::changeSessionId)
                        .maximumSessions(-1)
                        .sessionRegistry(sessionRegistry())
                        .expiredSessionStrategy(sessionInformationExpiredStrategy())
                );

        // 3. Centralized validation for active non-deleted user accounts on authenticated sessions
        if (userRepository != null) {
            http.addFilterBefore(
                    new UserAccountValidationFilter(userRepository, objectMapper),
                    AuthorizationFilter.class
            );
        }

        // Centralized enforcement of two-factor authorization on admin data endpoints
        http.addFilterBefore(
                new AdminAuthorizationFilter(objectMapper, adminGithubId),
                AuthorizationFilter.class
        );

        // 4. Form login and HTTP Basic disabled
        http
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        // 4. OAuth 2.0 Login integration (when configured)
        if (clientRegistrationRepository != null && successHandler != null && failureHandler != null) {
            http.oauth2Login(oauth2 -> oauth2
                    .clientRegistrationRepository(clientRegistrationRepository)
                    .authorizedClientRepository(new DiscardingOAuth2AuthorizedClientRepository())
                    .successHandler(successHandler)
                    .failureHandler(failureHandler)
            );
        }

        // 5. Logout Handling
        http.logout(logout -> logout
                .logoutUrl("/api/v1/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"message\":\"Logged out successfully\"}");
                })
        );

        // 6. Request Authorization Rules
        http.authorizeHttpRequests(auth -> {
            // Safe public endpoints
            auth.requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/v1/auth/status").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/v1/admin/auth/status").permitAll();
            auth.requestMatchers(HttpMethod.POST, "/api/v1/admin/auth/verify-password").permitAll();
            auth.requestMatchers(HttpMethod.POST, "/api/v1/admin/auth/logout").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/v1/discovery/public").permitAll();
            auth.requestMatchers(HttpMethod.GET, "/api/v1/system/wake").permitAll();
            auth.requestMatchers(HttpMethod.POST, "/api/v1/contact").permitAll();
            auth.requestMatchers(HttpMethod.POST, "/api/v1/reports/issues").permitAll();
            auth.requestMatchers(HttpMethod.POST, "/api/v1/requests/repositories").permitAll();

            // OAuth2 authentication endpoints
            auth.requestMatchers("/oauth2/authorization/**", "/login/oauth2/code/**").permitAll();

            // Actuator: explicitly allow only health and info endpoints
            auth.requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info").permitAll();
            // Any other actuator endpoint is strictly denied from public access
            auth.requestMatchers("/actuator/**").denyAll();

            // OpenAPI / Swagger UI: configurable for dev vs production
            if (openApiEnabled) {
                auth.requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                ).permitAll();
            } else {
                auth.requestMatchers(
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                ).denyAll();
            }

            // Protected Application Endpoints
            auth.requestMatchers(HttpMethod.DELETE, "/api/v1/account").authenticated();
            auth.requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated();
            auth.requestMatchers(HttpMethod.POST, "/api/v1/discovery/search").authenticated();
            auth.requestMatchers("/api/v1/saved-issues/**").authenticated();
            auth.requestMatchers("/api/v1/contributions/**").authenticated();
            auth.requestMatchers("/api/v1/preferences/**").authenticated();
            auth.requestMatchers("/api/v1/users/**").authenticated();
            auth.requestMatchers("/api/v1/ai-usage/**").authenticated();
            auth.requestMatchers("/api/v1/**").authenticated();
            auth.anyRequest().authenticated();
        });

        // 7. Centralized JSON Error Handling for 401 & 403
        http.exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint())
                .accessDeniedHandler(accessDeniedHandler())
        );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend.url:http://localhost:3000}") String frontendUrl,
            @Value("${app.admin-frontend.url:http://localhost:3001}") String adminFrontendUrl
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(
                frontendUrl,
                adminFrontendUrl,
                "http://localhost:3000",
                "http://localhost:3001",
                "http://localhost:5173",
                "http://localhost:5174",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:3001",
                "http://127.0.0.1:5173",
                "http://127.0.0.1:5174"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Requested-With", "Accept", "Origin"));
        configuration.setExposedHeaders(List.of("Set-Cookie", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    OffsetDateTime.now(),
                    HttpStatus.UNAUTHORIZED.value(),
                    HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    "Authentication is required to access this resource",
                    request.getRequestURI()
            );
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }

    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            String message = accessDeniedException.getMessage() != null && !accessDeniedException.getMessage().isBlank()
                    ? accessDeniedException.getMessage()
                    : "Access is denied";
            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    OffsetDateTime.now(),
                    HttpStatus.FORBIDDEN.value(),
                    HttpStatus.FORBIDDEN.getReasonPhrase(),
                    message,
                    request.getRequestURI()
            );
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }

    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    public static HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    public SessionInformationExpiredStrategy sessionInformationExpiredStrategy() {
        return event -> {
            HttpServletResponse response = event.getResponse();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponseDto errorResponse = new ErrorResponseDto(
                    OffsetDateTime.now(),
                    HttpStatus.UNAUTHORIZED.value(),
                    HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                    "Session has expired or been terminated",
                    event.getRequest().getRequestURI()
            );
            objectMapper.writeValue(response.getOutputStream(), errorResponse);
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
