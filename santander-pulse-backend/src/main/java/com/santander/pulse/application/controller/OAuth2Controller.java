package com.santander.pulse.application.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * OAuth2 login entry point aligned com o padrão adotado pelos "gênios":
 *  - quando o usuário clicar em "Entrar com Google", realizamos um 302
 *    direto para o endpoint de autorização do Spring Security
 *  - em caso de erro, redirecionamos de volta para o frontend com a flag
 *    apropriada
 *  - sob demanda (info=true) continuamos oferecendo o payload informativo
 */
@RestController
@RequestMapping("/login")
public class OAuth2Controller {

    private final String frontendUrl;
    private final String defaultCallbackUri;

    public OAuth2Controller(
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
            @Value("${app.oauth2.callback-path:/oauth2/callback}") String callbackPath
    ) {
        this.frontendUrl = normalizeFrontendUrl(frontendUrl);
        this.defaultCallbackUri = buildDefaultCallbackUri(this.frontendUrl, callbackPath);
    }

    @GetMapping
    public ResponseEntity<?> handleLogin(
            HttpServletRequest request,
            @RequestParam(required = false) String error,
            @RequestParam(required = false, defaultValue = "google") String provider,
            @RequestParam(required = false, name = "redirect_uri") String redirectUri,
            @RequestParam(required = false, defaultValue = "false") String info
    ) {
        if (Boolean.parseBoolean(info)) {
            return ResponseEntity.ok(Map.of(
                    "status", HttpStatus.OK.value(),
                    "message", "Utilize POST /auth/login para CPF/senha ou acesse /oauth2/authorization/google para login com Google.",
                    "providers", Map.of(
                            "cpf", Map.of(
                                    "type", "password",
                                    "endpoint", "/auth/login"
                            ),
                            "google", Map.of(
                                    "type", "oauth2",
                                    "endpoint", "/oauth2/authorization/google"
                            )
                    )
            ));
        }

        if ("true".equalsIgnoreCase(error)) {
            URI target = URI.create(frontendUrl + "/login?error=oauth_failed");
            return ResponseEntity.status(HttpStatus.FOUND).location(target).build();
        }

        if (!"google".equalsIgnoreCase(provider)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", HttpStatus.BAD_REQUEST.value(),
                    "error", "UnsupportedProvider",
                    "message", "Somente o provedor Google está habilitado neste endpoint."
            ));
        }

        String effectiveRedirectUri = StringUtils.hasText(redirectUri) && isAuthorizedRedirectUri(redirectUri)
                ? redirectUri
                : defaultCallbackUri;

        String authorizationUrl = ServletUriComponentsBuilder.fromContextPath(request)
                .path("/oauth2/authorization/google")
                .build(true)
                .toUriString();

        if (StringUtils.hasText(effectiveRedirectUri)) {
            authorizationUrl = UriComponentsBuilder.fromUriString(authorizationUrl)
                    .queryParam("redirect_uri", effectiveRedirectUri)
                    .build(true)
                    .toUriString();
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(authorizationUrl))
                .build();
    }

    private String normalizeFrontendUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return "http://localhost:4200";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private String buildDefaultCallbackUri(String normalizedFrontendUrl, String callbackPath) {
        String path = StringUtils.hasText(callbackPath) ? callbackPath.trim() : "/oauth2/callback";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return normalizedFrontendUrl + path;
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            return false;
        }

        try {
            URI candidate = new URI(uri);
            URI baseline = new URI(defaultCallbackUri);

            boolean hostsMatch = candidate.getHost() != null
                    && candidate.getHost().equalsIgnoreCase(baseline.getHost());
            boolean schemesMatch = candidate.getScheme() != null
                    && candidate.getScheme().equalsIgnoreCase(baseline.getScheme());
            boolean portsMatch = resolvePort(candidate) == resolvePort(baseline);

            return hostsMatch && schemesMatch && portsMatch;
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private int resolvePort(URI uri) {
        if (uri.getPort() != -1) {
            return uri.getPort();
        }
        if (uri.getScheme() == null) {
            return -1;
        }
        return switch (uri.getScheme().toLowerCase()) {
            case "http" -> 80;
            case "https" -> 443;
            default -> -1;
        };
    }
}