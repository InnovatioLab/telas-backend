package com.telas.security;

import com.telas.infra.config.RateLimitProperties;
import com.telas.infra.security.filters.IpRateLimiterFilter;
import com.telas.infra.security.filters.MonitoringApiKeyFilter;
import com.telas.infra.security.filters.SecurityFilter;
import com.telas.infra.security.model.TokenData;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.infra.security.services.TokenService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import com.telas.enums.Permission;
import com.telas.infra.security.model.AuthenticatedUser;

import java.util.UUID;

@TestConfiguration
class TestSecuritySupportConfig {

  @Bean
  MonitoringApiKeyFilter monitoringApiKeyFilter() {
    return new MonitoringApiKeyFilter();
  }

  @Bean
  IpRateLimiterFilter ipRateLimiterFilter() {
    RateLimitProperties props = new RateLimitProperties();
    props.setEnabled(false);
    return new IpRateLimiterFilter(props);
  }

  @Bean
  TokenService tokenService() {
    return new TokenService() {
      @Override
      public String generateToken(com.telas.entities.Client client) {
        return "test-token";
      }

      @Override
      public TokenData validateToken(String token) {
        return null;
      }
    };
  }

  @Bean
  UserDetailsService userDetailsService() {
    return username -> {
      throw new UnsupportedOperationException("Not used in security endpoint tests");
    };
  }

  @Bean
  AuthenticatedUserService authenticatedUserService() {
    return new AuthenticatedUserService() {
      @Override
      public AuthenticatedUser getLoggedUser() {
        throw new UnsupportedOperationException("Not used in security endpoint tests");
      }

      @Override
      public AuthenticatedUser validateSelfOrAdmin(UUID id) {
        throw new UnsupportedOperationException("Not used in security endpoint tests");
      }

      @Override
      public AuthenticatedUser validateAdmin() {
        throw new UnsupportedOperationException("Not used in security endpoint tests");
      }

      @Override
      public AuthenticatedUser validateDeveloper() {
        throw new UnsupportedOperationException("Not used in security endpoint tests");
      }

      @Override
      public void validatePermission(Permission permission) {
        throw new UnsupportedOperationException("Not used in security endpoint tests");
      }

      @Override
      public AuthenticatedUser validateActiveSubscription() {
        throw new UnsupportedOperationException("Not used in security endpoint tests");
      }

      @Override
      public void verifyTermsAccepted(UserDetails user) {
        throw new UnsupportedOperationException("Not used in security endpoint tests");
      }
    };
  }

  @Bean
  SecurityFilter securityFilter(
      TokenService tokenService,
      UserDetailsService userDetailsService,
      AuthenticatedUserService authenticatedUserService) {
    return new SecurityFilter(tokenService, userDetailsService, authenticatedUserService);
  }
}

