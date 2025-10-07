package com.santander.pulse.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.santander.pulse.domain.User;

/**
 * Ensures a default administrative user exists for local environments.
 * This prevents failed login attempts like "CPF or password is incorrect"
 * when the database starts empty and seeding is disabled.
 */
@Component
@ConditionalOnProperty(name = "app.bootstrap.default-admin.enabled", havingValue = "true", matchIfMissing = true)
public class DefaultAdminBootstrap {

    private static final Logger logger = LoggerFactory.getLogger(DefaultAdminBootstrap.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final String defaultAdminCpf;
    private final String defaultAdminPassword;
    private final String defaultAdminEmail;
    private final String defaultAdminFullName;

    public DefaultAdminBootstrap(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap.default-admin.cpf:11122233344}") String defaultAdminCpf,
            @Value("${app.bootstrap.default-admin.password:admin123}") String defaultAdminPassword,
            @Value("${app.bootstrap.default-admin.email:admin@santander.com}") String defaultAdminEmail,
            @Value("${app.bootstrap.default-admin.full-name:Santander Pulse Admin}") String defaultAdminFullName
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.defaultAdminCpf = defaultAdminCpf;
        this.defaultAdminPassword = defaultAdminPassword;
        this.defaultAdminEmail = defaultAdminEmail;
        this.defaultAdminFullName = defaultAdminFullName;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void ensureDefaultAdminUser() {
    boolean hasAdmin = !userRepository.findByRole(User.Role.ADMIN).isEmpty();

        if (hasAdmin) {
            logger.info("At least one ADMIN user already exists. Skipping default admin bootstrap.");
            return;
        }

        if (userRepository.existsByCpf(defaultAdminCpf)) {
            logger.info("CPF {} already present in user repository. Skipping default admin bootstrap.", maskCpf(defaultAdminCpf));
            return;
        }

    User admin = new User(
                defaultAdminCpf,
                defaultAdminEmail,
                passwordEncoder.encode(defaultAdminPassword),
                defaultAdminFullName
        );
        admin.setCpf(defaultAdminCpf);
    admin.setRole(User.Role.ADMIN);

        userRepository.save(admin);

        logger.info("Default ADMIN user created with CPF {}. Use the configured password to access the platform.", maskCpf(defaultAdminCpf));
    }

    private String maskCpf(String cpf) {
        if (cpf == null || cpf.length() != 11) {
            return cpf;
        }
        return cpf.substring(0, 3) + ".***.***-" + cpf.substring(9);
    }
}
