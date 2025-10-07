package com.santander.pulse.application.controller;

import com.santander.pulse.application.dto.AuthResponse;
import com.santander.pulse.application.dto.LoginRequest;
import com.santander.pulse.application.dto.RegisterRequest;
import com.santander.pulse.domain.User;
import com.santander.pulse.infrastructure.CustomUserDetailsService;
import com.santander.pulse.infrastructure.JwtService;
import com.santander.pulse.infrastructure.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Authentication controller for login, register and token management.
 * Implements banking-grade security and audit logging.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Authentication and user management endpoints")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final ClientRegistrationRepository clientRegistrationRepository;
    private final String defaultOAuthCallback;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            @Nullable ClientRegistrationRepository clientRegistrationRepository,
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl,
            @Value("${app.oauth2.callback-path:/oauth2/callback}") String callbackPath
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.defaultOAuthCallback = buildDefaultCallbackUri(frontendUrl, callbackPath);
    }

    @PostMapping("/login")
    @Operation(summary = "User login", description = "Authenticate user and return JWT tokens")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            logger.info("Login attempt for CPF: {}", loginRequest.getCpf());

            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getCpf(),
                    loginRequest.getPassword()
                )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            User user = (User) userDetails;

            String accessToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name(),
                user.getCpf()
            );

            AuthResponse response = new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.getJwtExpiration(),
                userInfo
            );

            logger.info("CPF {} logged in successfully", loginRequest.getCpf());
            return ResponseEntity.ok(response);

        } catch (AuthenticationException e) {
            logger.warn("Login failed for CPF: {} - {}", loginRequest.getCpf(), e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid credentials");
            error.put("message", "CPF or password is incorrect");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/register")
    @Operation(summary = "User registration", description = "Register a new user account")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            logger.info("Registration attempt for CPF: {}", registerRequest.getCpf());

            // Check if CPF already exists
            if (userRepository.existsByCpf(registerRequest.getCpf())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "CPF already exists");
                error.put("message", "CPF is already registered");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Check if email already exists
            if (userRepository.existsByEmail(registerRequest.getEmail())) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Email already exists");
                error.put("message", "Please use a different email address");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
            }

            // Create new user
            User user = new User(
                registerRequest.getCpf(),
                registerRequest.getEmail(),
                passwordEncoder.encode(registerRequest.getPassword()),
                registerRequest.getFullName()
            );
            user.setCpf(registerRequest.getCpf());
            user.setUsername(registerRequest.getCpf());

            User savedUser = userRepository.save(user);

            // Generate tokens
            String accessToken = jwtService.generateToken(savedUser);
            String refreshToken = jwtService.generateRefreshToken(savedUser);

            AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getFullName(),
                savedUser.getRole().name(),
                savedUser.getCpf()
            );

            AuthResponse response = new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.getJwtExpiration(),
                userInfo
            );

            logger.info("User with CPF {} registered successfully", registerRequest.getCpf());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("Registration failed for CPF: {} - {}", 
                        registerRequest.getCpf(), e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Registration failed");
            error.put("message", "Unable to create user account");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh token", description = "Generate new access token using refresh token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        try {
            if (refreshToken != null && refreshToken.startsWith("Bearer ")) {
                String token = refreshToken.substring(7);
                String username = jwtService.extractUsername(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(token, userDetails)) {
                    String newAccessToken = jwtService.generateToken(userDetails);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("accessToken", newAccessToken);
                    response.put("tokenType", "Bearer");
                    response.put("expiresIn", jwtService.getJwtExpiration());

                    return ResponseEntity.ok(response);
                }
            }

            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid refresh token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);

        } catch (Exception e) {
            logger.error("Token refresh failed: {}", e.getMessage());
            Map<String, String> error = new HashMap<>();
            error.put("error", "Token refresh failed");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @GetMapping("/providers")
    @Operation(summary = "Authentication providers", description = "List available authentication providers")
    public ResponseEntity<Map<String, Object>> getAuthProviders(HttpServletRequest request) {
        Map<String, Object> providers = new HashMap<>();

        Map<String, Object> googleInfo = new HashMap<>();
        Optional<ClientRegistration> googleRegistration = findGoogleRegistration();
        boolean googleEnabled = googleRegistration.filter(this::isRegistrationConfigured).isPresent();

        googleInfo.put("enabled", googleEnabled);
        googleRegistration.filter(this::isRegistrationConfigured).ifPresent(registration -> {
            googleInfo.put("authorizationUrl", buildAuthorizationUrl(request));
            googleInfo.put("redirectUri", resolveRedirectUri(request, registration));
            googleInfo.put("scopes", Set.copyOf(registration.getScopes()));
            googleInfo.put("postLoginRedirect", defaultOAuthCallback);
        });

        providers.put("google", googleInfo);

        Map<String, Object> response = new HashMap<>();
        response.put("providers", providers);
        return ResponseEntity.ok(response);
    }

    private Optional<ClientRegistration> findGoogleRegistration() {
        if (clientRegistrationRepository == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(clientRegistrationRepository.findByRegistrationId("google"));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private boolean isRegistrationConfigured(ClientRegistration registration) {
        if (registration == null) {
            return false;
        }

        String clientId = registration.getClientId();
        String clientSecret = registration.getClientSecret();

        return StringUtils.hasText(clientId)
                && StringUtils.hasText(clientSecret)
                && !hasPlaceholderValue(clientId)
                && !hasPlaceholderValue(clientSecret);
    }

    private String buildAuthorizationUrl(HttpServletRequest request) {
        String baseUrl = ServletUriComponentsBuilder.fromContextPath(request)
                .build()
                .toUriString();

        return ServletUriComponentsBuilder.fromUriString(baseUrl)
        .path("/login")
        .queryParam("provider", "google")
        .queryParam("redirect_uri", defaultOAuthCallback)
        .build(true)
        .toUriString();
    }

    private String resolveRedirectUri(HttpServletRequest request, ClientRegistration registration) {
        String baseUrl = ServletUriComponentsBuilder.fromContextPath(request)
                .build()
                .toUriString();

        String template = registration.getRedirectUri();
        if (!StringUtils.hasText(template)) {
            return baseUrl + "/login/oauth2/code/" + registration.getRegistrationId();
        }

        return template
                .replace("{baseUrl}", baseUrl)
                .replace("{registrationId}", registration.getRegistrationId())
                .replace("{action}", "login");
    }

    private String buildDefaultCallbackUri(String frontendUrl, String callbackPath) {
        String normalizedFront = StringUtils.hasText(frontendUrl) ? frontendUrl.trim() : "http://localhost:4200";
        if (normalizedFront.endsWith("/")) {
            normalizedFront = normalizedFront.substring(0, normalizedFront.length() - 1);
        }

        String path = StringUtils.hasText(callbackPath) ? callbackPath.trim() : "/oauth2/callback";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        return normalizedFront + path;
    }

    private boolean hasPlaceholderValue(String value) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        String normalized = value.trim().toLowerCase();
        return normalized.contains("dummy") || normalized.contains("changeme") || normalized.contains("placeholder");
    }
}