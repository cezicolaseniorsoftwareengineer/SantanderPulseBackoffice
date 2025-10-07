package com.santander.pulse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Santander Pulse Banking Application
 * 
 * Main application class with H2 database and JWT authentication.
 * Implements banking-grade security and audit capabilities.
 * 
 * @author Cezi Cola Senior Software Engineer
 * @version 1.0.0
 * @since 2025-10-03
 */
@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
@EnableWebSecurity
public class SantanderPulseApplication {

    public static void main(String[] args) {
        SpringApplication.run(SantanderPulseApplication.class, args);
        
        System.out.println("\n" +
            "========================================\n" +
            "  SANTANDER PULSE BACKEND STARTED     \n" +
            "========================================\n" +
            "  H2 Console: http://localhost:8080/api/h2-console\n" +
            "  API Docs: http://localhost:8080/api/swagger-ui.html\n" +
            "  Health: http://localhost:8080/api/actuator/health\n" +
            "========================================");
    }
}