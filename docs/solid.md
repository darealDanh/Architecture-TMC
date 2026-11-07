# SOLID in the Gate Station Software

Section 6.2 of our architecture document lists the SOLID principles as design guidelines, and CO-3 makes them a constraint. This document shows where each one appears in the Gate Station Software we built, and where we did not follow it fully.

The code is in `gate-station/src/main/java/com/tmc/gate`. Every class named below exists in that tree. The excerpts are copied from the code, shortened only where marked with `...`.

## Packages at a glance

| Package | Contents |
|---|---|
| `access` | `GateStation` (the scan-to-unlock flow), `AccessEvent`, `StartupRecovery`, and the ports `AccessEventLog`, `InterruptedUnlocks`, `ReferenceDataReader` |
| `validation` | `QrValidator`, the seven `ValidationRule` classes, `ReferenceData`, `NonceRegistry` |
| `credential` | QR format: `CredentialCodec`, `DeviceCertificate`, `ScannedCredential`, `Ed25519` |
| `turnstile` | `TurnstileGateway` and its three implementations |
| `sync` | `SyncClient` and the ports `Outbox`, `ReferenceDataWriter`, `EventIntake`, `ReferenceDataSource`, `GateStatusReceiver` |
| `store` | SQLite implementations of the ports |
| `alert`, `config` | Guard alerts, `ConfigReader` and `GateSettings` |
| `demo` | Wiring, the simulated phone, server and central system |

## S: Single Responsibility

Each class has one reason to change. The clearest case is `GateStation`. It decides the order of the steps in Section 2.1 of the C&C view, and nothing else:

```java
public AccessEvent handleScan(String rawQr) {
    Instant now = clock.instant();
    ScannedCredential credential;
    try {
        credential = codec.decode(rawQr);
    } catch (MalformedCredentialException e) {
        return record(AccessEvent.rejected(gate, null, RejectionReason.MALFORMED, now));
    }
    String employeeId = credential.certificate().employeeId();
    Optional<RejectionReason> violation =
            validator.firstViolation(credential, new ValidationContext(referenceData.current(), gate.factoryId(), now));
    if (violation.isPresent()) {
        return record(AccessEvent.rejected(gate, employeeId, violation.get(), now));
    }
    nonces.markUsed(credential.nonce());
    return unlock(record(AccessEvent.accepted(gate, employeeId, now)));
}
```

It does not parse QR bytes, check signatures, write SQL or speak HTTP. Each of those has its own class, and each class changes for a different reason:

| Class | Changes when |
|---|---|
| `CredentialCodec` | The QR payload format changes |
| `CertificateSignatureRule`, `FreshnessRule`, ... | One validation rule changes |
| `SqliteAccessEventLog` | The local storage changes |
| `HttpTurnstileGateway` | The Turnstile Control System API changes (AD-08) |
| `SyncClient` | The synchronisation protocol with central changes (NFR-07) |
| `GateStation` | The order of the access flow changes (AD-06) |

The order in `handleScan` is where AD-06 lives: `record(...)` writes the event before `unlock(...)` calls the turnstile. The test `eventIsStoredBeforeUnlockIsRequested` in `GateStationTest` checks this. Its fake turnstile looks up the event in the store and fails if the event is not there yet.

## O: Open/Closed

Section 6.2 says a new access rule should be a new class, not an edit. In the code, every check from FR-AT-01 is a separate `ValidationRule`:

```java
@FunctionalInterface
public interface ValidationRule {

    Optional<RejectionReason> check(ScannedCredential credential, ValidationContext context);
}
```

`QrValidator` runs the rules in order and returns the first failure. It never looks inside them:

```java
public Optional<RejectionReason> firstViolation(ScannedCredential credential, ValidationContext context) {
    return rules.stream()
            .map(rule -> rule.check(credential, context))
            .flatMap(Optional::stream)
            .findFirst();
}
```

Suppose TMC later wants to block entry outside an employee's shift. That would be a new `ShiftRule` class added to the list. `QrValidatorTest.newRuleIsAddedWithoutChangingExistingRules` does exactly this with a lambda, and no existing rule is touched.

The second example came from M2. The Issue 1 table says that when the turnstile does not answer, the gate should retry with an idempotency key. We already had `HttpTurnstileGateway`. Instead of putting a loop inside it, we wrapped it:

```java
public UnlockResult unlock(String gateId, UUID eventId) {
    UnlockResult result = delegate.unlock(gateId, eventId);
    for (int retry = 0; retry < maxRetries && result == UnlockResult.UNKNOWN; retry++) {
        result = delegate.unlock(gateId, eventId);
    }
    return result;
}
```

`RetryingTurnstileGateway` works around any `TurnstileGateway`, so the retry rule applies to the real adapter and the simulator alike. Neither of them was edited to get it.

## L: Liskov Substitution

Section 6.2 gives this example: "A simulated turnstile adapter used for testing must behave like the real adapter from the caller's point of view." We turned that sentence into a test. `TurnstileGatewayContract` is an abstract test class, and each implementation must pass it:

```java
abstract class TurnstileGatewayContract {

    abstract TurnstileGateway gatewayThatUnlocks();

    @Test
    void repeatedUnlockWithSameEventIdIsAccepted() {
        TurnstileGateway gateway = gatewayThatUnlocks();
        UUID eventId = UUID.randomUUID();

        assertThat(gateway.unlock("BD-GATE-01", eventId)).isEqualTo(UnlockResult.UNLOCKED);
        assertThat(gateway.unlock("BD-GATE-01", eventId)).isEqualTo(UnlockResult.UNLOCKED);
    }
    ...
}
```

`TurnstileGatewayContractTest` runs it against `SimulatedTurnstileGateway`, `HttpTurnstileGateway` (against a local HTTP server) and `RetryingTurnstileGateway`.

Substitution also depends on how failures are reported. `GateStation` expects one of three answers: `UNLOCKED`, `FAILED` or `UNKNOWN`. If the HTTP adapter threw an exception on a timeout while the simulator returned `UNKNOWN`, swapping one for the other would change the gate's behaviour. So `HttpTurnstileGateway` turns every network problem into `UNKNOWN`:

```java
} catch (IOException e) {
    return UnlockResult.UNKNOWN;
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    return UnlockResult.UNKNOWN;
}
```

## I: Interface Segregation

One SQLite table holds all access events, but the classes that use it need different things from it. We split the interfaces by client:

| Interface | Methods | Used by |
|---|---|---|
| `AccessEventLog` | `append`, `updateUnlockStatus`, `find` | `GateStation` |
| `Outbox` | `pending`, `markSynced`, `pendingCount` | `SyncClient` |
| `InterruptedUnlocks` | `resolveAsUnknown` | `StartupRecovery` |

`SqliteAccessEventLog` implements all three:

```java
public final class SqliteAccessEventLog implements AccessEventLog, Outbox, InterruptedUnlocks {
```

`GateStation` cannot mark an event as synced, and `SyncClient` cannot append one. If they could, a bug in the sync code could mark events as delivered that central never received, which is the loss NFR-07 forbids.

Reference data follows the same pattern. The gate only reads it (`ReferenceDataReader.current()`), and only the Sync Client replaces it (`ReferenceDataWriter.replace(...)`). `SqliteReferenceData` implements both. Alerts work the same way: `GateStation` receives an `AlertSink` with a single `raise` method, because the gate never clears an alert.

On the turnstile side, `TurnstileGateway` has a single method. This is the "gate station depends only on an unlock interface" from Section 6.2. The M2 table offered "retry or query the turnstile status". We chose retry partly because it needs no second method on this interface.

## D: Dependency Inversion

Section 6.2 says business logic should depend on abstractions it defines, with source dependencies pointing toward the business rules. In practice, each interface lives in the package of the class that calls it, not in the package of the class that implements it:

- `AccessEventLog`, `InterruptedUnlocks` and `ReferenceDataReader` are in `access`, next to `GateStation`.
- `Outbox`, `ReferenceDataWriter` and `EventIntake` are in `sync`, next to `SyncClient`.
- `NonceRegistry` is in `validation`, next to `ReplayRule`.

The `store` package implements them, so it imports `access`, `sync` and `validation`. None of those packages import `store`. These are the package dependencies of the main code today:

```
access     -> alert, credential, turnstile, validation
sync       -> access, alert, config, validation
validation -> config, credential
store      -> access, credential, sync, validation
turnstile, credential, alert, config -> (none)
```

We did not start this way. The plan put the store interfaces in `store`, and `GateStation` would then have imported `store` while `store` imported `AccessEvent` from `access`. That is a cycle. We moved the interfaces to their callers while building Task 6. Writing this document, we found a second cycle: `validation` used `RejectionReason` and `GateIdentity` from `access`, while `access` used `validation`. We moved `RejectionReason` into `validation`, and gave `ValidationContext` only the factory ID it needs instead of the whole `GateIdentity`. Both fixes were package moves; no logic changed.

Concrete classes meet in one place, `demo/Demo.java`:

```java
GateStation station = new GateStation(GATE, new CredentialCodec(),
        QrValidator.standard(settings, nonces), referenceData, nonces, events,
        new RetryingTurnstileGateway(SimulatedTurnstileGateway.sequence(...), settings.unlockRetries()),
        alerts, clock);
```

A real kiosk build would change only this wiring. It would pass `HttpTurnstileGateway` instead of the simulator, and an HTTPS client for tmc-central instead of `InMemoryCentral`. Time is injected as `java.time.Clock` for the same reason, so tests can move the clock four hours forward to trigger the offline alert.

## Where we bend the rules

- **`AccessEvent` has nullable fields.** `rejectionReason` is null for accepted events, and `employeeId` is null when the QR could not be read at all. Separate types for accepted and rejected events would be stricter, but the event is one row in one table and is synced as one record, so we kept one type.
- **`SyncClient` depends on the concrete `Alerts` class**, not an interface. It needs to raise, resolve and list alerts, and only one implementation exists. If a second appears, an interface can be extracted then.
- **Adding a rule to the default set edits one line.** `ShiftRule` from the Open/Closed example needs no change to existing rules, but `QrValidator.standard(...)` still has to list it. The edit is to the composition, not to any rule's logic.
- **One class implements three interfaces.** `SqliteAccessEventLog` is split for its clients, not internally. The three roles share one table, and splitting the class would mean three classes writing the same rows.
- **The Turnstile API format is our assumption.** `HttpTurnstileGateway` sends `POST /gates/{gateId}/unlock` with an `Idempotency-Key` header. The real Turnstile Control System API is not in the project description. If it differs, only this class changes, which is the point of AD-08.
- **Replay protection is per gate.** Used nonces are stored in each kiosk's Local Store, and kiosks do not talk to each other offline. A copied QR code could pass once at a second gate within its validity window, which is 30 seconds plus a 5-second tolerance. We accepted this, the same way M2 accepts that revocations reach a gate only at its next sync.
