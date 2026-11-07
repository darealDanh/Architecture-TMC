package com.tmc.gate.store;

import com.tmc.gate.credential.Ed25519;
import com.tmc.gate.validation.ReferenceData;
import java.security.PublicKey;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SqliteReferenceData implements ReferenceDataReader, ReferenceDataWriter {

    private final Connection connection;

    public SqliteReferenceData(LocalDatabase database) {
        this.connection = database.connection();
    }

    @Override
    public ReferenceData current() {
        try {
            return new ReferenceData(
                    readServerKey(),
                    readColumn("SELECT serial FROM revoked_certificates"),
                    readColumn("SELECT employee_id FROM revoked_employees"),
                    readActiveEmployees());
        } catch (SQLException e) {
            throw new LocalStoreException("Cannot read reference data", e);
        }
    }

    @Override
    public void replace(ReferenceData data) {
        try {
            connection.setAutoCommit(false);
            try {
                clearAll();
                writeServerKey(data.serverPublicKey());
                writeColumn("INSERT INTO revoked_certificates (serial) VALUES (?)", data.revokedCertificateSerials());
                writeColumn("INSERT INTO revoked_employees (employee_id) VALUES (?)", data.revokedEmployeeIds());
                writeActiveEmployees(data.activeEmployeeFactories());
                connection.commit();
            } catch (SQLException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new LocalStoreException("Cannot replace reference data", e);
        }
    }

    private PublicKey readServerKey() throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT public_key FROM server_key WHERE id = 1")) {
            return result.next() ? Ed25519.decodePublicKey(result.getBytes(1)) : null;
        }
    }

    private Set<String> readColumn(String sql) throws SQLException {
        Set<String> values = new HashSet<>();
        try (Statement statement = connection.createStatement(); ResultSet result = statement.executeQuery(sql)) {
            while (result.next()) {
                values.add(result.getString(1));
            }
        }
        return values;
    }

    private Map<String, String> readActiveEmployees() throws SQLException {
        Map<String, String> employees = new HashMap<>();
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT employee_id, factory_id FROM active_employees")) {
            while (result.next()) {
                employees.put(result.getString(1), result.getString(2));
            }
        }
        return employees;
    }

    private void clearAll() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("DELETE FROM server_key");
            statement.executeUpdate("DELETE FROM revoked_certificates");
            statement.executeUpdate("DELETE FROM revoked_employees");
            statement.executeUpdate("DELETE FROM active_employees");
        }
    }

    private void writeServerKey(PublicKey key) throws SQLException {
        if (key == null) {
            return;
        }
        try (PreparedStatement statement =
                connection.prepareStatement("INSERT INTO server_key (id, public_key) VALUES (1, ?)")) {
            statement.setBytes(1, key.getEncoded());
            statement.executeUpdate();
        }
    }

    private void writeColumn(String sql, Set<String> values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (String value : values) {
                statement.setString(1, value);
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void writeActiveEmployees(Map<String, String> employees) throws SQLException {
        try (PreparedStatement statement =
                connection.prepareStatement("INSERT INTO active_employees (employee_id, factory_id) VALUES (?, ?)")) {
            for (Map.Entry<String, String> employee : employees.entrySet()) {
                statement.setString(1, employee.getKey());
                statement.setString(2, employee.getValue());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
