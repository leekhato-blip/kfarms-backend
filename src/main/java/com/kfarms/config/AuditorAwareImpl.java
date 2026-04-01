package com.kfarms.config;


import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

// @Component("auditorProvider") // This name matches what is referenced in @EnableJpaAuditing
public class AuditorAwareImpl implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")){
            return Optional.empty(); // Only audit when logged in
        }
        String username = auth.getName();
        System.out.println("✅ Auditor is: " + username);
        return Optional.of(username);
    }
}
