package com.cse444.threadtrace.common.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // Public Thymeleaf and Passport Endpoints (FR7, FR8, QA3, QA4)
                .requestMatchers("/passports/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/passports/*/qr").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/passports/*").permitAll()
                // Role-restricted Audience Endpoints
                .requestMatchers("/api/v1/passports/*/auditor").hasRole("AUDITOR")
                .requestMatchers("/api/v1/passports/*/brand").hasAnyRole("BRAND", "AUDITOR")
                // General & Docs
                .requestMatchers(
                    "/actuator/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/api/**"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .httpBasic(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        UserDetails brandPat = User.withUsername("BRAND-PAT-001")
                .password("{noop}password")
                .roles("BRAND")
                .build();

        UserDetails brandEco = User.withUsername("BRAND-ECO-001")
                .password("{noop}password")
                .roles("BRAND")
                .build();

        UserDetails brandOther = User.withUsername("BRAND-OTHER-002")
                .password("{noop}password")
                .roles("BRAND")
                .build();

        UserDetails auditor = User.withUsername("auditor-inspector")
                .password("{noop}password")
                .roles("AUDITOR")
                .build();

        return new InMemoryUserDetailsManager(brandPat, brandEco, brandOther, auditor);
    }
}
