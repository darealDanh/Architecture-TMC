package com.tmc.gate.store;

import com.tmc.gate.access.AccessEvent;
import com.tmc.gate.access.AccessEventLog;
import com.tmc.gate.access.AccessResult;
import com.tmc.gate.access.Direction;
import com.tmc.gate.access.InterruptedUnlocks;
import com.tmc.gate.access.RejectionReason;
import com.tmc.gate.access.UnlockStatus;
import com.tmc.gate.sync.Outbox;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class SqliteAccessEventLog implements AccessEventLog, Outbox, InterruptedUnlocks {

    private static final String COLUMNS =
            "id, gate_id, factory_id, direction, employee_id, occurred_at, result, rejection_reason, unlock_status";

    private final Connection connection;

    public SqliteAccessEventLog(LocalDatabase database) {
        this.connection = database.connection();
    }

    @Override
    public void append(AccessEvent event) {
        String sql = "INSERT INTO access_events (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, event.id().toString());
            statement.setString(2, event.gateId());
            statement.setString(3, event.factoryId());
            statement.setString(4, event.direction().name());
            statement.setString(5, event.employeeId());
            statement.setLong(6, event.occurredAt().toEpochMilli());
            statement.setString(7, event.result().name());
            statement.setString(8, event.rejectionReason() == null ? null : event.rejectionReason().name());
            statement.setString(9, event.unlockStatus().name());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new LocalStoreException("Cannot store access event " + event.id(), e);
        }
    }

    @Override
    public void updateUnlockStatus(UUID eventId, UnlockStatus status) {
        try (PreparedStatement statement =
                connection.prepareStatement("UPDATE access_events SET unlock_status = ? WHERE id = ?")) {
            statement.setString(1, status.name());
            statement.setString(2, eventId.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new LocalStoreException("Cannot update access event " + eventId, e);
        }
    }

    @Override
    public Optional<AccessEvent> find(UUID eventId) {
        try (PreparedStatement statement =
                connection.prepareStatement("SELECT " + COLUMNS + " FROM access_events WHERE id = ?")) {
            statement.setString(1, eventId.toString());
            return readEvents(statement).stream().findFirst();
        } catch (SQLException e) {
            throw new LocalStoreException("Cannot read access event " + eventId, e);
        }
    }

    @Override
    public List<AccessEvent> pending(int limit) {
        String sql = "SELECT " + COLUMNS + " FROM access_events WHERE synced = 0 ORDER BY occurred_at, rowid LIMIT ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            return readEvents(statement);
        } catch (SQLException e) {
            throw new LocalStoreException("Cannot read pending events", e);
        }
    }

    @Override
    public void markSynced(Collection<UUID> eventIds) {
        try (PreparedStatement statement =
                connection.prepareStatement("UPDATE access_events SET synced = 1 WHERE id = ?")) {
            for (UUID id : eventIds) {
                statement.setString(1, id.toString());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new LocalStoreException("Cannot mark events as synced", e);
        }
    }

    @Override
    public int pendingCount() {
        try (PreparedStatement statement =
                connection.prepareStatement("SELECT COUNT(*) FROM access_events WHERE synced = 0");
                ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        } catch (SQLException e) {
            throw new LocalStoreException("Cannot count pending events", e);
        }
    }

    @Override
    public List<UUID> resolveAsUnknown() {
        String sql = "UPDATE access_events SET unlock_status = ? WHERE unlock_status = ? RETURNING id";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, UnlockStatus.UNLOCK_UNKNOWN.name());
            statement.setString(2, UnlockStatus.PENDING.name());
            List<UUID> resolved = new ArrayList<>();
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    resolved.add(UUID.fromString(result.getString(1)));
                }
            }
            return resolved;
        } catch (SQLException e) {
            throw new LocalStoreException("Cannot resolve interrupted unlocks", e);
        }
    }

    private static List<AccessEvent> readEvents(PreparedStatement statement) throws SQLException {
        List<AccessEvent> events = new ArrayList<>();
        try (ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                events.add(toEvent(result));
            }
        }
        return events;
    }

    private static AccessEvent toEvent(ResultSet row) throws SQLException {
        String rejectionReason = row.getString("rejection_reason");
        return new AccessEvent(
                UUID.fromString(row.getString("id")),
                row.getString("gate_id"),
                row.getString("factory_id"),
                Direction.valueOf(row.getString("direction")),
                row.getString("employee_id"),
                Instant.ofEpochMilli(row.getLong("occurred_at")),
                AccessResult.valueOf(row.getString("result")),
                rejectionReason == null ? null : RejectionReason.valueOf(rejectionReason),
                UnlockStatus.valueOf(row.getString("unlock_status")));
    }
}
