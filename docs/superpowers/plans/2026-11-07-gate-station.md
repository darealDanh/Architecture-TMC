# Gate Station Software Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the TMC Gate Station Software in Java (offline QR validation, write-before-unlock, turnstile adapter with retry, outbox sync), add CI, and write the SOLID write-up and team reflection.

**Architecture:** One Maven module `gate-station/`. Business logic (`GateStation`, `QrValidator`, `SyncClient`) depends only on small interfaces it owns (`TurnstileGateway`, `AccessEventLog`, `Outbox`, `EventIntake`, ...). SQLite, HTTP and the central system sit behind those interfaces. Central and the phone are simulated by classes in `com.tmc.gate.demo`.

**Tech Stack:** Java 17 (`--release 17`), Maven + Maven Wrapper, JUnit 5, AssertJ, `org.xerial:sqlite-jdbc`, JDK Ed25519, `java.net.http`, GitHub Actions.

**Spec:** `docs/superpowers/specs/2026-11-07-gate-station-design.md` (source document: `BTL ARCHITECTURE.txt`; rules: `CLAUDE.md`)

## Global Constraints

- No comments of any kind in Java code, including Javadoc and commented-out code.
- No Claude or AI attribution in commits: no `Co-Authored-By`, no "Generated with" lines. Commits use the machine's git identity.
- Base package `com.tmc.gate`. Interface names from the document: `TurnstileGateway`, `EventIntake`, `ConfigReader`.
- Defaults: QR validity window 30 s, clock tolerance 5 s, unlock retries 2, turnstile timeout 1 s, sync batch size 100, max offline 4 h, pending-events alert threshold 1000.
- Dependencies limited to sqlite-jdbc, JUnit 5, AssertJ (plus Maven plugins).
- Local build: `mvn -B verify` in `gate-station/` (Windows Maven 3.10.0 at `C:\ProgramData\chocolatey\lib\maven\apache-maven-3.10.0\bin`; if `mvn` is not found in PowerShell, refresh PATH with `$env:Path = [Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [Environment]::GetEnvironmentVariable('Path','User')`). CI uses `./mvnw -B verify`.
- Documents: plain direct English, no AI-style phrasing; every SOLID claim names a real class.

## Review Focus

- Empty or garbage scanner input (`""`, `"abc"`, wrong segment count, bad base64) must be stored as a `MALFORMED` rejection with no unlock call. Test in Task 6.
- Phone clock ahead of the gate by more than the tolerance must give `QR_EXPIRED`, not acceptance. Test in Task 3.
- A fresh kiosk with no downloaded reference data must reject every scan (`INVALID_CERTIFICATE`), never accept. Test in Task 3.
- Turnstile HTTP 500 must give `FAILED` with no retry; a response slower than the timeout must give `UNKNOWN`. Test in Task 5.
- After a restart (store reopened), events already acknowledged must not be re-sent, and a resend of unacknowledged events must not create duplicates at central. Test in Task 7.

---

## File Map

```
tmc/
  .gitignore
  README.md
  .github/workflows/ci.yml
  gate-station/
    pom.xml  mvnw  mvnw.cmd  .mvn/wrapper/maven-wrapper.properties
    src/main/java/com/tmc/gate/
      config/      ConfigReader, GateSettings
      access/      Direction, GateIdentity, AccessEvent, AccessResult, RejectionReason, UnlockStatus, GateStation, StartupRecovery
      credential/  DeviceCertificate, ScannedCredential, CredentialCodec, MalformedCredentialException, Ed25519
      validation/  ReferenceData, ValidationContext, ValidationRule, QrValidator, NonceRegistry,
                   CertificateSignatureRule, CertificateExpiryRule, DeviceSignatureRule, FreshnessRule,
                   RevocationRule, FactoryAuthorisationRule, ReplayRule
      store/       AccessEventLog, Outbox, InterruptedUnlocks, ReferenceDataReader, ReferenceDataWriter,
                   LocalDatabase, SqliteAccessEventLog, SqliteNonceRegistry, SqliteReferenceData
      turnstile/   TurnstileGateway, UnlockResult, HttpTurnstileGateway, SimulatedTurnstileGateway, RetryingTurnstileGateway
      alert/       Alert, AlertType, AlertSink, Alerts
      sync/        EventIntake, ReferenceDataSource, GateStatusReceiver, GateStatus, IntakeUnavailableException, SyncClient, SyncReport
      demo/        ServerAuthority, Phone, InMemoryCentral, Demo
    src/test/java/com/tmc/gate/...   (mirrors main)
  docs/solid.md
  docs/reflection.md
```

---

### Task 1: Repository, build, CI and configuration

**Files:**
- Create: `.gitignore`, `README.md`, `.github/workflows/ci.yml`, `gate-station/pom.xml`, Maven Wrapper files
- Create: `gate-station/src/main/java/com/tmc/gate/config/ConfigReader.java`, `GateSettings.java`
- Create: `gate-station/src/main/java/com/tmc/gate/access/Direction.java`, `GateIdentity.java`
- Test: `gate-station/src/test/java/com/tmc/gate/config/GateSettingsTest.java`

**Interfaces:**
- Produces:
  - `interface ConfigReader { Duration qrValidityWindow(); Duration clockTolerance(); int unlockRetries(); Duration turnstileTimeout(); int syncBatchSize(); Duration maxOfflineDuration(); int pendingEventsAlertThreshold(); }`
  - `record GateSettings(Duration qrValidityWindow, Duration clockTolerance, int unlockRetries, Duration turnstileTimeout, int syncBatchSize, Duration maxOfflineDuration, int pendingEventsAlertThreshold) implements ConfigReader` with `static GateSettings defaults()`; compact constructor rejects negative durations/counts and a batch size below 1 with `IllegalArgumentException`.
  - `enum Direction { ENTRY, EXIT }`
  - `record GateIdentity(String gateId, String factoryId, Direction direction)`

- [ ] **Step 1: Initialise git and commit the existing material**

```bash
git init -b main
git add "BTL ARCHITECTURE.txt" CLAUDE.md docs/superpowers
git commit -m "Add architecture document, project rules and gate station design"
git checkout -b feature/gate-station
```

`.gitignore` content: `target/`, `*.db`, `*.db-wal`, `*.db-shm`, `.idea/`, `*.iml`, `.vscode/`, `.DS_Store`.

- [ ] **Step 2: Create `gate-station/pom.xml`**

groupId `com.tmc`, artifactId `gate-station`, version `0.1.0`, packaging jar, `maven.compiler.release` 17, `project.build.sourceEncoding` UTF-8. Dependencies: `org.xerial:sqlite-jdbc:3.46.1.3`; test scope `org.junit.jupiter:junit-jupiter:5.11.3`, `org.assertj:assertj-core:3.26.3`. Plugins: `maven-surefire-plugin:3.5.2`, `maven-shade-plugin:3.6.0` bound to `package` with `ManifestResourceTransformer` mainClass `com.tmc.gate.demo.Demo`.

- [ ] **Step 3: Generate the Maven Wrapper**

Run in `gate-station/`: `mvn -N wrapper:wrapper -Dtype=only-script -Dmaven=3.9.9`
Expected: `mvnw`, `mvnw.cmd`, `.mvn/wrapper/maven-wrapper.properties` created. Run `git update-index --chmod=+x gate-station/mvnw` after adding it.

- [ ] **Step 4: Write `GateSettingsTest`**

```java
@Test void defaultsMatchTheDesign() {
    GateSettings s = GateSettings.defaults();
    assertThat(s.qrValidityWindow()).isEqualTo(Duration.ofSeconds(30));
    assertThat(s.clockTolerance()).isEqualTo(Duration.ofSeconds(5));
    assertThat(s.unlockRetries()).isEqualTo(2);
    assertThat(s.turnstileTimeout()).isEqualTo(Duration.ofSeconds(1));
    assertThat(s.syncBatchSize()).isEqualTo(100);
    assertThat(s.maxOfflineDuration()).isEqualTo(Duration.ofHours(4));
    assertThat(s.pendingEventsAlertThreshold()).isEqualTo(1000);
}
@Test void rejectsZeroBatchSize() {
    assertThatThrownBy(() -> new GateSettings(Duration.ofSeconds(30), Duration.ofSeconds(5), 2, Duration.ofSeconds(1), 0, Duration.ofHours(4), 1000))
        .isInstanceOf(IllegalArgumentException.class);
}
@Test void rejectsNegativeTolerance() { /* same with clockTolerance = Duration.ofSeconds(-1) */ }
```

- [ ] **Step 5: Run it, see it fail to compile, then implement `ConfigReader`, `GateSettings`, `Direction`, `GateIdentity`**

- [ ] **Step 6: Run `mvn -B verify` in `gate-station/`**

Expected: `BUILD SUCCESS`, 3 tests passing.

- [ ] **Step 7: Write `.github/workflows/ci.yml`**

```yaml
name: CI
on:
  pull_request:
  push:
    branches: [main]
jobs:
  build:
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: gate-station
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'
          cache: maven
      - run: ./mvnw -B verify
      - if: github.event_name == 'push'
        uses: actions/upload-artifact@v4
        with:
          name: gate-station
          path: gate-station/target/gate-station-*.jar
```

- [ ] **Step 8: Write `README.md`**

Short: what the repo is, layout, how to build (`./mvnw verify` / `mvnw.cmd verify`), how to run the demo (`java -jar gate-station/target/gate-station-0.1.0.jar`), and the GitHub steps after pushing: Settings → Branches → Add branch protection rule for `main` → Require status checks to pass → select `build`. Demo section is filled in Task 8.

- [ ] **Step 9: Commit**

```bash
git add .gitignore README.md .github gate-station
git commit -m "Set up Maven build, wrapper, CI workflow and gate settings"
```

---

### Task 2: QR credential format and phone simulation

**Files:**
- Create: `credential/Ed25519.java`, `DeviceCertificate.java`, `ScannedCredential.java`, `CredentialCodec.java`, `MalformedCredentialException.java`
- Create: `demo/ServerAuthority.java`, `demo/Phone.java`
- Test: `credential/CredentialCodecTest.java`, `demo/PhoneTest.java`

**Interfaces:**
- Produces:
  - `final class Ed25519 { static KeyPair generateKeyPair(); static byte[] sign(PrivateKey key, byte[] data); static boolean verify(PublicKey key, byte[] data, byte[] signature); static PublicKey decodePublicKey(byte[] x509); }` (`verify` returns false on any `GeneralSecurityException`)
  - `record DeviceCertificate(String serial, String employeeId, PublicKey devicePublicKey, Instant expiresAt, byte[] serverSignature)` with `byte[] signedContent()`
  - `record ScannedCredential(DeviceCertificate certificate, Instant issuedAt, String nonce, byte[] deviceSignature)` with `byte[] signedContent()`
  - `final class CredentialCodec { String encode(ScannedCredential c); ScannedCredential decode(String raw) throws MalformedCredentialException; }`
  - `final class ServerAuthority { ServerAuthority(); PublicKey publicKey(); DeviceCertificate certify(String employeeId, PublicKey devicePublicKey, Instant expiresAt); }`
  - `final class Phone { Phone(String employeeId, ServerAuthority authority, Instant certificateExpiry); String showQr(Instant now); DeviceCertificate certificate(); }`

Format (decided): 9 segments joined by `.`, each base64url without padding: `serial . employeeId . devicePublicKey(X.509 bytes) . certExpiry(epoch seconds as decimal text) . serverSignature . issuedAt(epoch millis as decimal text) . nonce . deviceSignature`, prefixed by literal segment `v1`. `DeviceCertificate.signedContent()` is the UTF-8 bytes of the first four encoded segments joined by `.`; `ScannedCredential.signedContent()` is the UTF-8 bytes of the first five certificate segments plus issuedAt and nonce joined by `.`. Nonce: 16 random bytes base64url.

- [ ] **Step 1: Write tests**

`CredentialCodecTest`:
- `roundTripPreservesEveryField`: encode a phone-made credential, decode, compare serial, employeeId, public key encoding, expiry, issuedAt, nonce, both signatures.
- `@ParameterizedTest @ValueSource(strings = {"", "abc", "v1.a.b", "v2.a.b.c.d.e.f.g.h", "v1.!!.b.c.d.e.f.g.h"}) rejectsMalformedInput` → `MalformedCredentialException`.
- `rejectsNonNumericTimestamp`.

`PhoneTest`:
- `qrCodesAreSignedByTheDeviceAndCertifiedByTheServer`: decode `showQr(now)`; `Ed25519.verify(authority.publicKey(), cert.signedContent(), cert.serverSignature())` true; `Ed25519.verify(cert.devicePublicKey(), cred.signedContent(), cred.deviceSignature())` true.
- `everyQrHasAFreshNonce`: two calls give different nonces.

- [ ] **Step 2: Run, see failures, implement, run until green**

Run: `mvn -B verify` → `BUILD SUCCESS`.

- [ ] **Step 3: Commit** `git commit -m "Add device-bound QR credential format and phone simulation"`

---

### Task 3: Validation rules

**Files:**
- Create: `validation/ReferenceData.java`, `ValidationContext.java`, `ValidationRule.java`, `QrValidator.java`, `NonceRegistry.java`, the seven rule classes, `access/RejectionReason.java`
- Test: `validation/QrValidatorTest.java`, one test class per rule

**Interfaces:**
- Consumes: Task 2 `ScannedCredential`, `Ed25519`, `ServerAuthority`, `Phone`; Task 1 `ConfigReader`, `GateIdentity`
- Produces:
  - `enum RejectionReason { MALFORMED, INVALID_CERTIFICATE, CERTIFICATE_EXPIRED, INVALID_SIGNATURE, QR_EXPIRED, REVOKED, NOT_AUTHORISED, REPLAYED }`
  - `record ReferenceData(PublicKey serverPublicKey, Set<String> revokedCertificateSerials, Set<String> revokedEmployeeIds, Map<String, String> activeEmployeeFactories)` with `static ReferenceData empty()` (null key, empty collections) and `boolean hasServerKey()`. `activeEmployeeFactories` maps employee ID to factory ID.
  - `record ValidationContext(ReferenceData referenceData, GateIdentity gate, Instant now)`
  - `interface ValidationRule { Optional<RejectionReason> check(ScannedCredential credential, ValidationContext context); }`
  - `interface NonceRegistry { boolean isUsed(String nonce); void markUsed(String nonce); }`
  - `final class QrValidator { QrValidator(List<ValidationRule> rules); Optional<RejectionReason> firstViolation(ScannedCredential c, ValidationContext ctx); static QrValidator standard(ConfigReader config, NonceRegistry nonces); }`

Rule order in `standard`: CertificateSignature, CertificateExpiry, DeviceSignature, Freshness, Revocation, FactoryAuthorisation, Replay. `FreshnessRule(ConfigReader)` accepts when `issuedAt` is not after `now + tolerance` and not before `now - window - tolerance`. `ReplayRule(NonceRegistry)` only checks; marking is done by `GateStation`. `CertificateSignatureRule` returns `INVALID_CERTIFICATE` when `!referenceData.hasServerKey()`.

- [ ] **Step 1: Write rule tests**

One failing and one passing case per rule, using `ServerAuthority` + `Phone` and a fixed `Instant now = Instant.parse("2026-11-07T07:00:00Z")`. Required cases, with exact values:
- certificate signed by a different `ServerAuthority` → `INVALID_CERTIFICATE`
- `ReferenceData.empty()` → `INVALID_CERTIFICATE`
- certificate expiry `now.minusSeconds(1)` → `CERTIFICATE_EXPIRED`
- device signature byte flipped → `INVALID_SIGNATURE`
- issuedAt `now.minusSeconds(35)` → accepted; `now.minusSeconds(36)` → `QR_EXPIRED`
- issuedAt `now.plusSeconds(5)` → accepted; `now.plusSeconds(6)` → `QR_EXPIRED` (phone clock ahead)
- serial in `revokedCertificateSerials` → `REVOKED`; employee in `revokedEmployeeIds` → `REVOKED`
- employee mapped to `"LONG_AN"` at a `"BINH_DUONG"` gate → `NOT_AUTHORISED`; employee missing from map → `NOT_AUTHORISED`
- nonce already marked in an in-test `NonceRegistry` → `REPLAYED`

`QrValidatorTest`:
- `validCredentialHasNoViolation`
- `returnsFirstViolationInRuleOrder`: credential both expired-certificate and replayed → `CERTIFICATE_EXPIRED`
- `newRuleIsAddedWithoutChangingExistingRules`: build a `QrValidator` with standard rules plus a lambda rule returning `NOT_AUTHORISED` → that reason is returned.

- [ ] **Step 2: Run, see failures, implement, run until green** (`mvn -B verify` → `BUILD SUCCESS`)

- [ ] **Step 3: Commit** `git commit -m "Add QR validation rules"`

---

### Task 4: Local Store on SQLite

**Files:**
- Create: `access/AccessEvent.java`, `AccessResult.java`, `UnlockStatus.java`
- Create: `store/AccessEventLog.java`, `Outbox.java`, `InterruptedUnlocks.java`, `ReferenceDataReader.java`, `ReferenceDataWriter.java`, `LocalDatabase.java`, `SqliteAccessEventLog.java`, `SqliteNonceRegistry.java`, `SqliteReferenceData.java`
- Test: `store/SqliteAccessEventLogTest.java`, `SqliteNonceRegistryTest.java`, `SqliteReferenceDataTest.java`

**Interfaces:**
- Consumes: Task 3 `ReferenceData`, `NonceRegistry`, `RejectionReason`; Task 1 `Direction`, `GateIdentity`; Task 2 `Ed25519.decodePublicKey`
- Produces:
  - `enum AccessResult { ACCEPTED, REJECTED }`
  - `enum UnlockStatus { NOT_REQUESTED, PENDING, UNLOCKED, UNLOCK_FAILED, UNLOCK_UNKNOWN }`
  - `record AccessEvent(UUID id, String gateId, String factoryId, Direction direction, String employeeId, Instant occurredAt, AccessResult result, RejectionReason rejectionReason, UnlockStatus unlockStatus)` with `static AccessEvent accepted(GateIdentity gate, String employeeId, Instant at)` (status `PENDING`, new random UUID), `static AccessEvent rejected(GateIdentity gate, String employeeId, RejectionReason reason, Instant at)` (status `NOT_REQUESTED`; employeeId may be null for `MALFORMED`), `AccessEvent withUnlockStatus(UnlockStatus s)`
  - `interface AccessEventLog { void append(AccessEvent e); void updateUnlockStatus(UUID id, UnlockStatus s); Optional<AccessEvent> find(UUID id); }`
  - `interface Outbox { List<AccessEvent> pending(int limit); void markSynced(Collection<UUID> ids); int pendingCount(); }`
  - `interface InterruptedUnlocks { List<UUID> resolveAsUnknown(); }`
  - `interface ReferenceDataReader { ReferenceData current(); }`
  - `interface ReferenceDataWriter { void replace(ReferenceData data); }`
  - `final class LocalDatabase implements AutoCloseable { static LocalDatabase open(Path file); Connection connection(); }` sets `PRAGMA journal_mode=WAL`, `PRAGMA synchronous=FULL`, creates tables if missing.
  - `SqliteAccessEventLog(LocalDatabase) implements AccessEventLog, Outbox, InterruptedUnlocks`; `SqliteNonceRegistry(LocalDatabase) implements NonceRegistry`; `SqliteReferenceData(LocalDatabase) implements ReferenceDataReader, ReferenceDataWriter` (returns `ReferenceData.empty()` before the first `replace`).

Schema is the builder's call. `pending` returns unsynced events in `occurredAt` order. `resolveAsUnknown` turns every `PENDING` event into `UNLOCK_UNKNOWN` and returns their IDs. `replace` runs in one transaction.

- [ ] **Step 1: Write tests** (each uses `@TempDir Path dir`, `LocalDatabase.open(dir.resolve("gate.db"))`)

`SqliteAccessEventLogTest`:
- `appendedEventSurvivesReopen`: append, close, reopen, `find` returns an equal event.
- `updatesUnlockStatus`
- `pendingReturnsUnsyncedEventsOldestFirstUpToLimit`: 3 events, `pending(2)` → first two by time.
- `markSyncedRemovesEventsFromOutbox`: `pendingCount()` 3 → 1 after marking two.
- `resolveAsUnknownTurnsPendingUnlocksIntoUnknown`: one `PENDING`, one `UNLOCKED` → returns only the first ID, its status now `UNLOCK_UNKNOWN`.
- `walModeIsEnabled`: `PRAGMA journal_mode` returns `wal`.

`SqliteNonceRegistryTest`: `unusedNonceIsNotUsed`, `markedNonceIsUsedAfterReopen`.

`SqliteReferenceDataTest`: `emptyBeforeFirstDownload`, `replaceStoresAndReturnsAllFields` (server key equal by encoding), `replaceOverwritesPreviousData`.

- [ ] **Step 2: Run, see failures, implement, run until green**

- [ ] **Step 3: Commit** `git commit -m "Add SQLite local store with outbox and reference data"`

---

### Task 5: Turnstile adapters

**Files:**
- Create: `turnstile/TurnstileGateway.java`, `UnlockResult.java`, `SimulatedTurnstileGateway.java`, `RetryingTurnstileGateway.java`, `HttpTurnstileGateway.java`
- Test: `turnstile/SimulatedTurnstileGatewayTest.java`, `RetryingTurnstileGatewayTest.java`, `HttpTurnstileGatewayTest.java`, `TurnstileGatewayContractTest.java`

**Interfaces:**
- Produces:
  - `enum UnlockResult { UNLOCKED, FAILED, UNKNOWN }`
  - `interface TurnstileGateway { UnlockResult unlock(String gateId, UUID eventId); }`
  - `final class SimulatedTurnstileGateway implements TurnstileGateway { static SimulatedTurnstileGateway always(UnlockResult r); static SimulatedTurnstileGateway sequence(UnlockResult... results); List<UUID> requests(); }` (`sequence` repeats its last result once exhausted)
  - `final class RetryingTurnstileGateway implements TurnstileGateway { RetryingTurnstileGateway(TurnstileGateway delegate, int maxRetries); }`. Retries only on `UNKNOWN`, same `eventId` every time.
  - `final class HttpTurnstileGateway implements TurnstileGateway { HttpTurnstileGateway(HttpClient client, URI baseUrl, Duration timeout); }`. Sends `POST {baseUrl}/gates/{gateId}/unlock` with header `Idempotency-Key: {eventId}` and an empty body. 2xx → `UNLOCKED`. Any other status → `FAILED`. `HttpTimeoutException` or `IOException` → `UNKNOWN`. `InterruptedException` → restore the interrupt flag, then `UNKNOWN`.

- [ ] **Step 1: Write tests**

`RetryingTurnstileGatewayTest`:
- `retriesUnknownUntilResolved`: `sequence(UNKNOWN, UNLOCKED)`, retries 2 → `UNLOCKED`, 2 requests, both with the same eventId.
- `givesUpAfterMaxRetries`: `always(UNKNOWN)`, retries 2 → `UNKNOWN`, 3 requests.
- `doesNotRetryFailure`: `always(FAILED)` → `FAILED`, 1 request.

`HttpTurnstileGatewayTest`: uses `com.sun.net.httpserver.HttpServer` on port 0.
- `successfulResponseUnlocks`: handler returns 200. Assert the method is POST, the path is `/gates/G1/unlock`, and the `Idempotency-Key` header equals the event ID.
- `serverErrorFails`: handler returns 500 → `FAILED`.
- `slowResponseIsUnknown`: handler sleeps 500 ms, timeout 100 ms → `UNKNOWN`.
- `unreachableTurnstileIsUnknown`: base URL points to a closed port → `UNKNOWN`.

`TurnstileGatewayContractTest` (the Liskov check): an abstract base class with `abstract TurnstileGateway gatewayThatUnlocks()` and the tests `unlockReturnsUnlockedForAnyGateAndEvent` and `repeatedUnlockWithSameEventIdIsAccepted`. Nested subclasses supply `SimulatedTurnstileGateway.always(UNLOCKED)`, `HttpTurnstileGateway` against a 200 server, and `RetryingTurnstileGateway` wrapping the simulator.

- [ ] **Step 2: Run, see failures, implement, run until green**

- [ ] **Step 3: Commit** `git commit -m "Add turnstile gateway with HTTP, simulated and retrying adapters"`

---

### Task 6: Alerts, gate access flow and startup recovery

**Files:**
- Create: `alert/Alert.java`, `AlertType.java`, `AlertSink.java`, `Alerts.java`
- Create: `access/GateStation.java`, `access/StartupRecovery.java`
- Test: `alert/AlertsTest.java`, `access/GateStationTest.java`, `access/StartupRecoveryTest.java`

**Interfaces:**
- Consumes: everything from Tasks 1–5
- Produces:
  - `enum AlertType { UNLOCK_UNKNOWN, GATE_OFFLINE_TOO_LONG, PENDING_EVENTS_HIGH }`
  - `record Alert(AlertType type, String detail, Instant raisedAt)`
  - `interface AlertSink { void raise(Alert alert); }`
  - `final class Alerts implements AlertSink { List<Alert> active(); void resolve(AlertType type); boolean isActive(AlertType type); }`. Raising a type that is already active, with the same detail, does not add a duplicate.
  - `final class GateStation { GateStation(GateIdentity gate, CredentialCodec codec, QrValidator validator, ReferenceDataReader referenceData, NonceRegistry nonces, AccessEventLog events, TurnstileGateway turnstile, AlertSink alerts, Clock clock); AccessEvent handleScan(String rawQr); }`
  - `final class StartupRecovery { StartupRecovery(InterruptedUnlocks interrupted, AlertSink alerts, Clock clock); void run(); }`. Raises one `UNLOCK_UNKNOWN` alert per recovered event, with the event ID as the detail.

`handleScan` order (spec, Access flow):
1. Decode. On `MalformedCredentialException`, append a rejected event with reason `MALFORMED` and no employee, then return it.
2. Validate against `ValidationContext(referenceData.current(), gate, clock.instant())`. If a rule fails, append a rejected event and return it.
3. On acceptance, call `nonces.markUsed(nonce)`, then append the accepted event with status `PENDING`.
4. Call `turnstile.unlock(gateId, eventId)`.
5. Map the result: `UNLOCKED`→`UNLOCKED`, `FAILED`→`UNLOCK_FAILED`, `UNKNOWN`→`UNLOCK_UNKNOWN`. Update the stored status. If the result is `UNKNOWN`, raise an `UNLOCK_UNKNOWN` alert with the event ID as detail.
6. Return the updated event.

- [ ] **Step 1: Write tests**

`GateStationTest` uses a real SQLite store in a `@TempDir`, `ServerAuthority`, a `Phone`, and `Clock.fixed`:
- `validQrUnlocksAndRecordsEvent`: status `UNLOCKED`; the stored event equals the returned one.
- `eventIsStoredBeforeUnlockIsRequested`: the turnstile is a lambda `TurnstileGateway` that calls `events.find(eventId)` and asserts the event is present with status `PENDING`, then returns `UNLOCKED`.
- `screenshotCannotBeUsedTwice`: the same QR string scanned twice. The second scan is `REJECTED` with `REPLAYED`, and the turnstile received only 1 request.
- `@ParameterizedTest` over `"", "abc", "v1.a.b"`: `garbageInputIsRecordedAsMalformedWithoutUnlock`. The result is `REJECTED/MALFORMED`, status `NOT_REQUESTED`, the turnstile got 0 requests, and `pendingCount()` is 1.
- `revokedEmployeeIsRejectedAndRecorded`
- `turnstileTimeoutIsRecordedAsUnknownAndAlertsGuard`: `always(UNKNOWN)` → status `UNLOCK_UNKNOWN`, and `alerts.isActive(UNLOCK_UNKNOWN)`.
- `turnstileFailureIsRecordedWithoutAlert`

`StartupRecoveryTest`: append a `PENDING` event, then run recovery. The event becomes `UNLOCK_UNKNOWN` and one alert is raised with the event ID as detail.

`AlertsTest`: `raiseAndResolve`, `duplicateRaiseIsIgnored`.

- [ ] **Step 2: Run, see failures, implement, run until green**

- [ ] **Step 3: Commit** `git commit -m "Add gate access flow, alerts and startup recovery"`

---

### Task 7: Sync Client

**Files:**
- Create: `sync/EventIntake.java`, `ReferenceDataSource.java`, `GateStatusReceiver.java`, `GateStatus.java`, `IntakeUnavailableException.java`, `SyncClient.java`, `SyncReport.java`, `demo/InMemoryCentral.java`
- Test: `sync/SyncClientTest.java`

**Interfaces:**
- Consumes: `Outbox`, `ReferenceDataWriter`, `Alerts`, `ConfigReader`, `GateIdentity`, `AccessEvent`
- Produces:
  - `interface EventIntake { Set<UUID> submit(List<AccessEvent> events); }`. The call returns the acknowledged IDs, or throws `IntakeUnavailableException`, which is unchecked.
  - `interface ReferenceDataSource { ReferenceData fetch(); }`. Throws `IntakeUnavailableException` when offline.
  - `interface GateStatusReceiver { void report(GateStatus status); }`
  - `record GateStatus(String gateId, Instant reportedAt, Instant lastSuccessfulSync, int pendingEvents, List<Alert> alerts)`
  - `record SyncReport(boolean connected, int uploaded)`
  - `final class SyncClient { SyncClient(GateIdentity gate, Outbox outbox, ReferenceDataWriter referenceData, EventIntake intake, ReferenceDataSource source, GateStatusReceiver statusReceiver, Alerts alerts, ConfigReader config, Clock clock); SyncReport syncOnce(); }`. `lastSuccessfulSync` starts at `clock.instant()` when the client is constructed.
  - `final class InMemoryCentral implements EventIntake, ReferenceDataSource, GateStatusReceiver { InMemoryCentral(ReferenceData data); void goOffline(); void goOnline(); void failAfter(int acceptedEvents); int storedEventCount(); List<GateStatus> statusReports(); }`. Events are stored deduplicated by ID. `failAfter(n)` stores the next n events, then throws `IntakeUnavailableException` without returning any acknowledgements, which simulates a connection drop before the ack arrives.

`syncOnce`:
1. Loop: take `outbox.pending(batchSize)` and stop when it's empty. Call `submit`, then `markSynced(acknowledged)`. On `IntakeUnavailableException`, stop and mark the client disconnected.
2. If still connected, call `referenceData.replace(source.fetch())`. If that throws `IntakeUnavailableException`, mark the client disconnected.
3. If connected, set `lastSuccessfulSync` to now and resolve `GATE_OFFLINE_TOO_LONG`. If disconnected and `now - lastSuccessfulSync > maxOfflineDuration`, raise `GATE_OFFLINE_TOO_LONG`.
4. If `pendingCount() > threshold`, raise `PENDING_EVENTS_HIGH`. Otherwise resolve it.
5. If connected, call `statusReceiver.report(...)`.
6. Return the report.

- [ ] **Step 1: Write tests**
- `uploadsAllPendingEventsInBatches`: 250 events, batch size 100 → `uploaded` is 250, `storedEventCount` is 250, `pendingCount` is 0.
- `connectionDropMidBatchLosesNothingAndDuplicatesNothing`: 10 events and `failAfter(4)`. The first sync reports `connected=false` and leaves `pendingCount` at 10. After `goOnline`, the second sync leaves `storedEventCount` at 10 and `pendingCount` at 0.
- `acknowledgedEventsAreNotResentAfterRestart`: sync 5 events, close and reopen the store, add 2 more, sync again. Central receives only the 2 new IDs in the second call; check this with a counting `EventIntake` wrapper.
- `downloadsReferenceDataWhenOnline`
- `offlineTooLongRaisesAlertAndReconnectResolvesIt`: a mutable test clock is advanced by 4 h 1 min while offline → alert active. After going online and syncing → alert resolved.
- `tooManyPendingEventsRaisesAlert`: the threshold is 3 in custom `GateSettings`, with 4 events while offline → `PENDING_EVENTS_HIGH` active.
- `reportsGateStatusWithAlerts`: an `UNLOCK_UNKNOWN` alert is active → the reported status contains it and the correct pending count.

- [ ] **Step 2: Run, see failures, implement, run until green**

- [ ] **Step 3: Commit** `git commit -m "Add sync client with outbox upload, reference download and gate status"`

---

### Task 8: Demo

**Files:**
- Create: `demo/Demo.java`
- Modify: `README.md`, the Demo section
- Test: `demo/DemoTest.java`

**Interfaces:**
- Consumes: all of the above
- Produces: `final class Demo { public static void main(String[] args); static void run(Path databaseFile, PrintStream out); }`

`run` wires the following:
- `LocalDatabase.open(databaseFile)` with `SqliteAccessEventLog`, `SqliteNonceRegistry` and `SqliteReferenceData`
- `GateSettings.defaults()`
- `RetryingTurnstileGateway(SimulatedTurnstileGateway.sequence(UNLOCKED, UNKNOWN, UNKNOWN, UNKNOWN, UNLOCKED), 2)`: scan 1 unlocks, scan 5 exhausts its three attempts, the offline scan unlocks
- a `ServerAuthority`, and an `InMemoryCentral` seeded with its public key and two employees: `E001` at `BINH_DUONG`, and `E002` at `BINH_DUONG`, revoked
- the expired QR is `phone.showQr(now.minusSeconds(40))`
- a gate `BD-GATE-01` at `BINH_DUONG`, direction `ENTRY`

It runs a startup recovery, an initial sync, then the following scans, printing one line each:
1. a valid scan by E001
2. the same QR again (replayed)
3. a QR made 40 s earlier (expired)
4. a scan by the revoked E002
5. a new E001 QR while the turnstile times out (unknown plus alert)

Then it takes central offline and does one more valid scan, which is accepted. It brings central back online, syncs, and prints the stored event count and active alerts. `main` uses `Path.of("gate-station-demo.db")` and `System.out`.

- [ ] **Step 1: Write `DemoTest.runsEndToEnd`**: run into a `@TempDir` and capture the output. Assert it contains `UNLOCKED`, `REPLAYED`, `QR_EXPIRED`, `REVOKED`, `UNLOCK_UNKNOWN`, and `central stored 6 events`.
- [ ] **Step 2: Implement until green.** Run `mvn -B verify` and expect `BUILD SUCCESS`. Then run `java -jar target/gate-station-0.1.0.jar` and check the output by eye.
- [ ] **Step 3: Update the README demo section with the real output, then commit** `git commit -m "Add end-to-end gate station demo"`

---

### Task 9: SOLID write-up

**Files:**
- Create: `docs/solid.md`

Content (spec, "SOLID write-up"), with one section each for S, O, L, I, D. Each section:
- names the real classes
- shows a short excerpt (at most about 15 lines) copied from the code
- explains in TMC terms which change the principle protects against
- cites the decision or requirement

Mapping:
- **S:** `GateStation` coordinates only. `QrValidator`, `SqliteAccessEventLog`, `HttpTurnstileGateway` and `SyncClient` each have one reason to change (AD-08, NFR-09).
- **O:** `ValidationRule` and `QrValidator.standard`. `RetryingTurnstileGateway` adds retry without editing adapters. Point to `newRuleIsAddedWithoutChangingExistingRules`.
- **L:** `TurnstileGatewayContractTest` runs the same tests against the simulated, HTTP and retrying gateways (Section 6.2 example).
- **I:** `AccessEventLog`, `Outbox`, `InterruptedUnlocks`, `NonceRegistry` and the `ReferenceDataReader`/`Writer` split. Each is implemented by one SQLite class but consumed separately.
- **D:** `GateStation` and `SyncClient` depend on interfaces in their own packages. `Demo` is the only place that wires concrete classes.

Close with an honest "Where we bend the rules" section:
- `AccessEvent` carries nullable fields.
- `SqliteAccessEventLog` implements three interfaces.
- The single-gate replay limit.
- The assumed Turnstile API format.

Before committing, re-read the draft against the CLAUDE.md writing rules and remove any AI-style phrasing.

- [ ] **Step 1: Write it, checking each excerpt against the current code**
- [ ] **Step 2: Commit** `git commit -m "Add SOLID write-up"`

---

### Task 10: Team reflection and merge

**Files:**
- Create: `docs/reflection.md`

- [ ] **Step 1: Ask the user for team facts** (what was hard, what was argued about, what surprised them, what they would change). This is the only input needed. Use whatever they give, in their own wording where possible.
- [ ] **Step 2: Write it in the team's voice.** Cover:
  - decisions that held up: Modular Monolith over Service-Based (3.8), edge validation (AD-04)
  - the M1→M2 change from QR Display to device-bound QR, and why
  - accepted trade-offs: stale revocations offline, fail-secure turnstile, phone handover, the cross-gate replay window
  - what building the gate code showed, drawn from what actually happened in Tasks 1–8
  - what we would do differently

  Anything that depends on team facts the user did not supply stays as a marked gap: `> [Team to fill: ...]`. Target 1–3 pages.
- [ ] **Step 3: Commit** `git commit -m "Add team reflection"`
- [ ] **Step 4: Final check and merge.** Run `mvn -B verify` (expect `BUILD SUCCESS`) and `git log` (expect no `Co-Authored-By` lines). Then run `git checkout main && git merge --no-ff feature/gate-station -m "Merge gate station module"`.
