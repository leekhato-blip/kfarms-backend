package com.kfarms.tenant.service;

import com.kfarms.demo.DemoAccountSupport;
import com.kfarms.entity.AppUser;
import com.kfarms.entity.Role;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantMember;
import com.kfarms.tenant.entity.TenantRole;
import com.kfarms.tenant.repository.TenantMemberRepository;
import com.kfarms.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(210)
@RequiredArgsConstructor
@ConditionalOnProperty(name = "kfarms.demo.seed-viewer", havingValue = "true")
public class DemoViewerAccountSeeder implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final TenantMemberRepository tenantMemberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Tenant demoTenant = tenantRepository
                .findBySlugIgnoreCase(DemoAccountSupport.DEMO_VIEWER_TENANT_SLUG)
                .orElse(null);

        if (demoTenant == null) {
            log.info(
                    "Demo viewer bootstrap skipped because tenant '{}' was not found.",
                    DemoAccountSupport.DEMO_VIEWER_TENANT_SLUG
            );
            return;
        }

        AppUser demoUser = appUserRepository.findByEmail(DemoAccountSupport.DEMO_VIEWER_EMAIL)
                .orElseGet(() -> appUserRepository.findByUsername(DemoAccountSupport.DEMO_VIEWER_USERNAME)
                        .orElseGet(AppUser::new));

        demoUser.setUsername(DemoAccountSupport.DEMO_VIEWER_USERNAME);
        demoUser.setEmail(DemoAccountSupport.DEMO_VIEWER_EMAIL);
        demoUser.setPassword(passwordEncoder.encode(DemoAccountSupport.DEMO_VIEWER_PASSWORD));
        demoUser.setRole(Role.USER);
        demoUser.setEnabled(true);
        AppUser savedUser = appUserRepository.save(demoUser);

        TenantMember membership = tenantMemberRepository
                .findByTenant_IdAndUser_Id(demoTenant.getId(), savedUser.getId())
                .orElseGet(TenantMember::new);

        membership.setTenant(demoTenant);
        membership.setUser(savedUser);
        membership.setRole(TenantRole.MANAGER);
        membership.setActive(true);
        membership.setLandingPage("/dashboard");
        membership.setThemePreference("SYSTEM");
        membership.setEmailNotifications(false);
        membership.setPushNotifications(false);
        membership.setWeeklySummary(false);
        membership.setCompactTables(false);
        tenantMemberRepository.save(membership);

        log.info("Demo viewer account is ready for tenant '{}'.", demoTenant.getSlug());
    }
}
