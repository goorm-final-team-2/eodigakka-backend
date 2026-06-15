package com.eodigakka.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Set;
import java.util.TreeSet;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class InitialSchemaMigrationTest {

  private static final Set<String> EXPECTED_TABLES =
      Set.of(
          "users",
          "appointments",
          "appointment_members",
          "place_candidates",
          "votes",
          "confirmed_places",
          "member_locations");

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

  @Test
  void appliesInitialSchemaMigration() throws Exception {
    Flyway flyway =
        Flyway.configure()
            .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
            .locations("classpath:db/migration")
            .load();

    flyway.migrate();

    try (Connection connection = postgres.createConnection("");
        Statement statement = connection.createStatement()) {
      assertThat(readApplicationTables(statement))
          .containsExactlyInAnyOrderElementsOf(EXPECTED_TABLES);
      assertThat(isMigrationApplied(statement, "2")).isTrue();
    }
  }

  private Set<String> readApplicationTables(Statement statement) throws Exception {
    Set<String> tables = new TreeSet<>();
    try (ResultSet resultSet =
        statement.executeQuery(
            """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
              AND table_name <> 'flyway_schema_history'
            """)) {
      while (resultSet.next()) {
        tables.add(resultSet.getString("table_name"));
      }
    }
    return tables;
  }

  private boolean isMigrationApplied(Statement statement, String version) throws Exception {
    try (ResultSet resultSet =
        statement.executeQuery(
            """
            SELECT success
            FROM flyway_schema_history
            WHERE version = '%s'
            """
                .formatted(version))) {
      return resultSet.next() && resultSet.getBoolean("success");
    }
  }
}
