package com.telas.infra.config;

import com.telas.infra.security.filters.IpRateLimiterFilter;
import com.telas.infra.security.filters.MonitoringApiKeyFilter;
import com.telas.infra.security.filters.SecurityFilter;
import com.telas.shared.constants.AllowedEndpointsConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final SecurityFilter securityFilter;
    private final MonitoringApiKeyFilter monitoringApiKeyFilter;
    private final IpRateLimiterFilter ipRateLimiterFilter;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests(auth -> {
                    AllowedEndpointsConstants.getAllowedEndpoints().forEach((method, routes) -> routes.forEach(route -> {
                        auth.requestMatchers(method, route).permitAll();
                        auth.requestMatchers(method, "/api" + route).permitAll();
                    }));
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(monitoringApiKeyFilter, SecurityFilter.class)
                .addFilterBefore(ipRateLimiterFilter, MonitoringApiKeyFilter.class)
                .sessionManagement(conf -> conf.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
