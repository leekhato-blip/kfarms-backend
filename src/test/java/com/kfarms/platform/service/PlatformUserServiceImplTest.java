package com.kfarms.platform.service;

import com.kfarms.entity.AppUser;
import com.kfarms.entity.Role;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.tenant.repository.TenantMemberRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PlatformUserServiceImplTest {

    private final AppUserRepository appUserRepository = mock(AppUserRepository.class);
    private final TenantMemberRepository tenantMemberRepository = mock(TenantMemberRepository.class);
    private final PlatformUserServiceImpl service =
            new PlatformUserServiceImpl(appUserRepository, tenantMemberRepository, null, null, null);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void preventsChangingYourOwnPlatformRole() {
        AppUser actor = platformAdmin(7L, "root@example.com", true);
        authenticateAs(actor.getEmail());

        when(appUserRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.setPlatformAdmin(actor.getId(), false)
        );

        assertEquals("Use another platform admin account to change your own platform role.", error.getMessage());
        verify(appUserRepository, never()).save(actor);
    }

    @Test
    void preventsChangingYourOwnEnabledStatus() {
        AppUser actor = platformAdmin(7L, "root@example.com", true);
        authenticateAs(actor.getEmail());

        when(appUserRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(actor.getId())).thenReturn(Optional.of(actor));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.setUserEnabled(actor.getId(), false)
        );

        assertEquals("Use another platform admin account to change your own sign-in access.", error.getMessage());
        verify(appUserRepository, never()).save(actor);
    }

    @Test
    void preventsRemovingLastEnabledPlatformAdmin() {
        AppUser actor = standardUser(3L, "operator@example.com", true);
        AppUser target = platformAdmin(11L, "solo-admin@example.com", true);
        authenticateAs(actor.getEmail());

        when(appUserRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(appUserRepository.countByRoleAndEnabledTrue(Role.PLATFORM_ADMIN)).thenReturn(1L);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.setPlatformAdmin(target.getId(), false)
        );

        assertEquals("At least one enabled platform admin must remain.", error.getMessage());
        verify(appUserRepository, never()).save(target);
    }

    @Test
    void allowsChangingAnotherPlatformAdminWhenCoverageRemains() {
        AppUser actor = platformAdmin(2L, "owner@example.com", true);
        AppUser target = platformAdmin(5L, "admin@example.com", true);
        authenticateAs(actor.getEmail());

        when(appUserRepository.findByEmail(actor.getEmail())).thenReturn(Optional.of(actor));
        when(appUserRepository.findById(target.getId())).thenReturn(Optional.of(target));
        when(appUserRepository.countByRoleAndEnabledTrue(Role.PLATFORM_ADMIN)).thenReturn(2L);

        assertDoesNotThrow(() -> service.setPlatformAdmin(target.getId(), false));

        assertEquals(Role.USER, target.getRole());
        verify(appUserRepository).save(target);
    }

    private void authenticateAs(String principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        principal,
                        "secret",
                        List.of(new SimpleGrantedAuthority("ROLE_PLATFORM_ADMIN"))
                )
        );
    }

    private AppUser platformAdmin(Long id, String email, boolean enabled) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(email);
        user.setEmail(email);
        user.setRole(Role.PLATFORM_ADMIN);
        user.setEnabled(enabled);
        return user;
    }

    private AppUser standardUser(Long id, String email, boolean enabled) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(email);
        user.setEmail(email);
        user.setRole(Role.USER);
        user.setEnabled(enabled);
        return user;
    }
}
