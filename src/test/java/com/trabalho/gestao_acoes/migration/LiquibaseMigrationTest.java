package com.trabalho.gestao_acoes.migration;

import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.ValidationFailedException;
import liquibase.exception.CommandExecutionException;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LiquibaseMigrationTest {
    private static final String CHANGELOG = "db/changelog/db.changelog-master.xml";

    @Test
    void emptyDatabaseMigratesOnceAndReleasesTheLock() throws Exception {
        try (Fixture fixture = fixture()) {
            fixture.liquibase.update();
            assertThat(fixture.scalar("SELECT COUNT(*) FROM DATABASECHANGELOG")).isEqualTo(4);
            assertThat(fixture.scalar("SELECT COUNT(*) FROM DATABASECHANGELOGLOCK WHERE LOCKED = FALSE")).isEqualTo(1);
            assertThat(fixture.tableExists("ACAO")).isTrue();
            assertThat(fixture.tableExists("CORRETORA")).isTrue();
            assertThat(fixture.tableExists("TRANSACAO")).isTrue();
            assertThat(fixture.tableExists("POSICAO_CARTEIRA")).isTrue();
            assertThat(fixture.tableExists("ADMIN_USER")).isTrue();

            fixture.liquibase.update();
            assertThat(fixture.scalar("SELECT COUNT(*) FROM DATABASECHANGELOG")).isEqualTo(4);
        }
    }

    @Test
    void changedChecksumFailsValidationWithoutClearingHistory() throws Exception {
        try (Fixture fixture = fixture()) {
            fixture.liquibase.update();
            fixture.execute("UPDATE DATABASECHANGELOG SET MD5SUM = '9:00000000000000000000000000000000' WHERE ID = '001-create-investment-tables'");
            assertThatThrownBy(fixture.liquibase::validate)
                    .isInstanceOf(CommandExecutionException.class)
                    .hasCauseInstanceOf(ValidationFailedException.class);
            assertThat(fixture.scalar("SELECT COUNT(*) FROM DATABASECHANGELOG")).isEqualTo(4);
        }
    }

    @Test
    void disposableInitialSchemaRollsBackAndCanBeAppliedAgain() throws Exception {
        try (Fixture fixture = fixture()) {
            fixture.liquibase.update();
            fixture.liquibase.rollback(4, "");
            assertThat(fixture.tableExists("ACAO")).isFalse();
            assertThat(fixture.tableExists("ADMIN_USER")).isFalse();
            fixture.liquibase.update();
            assertThat(fixture.scalar("SELECT COUNT(*) FROM DATABASECHANGELOG")).isEqualTo(4);
        }
    }

    private static Fixture fixture() throws Exception {
        var connection = DriverManager.getConnection(
                "jdbc:h2:mem:liquibase-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        Database database = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection));
        Liquibase liquibase = new Liquibase(CHANGELOG, new ClassLoaderResourceAccessor(), database);
        return new Fixture(liquibase, database, connection);
    }

    private record Fixture(Liquibase liquibase, Database database, java.sql.Connection connection) implements AutoCloseable {
        long scalar(String sql) throws Exception {
            try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
                result.next();
                return result.getLong(1);
            }
        }

        void execute(String sql) throws Exception {
            try (var statement = connection.createStatement()) { statement.execute(sql); }
        }

        boolean tableExists(String table) throws Exception {
            try (var result = connection.getMetaData().getTables(null, null, table, new String[]{"TABLE"})) {
                return result.next();
            }
        }

        @Override public void close() throws Exception { database.close(); }
    }
}
