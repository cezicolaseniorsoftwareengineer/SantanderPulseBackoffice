package com.santander.pulse.infrastructure;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Request Filter to intercept and validate JWT tokens.
 * Implements banking-grade security validation and logging.
 */
@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String AUTHORIZATION_HEADER = "Authorization";

    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final String contextPath;

    public JwtRequestFilter(UserDetailsService userDetailsService,
                            JwtService jwtService,
                            @Value("${server.servlet.context-path:}") String contextPath) {
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.contextPath = contextPath != null ? contextPath.trim() : "";
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String requestTokenHeader = request.getHeader(AUTHORIZATION_HEADER);
        final String requestURI = request.getRequestURI();
        final String normalizedPath = normalizePath(requestURI);

        String username = null;
        String jwtToken = null;

        if (isPublicEndpoint(normalizedPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (requestTokenHeader != null && requestTokenHeader.startsWith(BEARER_PREFIX)) {
            jwtToken = requestTokenHeader.substring(BEARER_PREFIX.length());

            try {
                username = jwtService.extractUsername(jwtToken);
                logger.debug("JWT token extracted for user: {}", username);
            } catch (IllegalArgumentException e) {
                logger.warn("Unable to get JWT Token: {}", e.getMessage());
            } catch (ExpiredJwtException e) {
                logger.warn("JWT Token has expired: {}", e.getMessage());
            } catch (MalformedJwtException e) {
                logger.warn("JWT Token is malformed: {}", e.getMessage());
            } catch (UnsupportedJwtException e) {
                logger.warn("JWT Token is unsupported: {}", e.getMessage());
            } catch (SignatureException e) {
                logger.warn("JWT Token signature is invalid: {}", e.getMessage());
            } catch (Exception e) {
                logger.error("Unexpected error processing JWT token: {}", e.getMessage());
            }
        } else {
            logger.debug("JWT Token does not begin with Bearer String or is null");
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwtToken, userDetails)) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    logger.debug("User {} authenticated successfully", username);
                } else {
                    logger.warn("JWT Token validation failed for user: {}", username);
                }
            } catch (Exception e) {
                logger.error("Authentication failed for user {}: {}", username, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String normalizedPath) {
        return normalizedPath.startsWith("/auth/") ||
                normalizedPath.startsWith("/oauth2/") ||
                normalizedPath.startsWith("/login/oauth2/") ||
                normalizedPath.startsWith("/h2-console/") ||
                normalizedPath.startsWith("/swagger-ui/") ||
                normalizedPath.startsWith("/v3/api-docs/") ||
                normalizedPath.equals("/actuator/health");
    }

    private String normalizePath(String requestURI) {
        if (contextPath == null || contextPath.isBlank()) {
            return requestURI;
        }
        if (requestURI.startsWith(contextPath)) {
            return requestURI.substring(contextPath.length());
        }
        return requestURI;
    }
}