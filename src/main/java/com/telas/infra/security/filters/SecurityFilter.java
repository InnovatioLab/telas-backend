package com.telas.infra.security.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telas.dtos.response.ResponseDto;
import com.telas.infra.security.services.AuthenticatedUserService;
import com.telas.infra.security.services.TokenService;
import com.telas.shared.constants.AllowedEndpointsConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
public class SecurityFilter extends OncePerRequestFilter {
    private final TokenService tokenService;
    private final UserDetailsService userDetailsService;
    private final AuthenticatedUserService authenticatedUserService;

    public SecurityFilter(TokenService tokenService, UserDetailsService userDetailsService, AuthenticatedUserService authenticatedUserService) {
        this.tokenService = tokenService;
        this.userDetailsService = userDetailsService;
        this.authenticatedUserService = authenticatedUserService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String idToken = recoverToken(request);
        String requestUri = request.getRequestURI();
        HttpMethod requestMethod = HttpMethod.valueOf(request.getMethod());
        boolean acceptTermsURL = "/api/clients/accept-terms-conditions".equals(requestUri);
        String fixedRequest = requestUri.replace("/api", "");
        boolean allowedURL = AllowedEndpointsConstants.isAllowedURL(requestMethod, fixedRequest);


        if (allowedURL) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            if (idToken != null) {
                String identificationNumber = tokenService.validateToken(idToken);
                UserDetails user = userDetailsService.loadUserByUsername(identificationNumber);

                Authentication authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                if (!acceptTermsURL) {
                    authenticatedUserService.verifyTermsAccepted(user);
                }
            }

            filterChain.doFilter(request, response);
        } catch (RuntimeException exception) {
            String errorMessage = exception.getMessage();
            logger.error("Exception: " + errorMessage);
            handleException(response, HttpStatus.BAD_REQUEST, errorMessage);
        }
    }

    private void handleException(HttpServletResponse response, HttpStatus status, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        ResponseDto<Object> obj = ResponseDto.fromData(null, status, message, Arrays.asList(message));
        response.getWriter().write(new ObjectMapper().writeValueAsString(obj));
    }

    private String recoverToken(HttpServletRequest request) {
        var authHeader = request.getHeader("Authorization");

        if (authHeader == null) {
            return null;
        }

        return authHeader.replace("Bearer ", "");
    }
}
