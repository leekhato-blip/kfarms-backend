package com.kfarms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.web.cors")
public class KfarmsCorsProperties {

    private List<String> allowedOrigins = new ArrayList<>(List.of(
            "http://localhost:5173",
            "http://127.0.0.1:5173",
            "http://localhost:5174",
            "http://127.0.0.1:5174"
    ));
    private List<String> allowedOriginPatterns = new ArrayList<>();
    private List<String> allowedMethods = new ArrayList<>(List.of(
            "GET",
            "POST",
            "PATCH",
            "PUT",
            "DELETE",
            "OPTIONS"
    ));
    private List<String> allowedHeaders = new ArrayList<>(List.of(
            "Authorization",
            "Content-Type",
            "X-Tenant-Id"
    ));
    private List<String> exposedHeaders = new ArrayList<>(List.of("Set-Cookie"));
}
