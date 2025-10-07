package com.santander.pulse.infrastructure;

import com.santander.pulse.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Custom UserDetailsService for loading user details from database.
 * Implements banking-specific user loading and validation.
 */
@Service
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);
    
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        logger.debug("Loading user by username: {}", username);
        
    User user = userRepository.findByCpf(username)
        .or(() -> userRepository.findByUsername(username))
                .orElseThrow(() -> {
                    logger.warn("User not found: {}", username);
                    return new UsernameNotFoundException("User not found: " + username);
                });

        logger.debug("User loaded successfully: {} with role: {}", username, user.getRole());
        return user;
    }

    /**
     * Load user by email (alternative method)
     */
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        logger.debug("Loading user by email: {}", email);
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    logger.warn("User not found by email: {}", email);
                    return new UsernameNotFoundException("User not found with email: " + email);
                });

        logger.debug("User loaded by email successfully: {}", email);
        return user;
    }
}