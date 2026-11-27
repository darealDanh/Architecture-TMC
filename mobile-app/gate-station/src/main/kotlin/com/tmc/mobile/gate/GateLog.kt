package com.tmc.mobile.gate

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.time.Instant
import java.util.UUID

enum class GateStatus {
    PENDING,
    PASSED,
    REJECTED,
    UNLOCK_FAILED,
    UNLOCK_UNKNOWN,
}

data class GateEvent(
    val id: String,
    val employeeId: String?,
    val timestamp: Instant,
    val status: GateStatus,
    val reason: String?,
)

class GateLog(path: Path) : AutoCloseable {
    private val connection: Connection

    init {
        val absolutePath = path.toAbsolutePath()
        Files.createDirectories(absolutePath.parent)
        connection = DriverManager.getConnection("jdbc:sqlite:$absolutePath")
        connection.createStatement().use { statement ->
            statement.execute("PRAGMA journal_mode=WAL")
            statement.execute("PRAGMA synchronous=FULL")
            statement.execute(
                """
                CREATE TABLE IF NOT EXISTS access_events (
                    id TEXT PRIMARY KEY,
                    employee_id TEXT,
                    gate_id TEXT NOT NULL,
                    factory TEXT NOT NULL,
                    direction TEXT NOT NULL,
                    scanned_at TEXT NOT NULL,
                    status TEXT NOT NULL,
                    reason TEXT,
                    certificate_serial TEXT,
                    nonce TEXT,
                    UNIQUE (certificate_serial, nonce)
                )
                """.trimIndent(),
            )
        }
    }

    @Synchronized
    fun recordPending(
        employeeId: String,
        certificateSerial: String,
        nonce: String,
        gateId: String,
        factory: String,
        direction: String,
        at: Instant,
    ): GateEvent? {
        val event = GateEvent(UUID.randomUUID().toString(), employeeId, at, GateStatus.PENDING, null)
        connection.prepareStatement(
            """
            INSERT INTO access_events
                (id, employee_id, gate_id, factory, direction, scanned_at, status, certificate_serial, nonce)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (certificate_serial, nonce) DO NOTHING
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, event.id)
            statement.setString(2, employeeId)
            statement.setString(3, gateId)
            statement.setString(4, factory)
            statement.setString(5, direction)
            statement.setString(6, at.toString())
            statement.setString(7, event.status.name)
            statement.setString(8, certificateSerial)
            statement.setString(9, nonce)
            return if (statement.executeUpdate() == 1) event else null
        }
    }

    @Synchronized
    fun recordRejected(
        reason: String,
        employeeId: String?,
        gateId: String,
        factory: String,
        direction: String,
        at: Instant,
    ): GateEvent {
        val event = GateEvent(UUID.randomUUID().toString(), employeeId, at, GateStatus.REJECTED, reason)
        connection.prepareStatement(
            """
            INSERT INTO access_events (id, employee_id, gate_id, factory, direction, scanned_at, status, reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
        ).use { statement ->
            statement.setString(1, event.id)
            statement.setString(2, employeeId)
            statement.setString(3, gateId)
            statement.setString(4, factory)
            statement.setString(5, direction)
            statement.setString(6, at.toString())
            statement.setString(7, event.status.name)
            statement.setString(8, reason)
            statement.executeUpdate()
        }
        return event
    }

    @Synchronized
    fun complete(id: String, status: GateStatus): Unit {
        require(status == GateStatus.PASSED || status == GateStatus.UNLOCK_FAILED || status == GateStatus.UNLOCK_UNKNOWN)
        connection.prepareStatement("UPDATE access_events SET status = ? WHERE id = ? AND status = 'PENDING'").use { statement ->
            statement.setString(1, status.name)
            statement.setString(2, id)
            check(statement.executeUpdate() == 1)
        }
    }

    @Synchronized
    fun recent(limit: Int = 10): List<GateEvent> {
        connection.prepareStatement(
            "SELECT id, employee_id, scanned_at, status, reason FROM access_events ORDER BY rowid DESC LIMIT ?",
        ).use { statement ->
            statement.setInt(1, limit)
            statement.executeQuery().use { rows ->
                return buildList {
                    while (rows.next()) {
                        add(
                            GateEvent(
                                id = rows.getString("id"),
                                employeeId = rows.getString("employee_id"),
                                timestamp = Instant.parse(rows.getString("scanned_at")),
                                status = GateStatus.valueOf(rows.getString("status")),
                                reason = rows.getString("reason"),
                            ),
                        )
                    }
                }
            }
        }
    }

    @Synchronized
    override fun close() {
        connection.close()
    }
}
