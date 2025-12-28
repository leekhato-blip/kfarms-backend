package com.kfarms.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

@Configuration
public class AuditConfig {
    @Bean(name = "auditorProvider")
    public AuditorAware<String> auditorProvider(){
        System.out.println("✅ JPA Auditing Enabled!");
        return new AuditorAwareImpl();
    }
}
