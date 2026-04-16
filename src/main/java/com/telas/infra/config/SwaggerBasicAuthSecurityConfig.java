package com.telas.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.core.Ordered;

@Configuration
@ConditionalOnProperty(prefix = "swagger.basic-auth", name = "enabled", havingValue = "true")
public class SwaggerBasicAuthSecurityConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain swaggerBasicAuthFilterChain(
            HttpSecurity http,
            CorsConfigurationSource corsConfigurationSource,
            PasswordEncoder passwordEncoder,
            @Value("${swagger.basic-auth.username:}") String username,
            @Value("${swagger.basic-auth.password:}") String password) throws Exception {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "swagger.basic-auth.enabled=true requires non-empty swagger.basic-auth.username and swagger.basic-auth.password");
        }
        UserDetails user = User.builder()
                .username(username.trim())
                .password(passwordEncoder.encode(password))
                .roles("SWAGGER")
                .build();
        InMemoryUserDetailsManager userDetailsManager = new InMemoryUserDetailsManager(user);
        return http
                .securityMatcher(new SwaggerUiBasicAuthRequestMatcher())
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsConfigurationSource))
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .userDetailsService(userDetailsManager)
                .build();
    }
}
