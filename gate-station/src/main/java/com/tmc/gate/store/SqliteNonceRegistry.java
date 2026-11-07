package com.tmc.gate.store;

import com.tmc.gate.validation.NonceRegistry;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public final class SqliteNonceRegistry implements NonceRegistry {

    private final Connection connection;

    public SqliteNonceRegistry(LocalDatabase database) {
        this.connection = database.connection();
    }

    @Override
    public boolean isUsed(String nonce) {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM used_nonces WHERE nonce = ?")) {
            statement.setString(1, nonce);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (SQLException e) {
            throw new LocalStoreException("Cannot check nonce", e);
        }
    }

    @Override
    public void markUsed(String nonce) {
        try (PreparedStatement statement =
                connection.prepareStatement("INSERT OR IGNORE INTO used_nonces (nonce) VALUES (?)")) {
            statement.setString(1, nonce);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new LocalStoreException("Cannot record nonce", e);
        }
    }
}
