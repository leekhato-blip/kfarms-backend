package com.kfarms.platform.service;

import com.kfarms.entity.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(150)
@RequiredArgsConstructor
public class PlatformAccessBackfillRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        int updated = jdbcTemplate.update(
                """
                update app_user
                   set platform_access = true
                 where coalesce(platform_access, false) = false
                   and (role = ? or username like 'roots.%')
                """,
                Role.PLATFORM_ADMIN.name()
        );

        if (updated > 0) {
            log.info("Backfilled platform access for {} user(s).", updated);
        }
    }
}
