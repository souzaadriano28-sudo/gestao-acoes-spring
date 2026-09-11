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
            assertThat(fixture.scalar("SELECT COUNT(*) FROM DATABASECHANGELOG")).isEqualTo(12);
            assertThat(fixture.scalar("SELECT COUNT(*) FROM DATABASECHANGELOGLOCK WHERE LOCKED = FALSE")).isEqualTo(1);
            assertThat(fixture.tableExists("ACAO")).isTrue();
            assertThat(fixture.tableExists("CORRETORA")).isTrue();
            assertThat(fixture.tableExists("TRANSACAO")).isTrue();
            assertThat(fixture.tableExists("POSICAO_CARTEIRA")).isTrue();
            assertThat(fixture.tableExists("ADMIN_USER")).isTrue();
            assertThat(fixture.columnExists("ACAO", "QUOTE_PROVIDER")).isTrue();
            assertThat(fixture.indexExists("TRANSACAO", "IDX_TRANSACAO_DATA_ID")).isTrue();
            assertThat(fixture.tableExists("EXCHANGE_RATE_SNAPSHOT")).isTrue();
            assertThat(fixture.columnExists("CORRETORA", "REGULATORY_STATUS")).isTrue();
            assertThat(fixture.indexExists("TRANSACAO", "IDX_TRANSACAO_TIPO_DATA_ID")).isTrue();

            fixture.liquibase.update();
            assertThat(fixture.scalar("SELECT COUNT(*) FROM DATABASECHANGELOG")).isEqualTo(12);
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
            assertThat(fixture.scalar("SELECT COUNT(*) FROM DATABASECHANGELOG")).isEqualTo(12);
        }
    }

    @Test
    void disposableInitialSchemaRollsBackAndCanBeAppliedAgain() throws Exception {
        try (Fixture fixture = fixture()) {
            fixture.liquibase.update();
            fixture.liquibase.rollback(12, "");
            assertThat(fixture.tableExists("ACAO")).isFalse();
            assertThat(fixture.tableExists("ADMIN_USER")).isFalse();
            fixture.liquibase.update();
            assertThat(fixture.scalar("SELECT COUNT(*) FROM DATABASECHANGELOG")).isEqualTo(12);
        }
    }

    @Test
    void existingDatabaseUpgradesWithoutReclassifyingLegacyData() throws Exception {
        try (Fixture fixture = fixture()) {
            fixture.liquibase.update(6, "");
            fixture.execute("INSERT INTO admin_user (id, username, password_hash, enabled, failed_attempts, created_at, updated_at, version) VALUES (1, 'admin', 'hash', TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)");
            fixture.execute("INSERT INTO corretora (razao_social, cep, cnpj, validada_na_cvm) VALUES ('Legada', '01001000', '12345678000199', TRUE)");

            fixture.liquibase.update();

            assertThat(fixture.scalar("SELECT COUNT(*) FROM DATABASECHANGELOG")).isEqualTo(12);
            assertThat(fixture.text("SELECT regulatory_status FROM corretora WHERE cnpj = '12345678000199'"))
                    .isEqualTo("NOT_CHECKED");
            assertThat(fixture.scalar("SELECT COUNT(*) FROM corretora WHERE validada_na_cvm = TRUE")).isEqualTo(1);
            assertThat(fixture.scalar("SELECT COUNT(*) FROM exchange_rate_snapshot")).isZero();
            assertThat(fixture.scalar("SELECT COUNT(*) FROM user_account")).isEqualTo(1);
            assertThat(fixture.scalar("SELECT COUNT(*) FROM portfolio")).isEqualTo(1);
            assertThat(fixture.scalar("SELECT COUNT(*) FROM corretora WHERE owner_id = 1")).isEqualTo(1);
            fixture.execute("INSERT INTO user_account (username, email, password_hash, enabled, failed_attempts, created_at, updated_at, version) VALUES ('next-user', 'next@atlas.local', 'hash', TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)");
            assertThat(fixture.scalar("SELECT MAX(id) FROM user_account")).isGreaterThan(1);
        }
    }

    @Test
    void legacyGlobalBrokerConstraintIsRemovedWithoutRemovingScopedUniqueness() throws Exception {
        try (Fixture fixture = fixture()) {
            fixture.liquibase.update(11, "");
            fixture.execute("ALTER TABLE corretora ADD CONSTRAINT uk_corretora_cnpj UNIQUE (cnpj)");

            fixture.liquibase.update();
            fixture.execute("INSERT INTO user_account (username, email, password_hash, enabled, failed_attempts, created_at, updated_at, version) VALUES ('owner-one', 'owner-one@atlas.local', 'hash', TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)");
            fixture.execute("INSERT INTO user_account (username, email, password_hash, enabled, failed_attempts, created_at, updated_at, version) VALUES ('owner-two', 'owner-two@atlas.local', 'hash', TRUE, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)");
            fixture.execute("INSERT INTO corretora (cnpj, razao_social, cep, validada_na_cvm, regulatory_status, owner_id) VALUES ('02332886000104', 'XP one', '01001000', TRUE, 'NOT_CHECKED', (SELECT id FROM user_account WHERE username = 'owner-one'))");
            fixture.execute("INSERT INTO corretora (cnpj, razao_social, cep, validada_na_cvm, regulatory_status, owner_id) VALUES ('02332886000104', 'XP two', '01001000', TRUE, 'NOT_CHECKED', (SELECT id FROM user_account WHERE username = 'owner-two'))");

            assertThatThrownBy(() -> fixture.execute("INSERT INTO corretora (cnpj, razao_social, cep, validada_na_cvm, regulatory_status, owner_id) VALUES ('02332886000104', 'XP duplicate', '01001000', TRUE, 'NOT_CHECKED', (SELECT id FROM user_account WHERE username = 'owner-one'))"))
                    .isInstanceOf(java.sql.SQLException.class);
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

        String text(String sql) throws Exception {
            try (var statement = connection.createStatement(); var result = statement.executeQuery(sql)) {
                result.next();
                return result.getString(1);
            }
        }

        boolean columnExists(String table, String column) throws Exception {
            try (var result = connection.getMetaData().getColumns(null, null, table, column)) { return result.next(); }
        }

        boolean indexExists(String table, String index) throws Exception {
            try (var result = connection.getMetaData().getIndexInfo(null, null, table, false, false)) {
                while (result.next()) if (index.equalsIgnoreCase(result.getString("INDEX_NAME"))) return true;
                return false;
            }
        }

        @Override public void close() throws Exception { database.close(); }
    }
}
