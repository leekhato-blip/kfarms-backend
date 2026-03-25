package com.kfarms.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kfarms.cookie")
public class KfarmsCookieProperties {

    private boolean secure = false;
    private String sameSite = "Lax";
    private String domain = "";
    private long maxAgeDays = 1;
}
