package com.example.petManagementService.config;

import com.example.petManagementService.common.security.JwtAuthenticationFilter;
import com.example.petManagementService.common.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// @EnableMethodSecurity is what makes @PreAuthorize on controller methods actually get
// enforced. It works independently of the authorizeHttpRequests rules below — a method
// can require CENTER_ADMIN via @PreAuthorize even while its URL is left permitAll(),
// which is exactly what the requests/intake controllers rely on.
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {

        // Not a @Component on purpose: instantiating it here (instead of letting Spring
        // Boot auto-register it as a raw servlet Filter bean) keeps it registered exactly
        // once, at the position chosen below.
        JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(jwtService);

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/h2-console/**").permitAll()
                        // requests/intake controllers all assume @AuthenticationPrincipal
                        // AuthenticatedUser is present (there's no anonymous "my requests" or
                        // "raise an intake"); without this they'd NPE instead of 401ing when
                        // called with no token. Everything else stays permitAll, unchanged.
                        .requestMatchers("/requests/**", "/intake/**").authenticated()
                        // The centers feed and a center's detail page are browsable without a
                        // token; everything else under /centers mutates or exposes membership
                        // and reads user.id() off the principal, so it must 401 rather than NPE.
                        // These two lines are order-sensitive: first match wins, so the public
                        // GETs have to precede the catch-all. "/centers/*" stops at the detail
                        // page — it deliberately does not cover /centers/{id}/members.
                        .requestMatchers(HttpMethod.GET, "/centers", "/centers/*").permitAll()
                        .requestMatchers("/centers/**").authenticated()
                        // Left wide open for now (pre-existing behavior, unchanged elsewhere):
                        // the filter populates SecurityContext when a valid token is present,
                        // but nothing requires one at the URL level for other modules yet.
                        // @PreAuthorize on individual controller methods (requests/intake)
                        // enforces roles independently of this.
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Vite dev server origin for the frontend; add production frontend origin(s) here when deployed.
        config.setAllowedOrigins(List.of("http://localhost:5173","https://pet-management-user-dashboard.vercel.app"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
