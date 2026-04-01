package com.kfarms.tenant.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(50)
@RequiredArgsConstructor
public class TenantSchemaRepairRunner implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        runSafely("inventory tenant uniqueness", this::repairInventoryUniqueness);
        runSafely("fish pond tenant uniqueness", this::repairFishPondUniqueness);
    }

    private void repairInventoryUniqueness() {
        dropLegacyUniqueConstraints(
                "inventory",
                "UNIQUE (item_name, category)"
        );
        ensureUniqueConstraint(
                "inventory",
                "uk_inventory_tenant_item_category",
                "tenant_id, item_name, category"
        );
    }

    private void repairFishPondUniqueness() {
        dropLegacyUniqueConstraints(
                "fish_pond",
                "UNIQUE (pond_name)"
        );
        ensureUniqueConstraint(
                "fish_pond",
                "uk_fish_pond_tenant_name",
                "tenant_id, pond_name"
        );
    }

    private void dropLegacyUniqueConstraints(String tableName, String definition) {
        String sql = """
                DO $$
                DECLARE
                    constraint_name text;
                BEGIN
                    FOR constraint_name IN
                        SELECT c.conname
                        FROM pg_constraint c
                        JOIN pg_class t ON t.oid = c.conrelid
                        JOIN pg_namespace n ON n.oid = t.relnamespace
                        WHERE c.contype = 'u'
                          AND t.relname = '%s'
                          AND n.nspname = current_schema()
                          AND pg_get_constraintdef(c.oid) = '%s'
                    LOOP
                        EXECUTE format('ALTER TABLE %%I.%%I DROP CONSTRAINT %%I', current_schema(), '%s', constraint_name);
                    END LOOP;
                END $$;
                """.formatted(tableName, definition, tableName);
        jdbcTemplate.execute(sql);
    }

    private void ensureUniqueConstraint(String tableName, String constraintName, String columnList) {
        Integer existing = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM pg_constraint c
                JOIN pg_class t ON t.oid = c.conrelid
                JOIN pg_namespace n ON n.oid = t.relnamespace
                WHERE c.contype = 'u'
                  AND c.conname = ?
                  AND t.relname = ?
                  AND n.nspname = current_schema()
                """,
                Integer.class,
                constraintName,
                tableName
        );

        if (existing != null && existing > 0) {
            return;
        }

        if (hasDuplicateRows(tableName, columnList)) {
            log.warn(
                    "Skipping startup constraint {} on {} because duplicate rows already exist for {}. " +
                            "The application will continue booting, but the data should be cleaned up before retrying.",
                    constraintName,
                    tableName,
                    columnList
            );
            return;
        }

        jdbcTemplate.execute(
                "ALTER TABLE %s ADD CONSTRAINT %s UNIQUE (%s)"
                        .formatted(tableName, constraintName, columnList)
        );
        log.info("Applied tenant-aware unique constraint {} on {}", constraintName, tableName);
    }

    private boolean hasDuplicateRows(String tableName, String columnList) {
        Integer duplicateGroupCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM (
                    SELECT %s
                    FROM %s
                    GROUP BY %s
                    HAVING COUNT(*) > 1
                ) duplicates
                """.formatted(columnList, tableName, columnList),
                Integer.class
        );
        return duplicateGroupCount != null && duplicateGroupCount > 0;
    }

    private void runSafely(String repairName, Runnable repair) {
        try {
            repair.run();
        } catch (DataAccessException ex) {
            log.warn("Skipping {} during startup because the live database is not ready for that repair yet.", repairName, ex);
        }
    }
}
