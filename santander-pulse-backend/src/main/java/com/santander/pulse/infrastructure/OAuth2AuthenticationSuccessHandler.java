package com.santander.pulse.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.santander.pulse.application.dto.AuthResponse;
import com.santander.pulse.domain.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

/**
 * Handles successful OAuth2 authentication by issuing JWT tokens and redirecting to the frontend.
 */
@Component
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Value("${app.oauth2.callback-path:/oauth2/callback}")
    private String oauth2CallbackPath;

    public OAuth2AuthenticationSuccessHandler(
            JwtService jwtService,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper,
            HttpCookieOAuth2AuthorizationRequestRepository authorizationRequestRepository
    ) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.authorizationRequestRepository = authorizationRequestRepository;
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            logger.warn("Unexpected authentication type: {}", authentication.getClass().getName());
            super.onAuthenticationSuccess(request, response, authentication);
            return;
        }

        OAuth2User oAuth2User = oauthToken.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = getAttribute(attributes, "email");
        String name = getAttribute(attributes, "name");
        String subject = getAttribute(attributes, "sub");

        String resolvedEmail = email;
        if (!StringUtils.hasText(resolvedEmail) && StringUtils.hasText(subject)) {
            resolvedEmail = subject + "@googleusercontent.com";
        }

        if (!StringUtils.hasText(resolvedEmail)) {
            logger.error("Google OAuth response did not contain an email address");
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unable to retrieve email from Google account");
            return;
        }

        final String emailToUse = resolvedEmail;

        User user = userRepository.findByEmail(emailToUse)
                .orElseGet(() -> createUserFromOAuth(emailToUse, name));

        if (StringUtils.hasText(name) && !name.equals(user.getFullName())) {
            user.setFullName(name);
            user = userRepository.save(user);
        }

        String accessToken = jwtService.generateToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

    AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getFullName(),
        user.getRole().name(),
        user.getCpf()
    );

    String userPayload = Base64.getUrlEncoder()
        .encodeToString(objectMapper.writeValueAsBytes(userInfo));

    String callbackUrl = determineRedirectTarget(request);

    String targetUrl = UriComponentsBuilder.fromUriString(callbackUrl)
        .queryParam("accessToken", accessToken)
        .queryParam("refreshToken", refreshToken)
        .queryParam("expiresIn", jwtService.getJwtExpiration())
        .queryParam("user", userPayload)
        .build(true)
        .toUriString();

        logger.info("OAuth2 login succeeded for email: {}", emailToUse);
        authorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private User createUserFromOAuth(String email, String name) {
        User user = new User(
                email,
                email,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                StringUtils.hasText(name) ? name : "Google User"
        );
        user.setRole(User.Role.USER);
        user.setCpf(null);
        return userRepository.save(user);
    }

    private String determineRedirectTarget(HttpServletRequest request) {
        String defaultTarget = resolveFrontendCallbackUrl();

        return CookieUtils.getCookie(request, HttpCookieOAuth2AuthorizationRequestRepository.OAUTH2_REDIRECT_URI_COOKIE_NAME)
                .map(Cookie::getValue)
                .filter(this::isAuthorizedRedirectUri)
                .orElse(defaultTarget);
    }

    private String resolveFrontendCallbackUrl() {
        String base = this.frontendUrl != null ? this.frontendUrl.trim() : "http://localhost:4200";
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }

        String configuredPath = StringUtils.hasText(oauth2CallbackPath) ? oauth2CallbackPath.trim() : "/oauth2/callback";
        if (!configuredPath.startsWith("/")) {
            configuredPath = "/" + configuredPath;
        }

        return base + configuredPath;
    }

    private boolean isAuthorizedRedirectUri(String uri) {
        if (!StringUtils.hasText(uri)) {
            return false;
        }

        try {
            URI clientRedirect = new URI(uri);
            URI authorizedRedirect = new URI(resolveFrontendCallbackUrl());

            boolean hostsMatch = clientRedirect.getHost() != null
                    && clientRedirect.getHost().equalsIgnoreCase(authorizedRedirect.getHost());

            boolean portsMatch = resolvePort(clientRedirect) == resolvePort(authorizedRedirect);

            boolean schemesMatch = clientRedirect.getScheme() != null
                    && clientRedirect.getScheme().equalsIgnoreCase(authorizedRedirect.getScheme());

            return hostsMatch && portsMatch && schemesMatch;
        } catch (URISyntaxException ex) {
            logger.warn("Invalid redirect URI received in OAuth2 flow: {}", uri, ex);
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

    private String getAttribute(Map<String, Object> attributes, String key) {
        Object value = attributes.get(key);
        return value != null ? value.toString() : null;
    }
}
