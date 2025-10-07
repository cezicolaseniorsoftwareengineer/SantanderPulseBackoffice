package com.santander.pulse.infrastructure;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter to add security headers including Content-Security-Policy.
 * Follows OWASP recommendations for secure HTTP headers.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        
        // Content Security Policy - permite scripts inline necessários para Angular
        response.setHeader(
            "Content-Security-Policy",
            "default-src 'self'; " +
            "script-src 'self' 'unsafe-inline' 'unsafe-eval'; " +
            "style-src 'self' 'unsafe-inline'; " +
            "img-src 'self' data: https:; " +
            "font-src 'self' data:; " +
            "connect-src 'self' http://localhost:8080 http://localhost:4200; " +
            "frame-ancestors 'self'"
        );

        // X-Content-Type-Options: impede que o navegador tente adivinhar o tipo MIME
        response.setHeader("X-Content-Type-Options", "nosniff");

        // X-Frame-Options: protege contra clickjacking
        response.setHeader("X-Frame-Options", "SAMEORIGIN");

        // X-XSS-Protection: proteção adicional contra XSS (legado mas ainda útil)
        response.setHeader("X-XSS-Protection", "1; mode=block");

        // Referrer-Policy: controla informações de referência
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Permissions-Policy: controla acesso a APIs do navegador
        response.setHeader(
            "Permissions-Policy",
            "geolocation=(), microphone=(), camera=(), payment=()"
        );

        filterChain.doFilter(request, response);
    }
}
