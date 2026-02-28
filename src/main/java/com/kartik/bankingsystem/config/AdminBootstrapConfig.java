package com.kartik.bankingsystem.config;

import com.kartik.bankingsystem.entity.Role;
import com.kartik.bankingsystem.entity.User;
import com.kartik.bankingsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminBootstrapConfig {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.admin.enabled:true}")
    private boolean enabled;

    @Value("${app.bootstrap.admin.full-name:Platform Admin}")
    private String adminFullName;

    @Value("${app.bootstrap.admin.email:admin@banking.local}")
    private String adminEmail;

    @Value("${app.bootstrap.admin.password:Admin@12345}")
    private String adminPassword;

    @Bean
    public ApplicationRunner bootstrapAdminUser() {
        return args -> {
            if (!enabled) {
                log.info("Admin bootstrap disabled");
                return;
            }

            String normalizedEmail = adminEmail.trim().toLowerCase();
            if (userRepository.existsByEmail(normalizedEmail)) {
                log.info("Admin user already exists: {}", normalizedEmail);
                return;
            }

            User admin = User.builder()
                    .fullName(adminFullName)
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(adminPassword))
                    .roles(Set.of(Role.ADMIN, Role.CUSTOMER))
                    .active(true)
                    .build();

            userRepository.save(admin);
            log.info("Bootstrapped admin user: {}", normalizedEmail);
        };
    }
}
