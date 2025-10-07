package com.santander.pulse.infrastructure;

import com.santander.pulse.domain.Customer;
import com.santander.pulse.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Data initializer to populate the database with test data when explicitly enabled.
 * Creates default users and sample customers for development environments.
 */
@Component
@ConditionalOnProperty(value = "pulse.seed-data.enabled", havingValue = "true")
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            CustomerRepository customerRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.customerRepository = customerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeData() {
        logger.info("Initializing database with test data...");

        createDefaultUsers();
        createSampleCustomers();

        logger.info("Database initialization completed successfully");
    }

    private void createDefaultUsers() {
        // Create admin user
        if (!userRepository.existsByCpf("11122233344")) {
            User admin = new User(
                "11122233344",
                "admin@santander.com",
                passwordEncoder.encode("admin123"),
                "System Administrator"
            );
            admin.setRole(User.Role.ADMIN);
            admin.setCpf("11122233344");
            userRepository.save(admin);
            logger.info("Created admin user: CPF 11122233344 / admin123");
        }

        // Create manager user
        if (!userRepository.existsByCpf("55566677788")) {
            User manager = new User(
                "55566677788",
                "manager@santander.com",
                passwordEncoder.encode("manager123"),
                "Bank Manager"
            );
            manager.setRole(User.Role.MANAGER);
            manager.setCpf("55566677788");
            userRepository.save(manager);
            logger.info("Created manager user: CPF 55566677788 / manager123");
        }

        // Create regular user
        if (!userRepository.existsByCpf("99988877766")) {
            User user = new User(
                "99988877766",
                "user@santander.com",
                passwordEncoder.encode("user123"),
                "Bank User"
            );
            user.setRole(User.Role.USER);
            user.setCpf("99988877766");
            userRepository.save(user);
            logger.info("Created regular user: CPF 99988877766 / user123");
        }
    }

    private void createSampleCustomers() {
        // Sample customer 1
        if (!customerRepository.existsByCpf("12345678901")) {
            Customer customer1 = new Customer(
                "Jo\u00e3o Silva Santos",
                "12345678901",
                "joao.silva@email.com",
                "(11) 99999-1234"
            );
            customerRepository.save(customer1);
            logger.info("Created sample customer: Jo\u00e3o Silva Santos");
        }

        // Sample customer 2
        if (!customerRepository.existsByCpf("98765432100")) {
            Customer customer2 = new Customer(
                "Maria Oliveira Costa",
                "98765432100",
                "maria.oliveira@email.com",
                "(11) 88888-5678"
            );
            customerRepository.save(customer2);
            logger.info("Created sample customer: Maria Oliveira Costa");
        }

        // Sample customer 3
        if (!customerRepository.existsByCpf("11122233344")) {
            Customer customer3 = new Customer(
                "Carlos Eduardo Ferreira",
                "11122233344",
                "carlos.eduardo@email.com",
                "(11) 77777-9012"
            );
            customerRepository.save(customer3);
            logger.info("Created sample customer: Carlos Eduardo Ferreira");
        }

        // Sample customer 4 (inactive)
        if (!customerRepository.existsByCpf("55566677788")) {
            Customer customer4 = new Customer(
                "Ana Paula Rodrigues",
                "55566677788",
                "ana.paula@email.com",
                "(11) 66666-3456"
            );
            customer4.deactivate();
            customerRepository.save(customer4);
            logger.info("Created sample customer (inactive): Ana Paula Rodrigues");
        }
    }
}