package io.github.developeranalytics.persistence;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
@Tag("persistence")
class FlywayMigrationTest {

    @Inject DataSource dataSource;

    @Test
    void flywayCreatesBaselineSchemaInFreshDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet tableResult = statement.executeQuery("SELECT to_regclass('public.application_metadata') IS NOT NULL")) {
            assertTrue(tableResult.next()); assertTrue(tableResult.getBoolean(1));
        }
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet weeklyTable = statement.executeQuery("SELECT to_regclass('public.repository_user_activity_week') IS NOT NULL")) {
            assertTrue(weeklyTable.next()); assertFalse(weeklyTable.getBoolean(1));
        }
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet fileChangesTable = statement.executeQuery("SELECT to_regclass('public.contribution_file_change') IS NOT NULL")) {
            assertTrue(fileChangesTable.next()); assertTrue(fileChangesTable.getBoolean(1));
        }
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet valueResult = statement.executeQuery("SELECT metadata_value FROM application_metadata WHERE metadata_key = 'schema_baseline'")) {
            assertTrue(valueResult.next()); assertEquals("1", valueResult.getString(1));
        }
    }

    @Test
    void flywayHistoryContainsOnlySuccessfulMigrations() throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "SELECT count(*) FILTER (WHERE success = false), count(*) FILTER (WHERE version IS NOT NULL) FROM flyway_schema_history")) {
            assertTrue(result.next()); assertEquals(0, result.getInt(1)); assertEquals(41, result.getInt(2));
        }
    }

    @Test
    void everySingleColumnForeignKeyHasLeadingIndex() throws Exception {
        String sql = """
                SELECT cls.relname, att.attname, con.conname
                FROM pg_constraint con
                JOIN pg_class cls ON cls.oid = con.conrelid
                JOIN pg_namespace ns ON ns.oid = cls.relnamespace
                JOIN pg_attribute att
                  ON att.attrelid = con.conrelid
                 AND att.attnum = con.conkey[1]
                WHERE con.contype = 'f'
                  AND array_length(con.conkey, 1) = 1
                  AND ns.nspname = current_schema()
                  AND NOT EXISTS (
                      SELECT 1
                      FROM pg_index idx
                      WHERE idx.indrelid = con.conrelid
                        AND idx.indisvalid
                        AND idx.indisready
                        AND idx.indkey[0] = con.conkey[1]
                  )
                ORDER BY cls.relname, att.attname
                """;
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertTrue(!result.next(), "Every single-column foreign key must have an index with the FK column first");
        }
    }
}
