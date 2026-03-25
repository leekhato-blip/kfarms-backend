package com.kfarms.security;

import com.kfarms.entity.AppUser;
import com.kfarms.entity.Role;
import com.kfarms.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceTest {

    @Test
    void disabledUsersAreReturnedAsDisabledPrincipals() {
        AppUserRepository repository = mock(AppUserRepository.class);
        CustomUserDetailsService service = new CustomUserDetailsService(repository);

        AppUser user = new AppUser();
        user.setId(11L);
        user.setUsername("delta.admin");
        user.setEmail("delta@example.com");
        user.setPassword("encoded");
        user.setRole(Role.USER);
        user.setEnabled(false);

        when(repository.findByEmail("delta@example.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("delta@example.com");

        assertFalse(details.isEnabled());
        assertTrue(details.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_USER".equals(authority.getAuthority())));
    }
}
