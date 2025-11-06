package com.kfarms.security;

import com.kfarms.entity.AppUser;
import com.kfarms.repository.AppUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Primary
public class CustomUserDetailsService implements UserDetailsService {
    private final AppUserRepository userRepo;

    @Override
    public UserDetails loadUserByUsername(String input) throws UsernameNotFoundException {
        AppUser user = userRepo.findByEmail(input)
                .or(() -> userRepo.findByUsername(input))
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return User.withUsername(user.getEmail()) // email is main identity
                .password(user.getPassword())
                .roles(user.getRole().name()) // e.g. ADMIN
                .build();
    }


}
