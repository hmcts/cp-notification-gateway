package uk.gov.hmcts.cp.notification.web;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

// Interim posture (NG-S04): the read-only query API is served open (unauthenticated) in every profile.
// Authentication/authorisation is delivered by NG-S14, which replaces this permit-all chain.
//
// CSRF is left at Spring Security's default (enabled). The only HTTP surface today is GET query endpoints,
// which CSRF never guards, so this has no functional effect now. When a state-changing (POST/PUT/DELETE)
// endpoint is introduced it must configure CSRF explicitly — a stateless service needs a non-session token
// strategy (e.g. CookieCsrfTokenRepository) or an explicit ignore for header-token-authenticated routes.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
                .build();
    }
}
