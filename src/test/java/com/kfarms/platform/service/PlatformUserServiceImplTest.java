package com.kfarms.platform.service;

import com.kfarms.entity.AppUser;
import com.kfarms.entity.Role;
import com.kfarms.platform.dto.PlatformUserListItemDto;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.tenant.repository.TenantMemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlatformUserServiceImplTest {

    @Test
    void searchUsersScopesResultsToPlatformAdmins() {
        Pageable pageable = PageRequest.of(0, 10);
        AppUser platformAdmin = platformAdmin(7L, "roots.ops", "platform.ops@demo.kfarms.local", true);

        AtomicReference<Role> requestedRole = new AtomicReference<>();
        AtomicReference<String> requestedSearch = new AtomicReference<>();
        AtomicReference<Pageable> requestedPageable = new AtomicReference<>();

        AppUserRepository appUserRepository = appUserRepository((role, search, currentPageable) -> {
            requestedRole.set(role);
            requestedSearch.set(search);
            requestedPageable.set(currentPageable);
            return new PageImpl<>(List.of(platformAdmin), currentPageable, 1);
        });

        TenantMemberRepository tenantMemberRepository = tenantMemberRepository(userId -> {
            assertEquals(7L, userId);
            return 3;
        });

        PlatformUserServiceImpl service =
                new PlatformUserServiceImpl(appUserRepository, tenantMemberRepository, null, null, null);

        Page<PlatformUserListItemDto> result = service.searchUsers("  roots  ", pageable);

        assertEquals(Role.PLATFORM_ADMIN, requestedRole.get());
        assertEquals("roots", requestedSearch.get());
        assertEquals(pageable, requestedPageable.get());
        assertEquals(1, result.getTotalElements());

        PlatformUserListItemDto user = result.getContent().get(0);
        assertEquals(7L, user.getId());
        assertEquals(Role.PLATFORM_ADMIN, user.getRole());
        assertEquals(3, user.getTenantCount());
        assertTrue(user.isActive());
    }

    @Test
    void searchUsersPassesNullSearchWhenInputIsBlank() {
        Pageable pageable = PageRequest.of(0, 10);
        AtomicReference<String> requestedSearch = new AtomicReference<>("not-null");

        AppUserRepository appUserRepository = appUserRepository((role, search, currentPageable) -> {
            requestedSearch.set(search);
            return Page.empty(currentPageable);
        });

        PlatformUserServiceImpl service =
                new PlatformUserServiceImpl(appUserRepository, tenantMemberRepository(userId -> 0), null, null, null);

        Page<PlatformUserListItemDto> result = service.searchUsers("   ", pageable);

        assertTrue(result.isEmpty());
        assertEquals(null, requestedSearch.get());
    }

    private AppUserRepository appUserRepository(SearchByRoleHandler handler) {
        return (AppUserRepository) Proxy.newProxyInstance(
                AppUserRepository.class.getClassLoader(),
                new Class[]{AppUserRepository.class},
                (proxy, method, args) -> {
                    if ("searchByRole".equals(method.getName())) {
                        return handler.handle((Role) args[0], (String) args[1], (Pageable) args[2]);
                    }

                    if ("toString".equals(method.getName())) {
                        return "AppUserRepositoryTestProxy";
                    }

                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }

                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }

                    throw new UnsupportedOperationException("Unexpected repository call: " + method.getName());
                }
        );
    }

    private TenantMemberRepository tenantMemberRepository(TenantCountHandler handler) {
        return (TenantMemberRepository) Proxy.newProxyInstance(
                TenantMemberRepository.class.getClassLoader(),
                new Class[]{TenantMemberRepository.class},
                (proxy, method, args) -> {
                    if ("countByUser_Id".equals(method.getName())) {
                        return handler.handle((Long) args[0]);
                    }

                    if ("toString".equals(method.getName())) {
                        return "TenantMemberRepositoryTestProxy";
                    }

                    if ("hashCode".equals(method.getName())) {
                        return System.identityHashCode(proxy);
                    }

                    if ("equals".equals(method.getName())) {
                        return proxy == args[0];
                    }

                    throw new UnsupportedOperationException("Unexpected repository call: " + method.getName());
                }
        );
    }

    private AppUser platformAdmin(Long id, String username, String email, boolean enabled) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setRole(Role.PLATFORM_ADMIN);
        user.setEnabled(enabled);
        return user;
    }

    @FunctionalInterface
    private interface SearchByRoleHandler {
        Page<AppUser> handle(Role role, String search, Pageable pageable);
    }

    @FunctionalInterface
    private interface TenantCountHandler {
        int handle(Long userId);
    }
}
