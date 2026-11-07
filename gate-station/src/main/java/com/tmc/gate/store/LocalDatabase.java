package com.tmc.gate.store;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public final class LocalDatabase implements AutoCloseable {

    private static final List<String> SETUP = List.of(
            "PRAGMA journal_mode=WAL",
            "PRAGMA synchronous=FULL",
            """
            CREATE TABLE IF NOT EXISTS access_events (
                id TEXT PRIMARY KEY,
                gate_id TEXT NOT NULL,
                factory_id TEXT NOT NULL,
                direction TEXT NOT NULL,
                employee_id TEXT,
                occurred_at INTEGER NOT NULL,
                result TEXT NOT NULL,
                rejection_reason TEXT,
                unlock_status TEXT NOT NULL,
                synced INTEGER NOT NULL DEFAULT 0
            )""",
            "CREATE INDEX IF NOT EXISTS access_events_outbox ON access_events (synced, occurred_at)",
            "CREATE TABLE IF NOT EXISTS used_nonces (nonce TEXT PRIMARY KEY)",
            "CREATE TABLE IF NOT EXISTS server_key (id INTEGER PRIMARY KEY CHECK (id = 1), public_key BLOB NOT NULL)",
            "CREATE TABLE IF NOT EXISTS revoked_certificates (serial TEXT PRIMARY KEY)",
            "CREATE TABLE IF NOT EXISTS revoked_employees (employee_id TEXT PRIMARY KEY)",
            "CREATE TABLE IF NOT EXISTS active_employees (employee_id TEXT PRIMARY KEY, factory_id TEXT NOT NULL)");

    private final Connection connection;

    private LocalDatabase(Connection connection) {
        this.connection = connection;
    }

    public static LocalDatabase open(Path file) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
            try (Statement statement = connection.createStatement()) {
                for (String sql : SETUP) {
                    statement.execute(sql);
                }
            }
            return new LocalDatabase(connection);
        } catch (SQLException e) {
            throw new LocalStoreException("Cannot open local store at " + file, e);
        }
    }

    public Connection connection() {
        return connection;
    }

    @Override
    public void close() {
        try {
            connection.close();
        } catch (SQLException e) {
            throw new LocalStoreException("Cannot close local store", e);
        }
    }
}
