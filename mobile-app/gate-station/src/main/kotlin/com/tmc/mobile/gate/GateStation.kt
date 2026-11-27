package com.tmc.mobile.gate

import com.tmc.mobile.domain.gate.GatePassDecision
import com.tmc.mobile.domain.gate.ValidateGatePass
import java.time.Clock

fun interface Turnstile {
    fun unlock(gateId: String): Boolean
}

class GateStation(
    val gateId: String,
    val factory: String,
    val direction: String,
    private val validator: ValidateGatePass,
    private val log: GateLog,
    private val turnstile: Turnstile,
    private val clock: Clock,
) {
    @Synchronized
    fun scan(payload: String): GateEvent {
        val now = clock.instant()
        return when (val decision = validator(payload, factory)) {
            is GatePassDecision.Rejected ->
                log.recordRejected(decision.reason.name, null, gateId, factory, direction, now)
            is GatePassDecision.Allowed -> {
                val pending = log.recordPending(
                    decision.employeeId,
                    decision.certificateSerial,
                    decision.nonce,
                    gateId,
                    factory,
                    direction,
                    now,
                ) ?: return log.recordRejected("REPLAY", decision.employeeId, gateId, factory, direction, now)

                val result = try {
                    if (turnstile.unlock(gateId)) GateStatus.PASSED else GateStatus.UNLOCK_FAILED
                } catch (e: Exception) {
                    GateStatus.UNLOCK_UNKNOWN
                }
                log.complete(pending.id, result)
                pending.copy(status = result)
            }
        }
    }

    fun recent(): List<GateEvent> = log.recent()
}
