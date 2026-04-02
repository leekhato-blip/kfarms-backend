package com.kfarms.platform.service;

import com.kfarms.entity.AppUser;
import com.kfarms.entity.Role;
import com.kfarms.repository.AppUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@Order(110)
@RequiredArgsConstructor
public class PlatformDefaultOwnerBootstrapRunner implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${kfarms.platform.default-owner.username:kato}")
    private String defaultOwnerUsername;

    @Value("${kfarms.platform.default-owner.email:leekhato@gmail.com}")
    private String defaultOwnerEmail;

    @Value("${kfarms.platform.default-owner.password:9090}")
    private String defaultOwnerPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String username = StringUtils.trimWhitespace(defaultOwnerUsername);
        String email = StringUtils.trimWhitespace(defaultOwnerEmail);
        String password = defaultOwnerPassword == null ? "" : defaultOwnerPassword.trim();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(email) || !StringUtils.hasText(password)) {
            return;
        }

        AppUser user = appUserRepository.findByEmail(email)
                .or(() -> appUserRepository.findByUsername(username))
                .orElseGet(AppUser::new);

        boolean isNewUser = user.getId() == null;
        boolean changed = isNewUser;

        if (!username.equals(user.getUsername())) {
            user.setUsername(username);
            changed = true;
        }

        if (!email.equalsIgnoreCase(StringUtils.trimWhitespace(user.getEmail()))) {
            user.setEmail(email.toLowerCase());
            changed = true;
        }

        if (user.getRole() != Role.PLATFORM_ADMIN) {
            user.setRole(Role.PLATFORM_ADMIN);
            changed = true;
        }

        if (!user.isPlatformAccess()) {
            user.setPlatformAccess(true);
            changed = true;
        }

        if (!user.isEnabled()) {
            user.setEnabled(true);
            changed = true;
        }

        if (!StringUtils.hasText(user.getPassword()) || !passwordEncoder.matches(password, user.getPassword())) {
            user.setPassword(passwordEncoder.encode(password));
            changed = true;
        }

        if (!changed) {
            return;
        }

        appUserRepository.save(user);
        log.info("Bootstrapped ROOTS default platform owner account for {}.", email);
    }
}
