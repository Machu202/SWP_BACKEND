package com.mangastudio.backend.component;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Preserves chats created before the MAT naming change. JPA creates the new
 * table first; this runner copies every legacy row, verifies the copy, and
 * only then removes the old table. It is deliberately idempotent so an
 * interrupted deployment can safely retry on the next startup.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class DirectChatSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public DirectChatSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists("direct_chat_message")) return;
        if (!tableExists("mat_chat_message")) {
            System.err.println(">>> [MAT CHAT MIGRATION] New mat_chat_message table is not available; legacy chat was left untouched.");
            return;
        }

        try {
            jdbcTemplate.update("""
                    INSERT INTO mat_chat_message (sender_id, receiver_id, content, created_at, read_at)
                    SELECT legacy.sender_id, legacy.recipient_id, legacy.content, legacy.created_at, legacy.read_at
                    FROM direct_chat_message legacy
                    WHERE NOT EXISTS (
                        SELECT 1 FROM mat_chat_message migrated
                        WHERE migrated.sender_id = legacy.sender_id
                          AND migrated.receiver_id = legacy.recipient_id
                          AND migrated.content = legacy.content
                          AND migrated.created_at = legacy.created_at
                          AND (migrated.read_at = legacy.read_at
                               OR (migrated.read_at IS NULL AND legacy.read_at IS NULL))
                    )
                    """);

            Integer missingRows = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM direct_chat_message legacy
                    WHERE NOT EXISTS (
                        SELECT 1 FROM mat_chat_message migrated
                        WHERE migrated.sender_id = legacy.sender_id
                          AND migrated.receiver_id = legacy.recipient_id
                          AND migrated.content = legacy.content
                          AND migrated.created_at = legacy.created_at
                          AND (migrated.read_at = legacy.read_at
                               OR (migrated.read_at IS NULL AND legacy.read_at IS NULL))
                    )
                    """, Integer.class);

            if (missingRows != null && missingRows == 0) {
                jdbcTemplate.execute("DROP TABLE direct_chat_message");
                System.out.println(">>> [MAT CHAT MIGRATION] Legacy direct chat messages migrated successfully.");
            } else {
                System.err.println(">>> [MAT CHAT MIGRATION] Verification failed; legacy chat table was not removed.");
            }
        } catch (RuntimeException exception) {
            // Never delete the legacy table when copying or verification fails.
            System.err.println(">>> [MAT CHAT MIGRATION] Migration postponed safely: " + exception.getMessage());
        }
    }

    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE LOWER(table_name) = ?",
                Integer.class,
                tableName.toLowerCase());
        return count != null && count > 0;
    }
}
