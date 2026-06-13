package com.cinemamemory.api.user;

import com.cinemamemory.api.config.AppProperties;
import java.util.Locale;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
public class AdminAccountInitializer implements ApplicationRunner {
    private final AppProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountInitializer(
            AppProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppProperties.Admin admin = properties.admin();
        if (admin == null || !StringUtils.hasText(admin.email()) || !StringUtils.hasText(admin.password())) {
            return;
        }

        String email = admin.email().trim().toLowerCase(Locale.ROOT);
        String displayName = StringUtils.hasText(admin.displayName()) ? admin.displayName().trim() : "Administrator";

        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> userRepository.save(new User(
                        email,
                        passwordEncoder.encode(admin.password()),
                        displayName,
                        null,
                        UserRole.ADMIN
                )));

        if (user.getRole() != UserRole.ADMIN) {
            user.updateRole(UserRole.ADMIN);
        }

        if (!StringUtils.hasText(user.getPasswordHash())) {
            user.updatePasswordHash(passwordEncoder.encode(admin.password()));
        }
    }
}
