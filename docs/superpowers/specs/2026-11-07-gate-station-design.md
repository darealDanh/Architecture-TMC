# Gate Station Software, SOLID Write-up and Team Reflection: Design

Date: 2026-11-07

## Intent

TMC is a software architecture class project. The team has finished the design work (M1 and M2) in `BTL ARCHITECTURE.txt`: requirements, style selection (Modular Monolith central system with offline gate stations), logical and physical architecture, decisions AD-01 to AD-13, design principles (Section 6.2), the two M2 issues and three architecture views. None of it has been implemented.

This milestone has four deliverables:

1. A git repository for the project.
2. One module of the architecture implemented as working code, to show the design holds up in practice.
3. A SOLID write-up that shows how that code applies each principle, pointing at real classes.
4. A team reflection on the whole project.

The module is the Gate Station Software. It carries the decisions the design depends on most: offline validation (AD-04, AD-05), write-before-unlock (AD-06), unique event IDs and idempotent sync (AD-07), adapters for external systems (AD-08), and both M2 solutions (device-bound QR and the outbox). It also gives a natural example of each SOLID principle, and it can run and be tested without real hardware or a central server.

## Goals

- A Java implementation of the four parts of Gate Station Software from Section 4.6: QR Validator, Local Store, Turnstile Adapter, Sync Client, plus the Gate Software orchestration of the C&C view, Section 2.1.
- Behaviour follows the access flow of C&C Section 2.1 and the M2 handling table exactly.
- Tests that prove the key properties: valid code opens the gate, copied or expired code is rejected, a turnstile timeout is retried and, if still unresolved, recorded as `UNLOCK_UNKNOWN` with a guard alert, and sync neither loses nor duplicates events when the connection breaks mid-batch.
- CI that builds and tests every pull request and every push to `main`, so a broken PR is visible before merge.
- `docs/solid.md`: one section per principle, each tied to named classes and to the requirement or decision it serves.
- `docs/reflection.md`: the team's reflection, written in the team's voice.

## Non-goals

- tmc-central, the Mobile App and the Web Portal. Central is represented only by the `EventIntake` interface and the reference data the gate downloads, with in-memory fakes for tests and the demo.
- Real hardware. The QR scanner is a string input; the turnstile is reached through `TurnstileGateway`.
- Real encryption at rest, mutual TLS and VPN. The documents cite them from the design; the code does not implement them.
- A real Mobile App. A test helper signs QR codes the way the phone would (M2, Issue 2, steps 1 to 3).

## Anti-goals

The work fails, even if it runs, if:

- The code has comments of any kind. Names and structure must carry the meaning.
- The documents read as AI-generated: generic claims, filler, marketing tone, stock phrases, forced lists of three, summary closers.
- The reflection invents team experiences. Every point must come from the project document or from facts the team supplies.
- Any commit or PR carries Claude or AI attribution.
- The SOLID write-up makes claims the code does not back up.

## Constraints

- Java 17 language level, Maven, JUnit 5, SQLite through `org.xerial:sqlite-jdbc`. The local machine has JDK 26 and no Maven, so the project ships the Maven Wrapper (`mvnw`, `mvnw.cmd`) and compiles with `--release 17`.
- Rules in `CLAUDE.md`: no comments, clean code, plain prose, no AI attribution, PR-based workflow with CI.
- Code must stay consistent with `BTL ARCHITECTURE.txt`. Interface names come from the document: `TurnstileGateway`, `EventIntake`, `ConfigReader`.
- Minimal dependencies: sqlite-jdbc, JUnit 5, and an assertion library only if it clearly improves test readability (AssertJ). Crypto uses the JDK (Ed25519, available since Java 15). JSON is avoided; the QR payload uses a compact text format.

## How it works (decided)

### Access flow (C&C 2.1, AD-04, AD-06)

`GateStation.handleScan(String rawQr)`:

1. Parse the raw QR into a `ScannedCredential`. Unparseable input becomes a rejected event with reason `MALFORMED`.
2. Run every `ValidationRule` against it and the current reference data. The first failure gives the rejection reason.
3. Write the `AccessEvent` to the Local Store, accepted or rejected, before any unlock call (AD-06).
4. If accepted, call `TurnstileGateway.unlock(gateId, eventId)`. The event ID is the idempotency key.
5. Write the unlock result back to the stored event: `UNLOCKED`, `UNLOCK_FAILED` or `UNLOCK_UNKNOWN` (timeout).
6. Return the decision so the kiosk can show it.

Steps 1 to 5 never touch the network beyond the factory LAN.

### QR credential (AD-05, M2 Issue 2)

- Device certificate: employee ID, device public key (Ed25519), certificate expiry, signed by the server key.
- QR payload: certificate, timestamp, nonce, and a device signature over (certificate, timestamp, nonce).
- Encoding: base64url segments joined by `.`. No personal data besides the employee ID.

### Validation rules (FR-AT-01, Open/Closed)

Each rule is its own class implementing `ValidationRule`:

| Rule | Rejection reason |
|---|---|
| Server signature on the certificate | `INVALID_CERTIFICATE` |
| Certificate not expired | `CERTIFICATE_EXPIRED` |
| Device signature over the payload | `INVALID_SIGNATURE` |
| Timestamp within the validity window plus clock tolerance | `QR_EXPIRED` |
| Certificate or employee not revoked | `REVOKED` |
| Employee active and authorised for this gate's factory | `NOT_AUTHORISED` |
| Nonce not seen before | `REPLAYED` |

QR validity window and clock tolerance are read through `ConfigReader` (AD-13). Defaults: 30 s window, 5 s tolerance. Time comes from an injected `java.time.Clock`.

### Local Store (FR-AT-06, NFR-02, AD-06, AD-07)

- SQLite with WAL mode and `synchronous=FULL`.
- Tables: access events (with a sync status), used nonces, and reference data (server public key, revocations, active employees and their factories, gate configuration).
- Each event gets a UUID when it is created at the gate.
- On startup, events still marked as waiting for an unlock result become `UNLOCK_UNKNOWN` (M2, kiosk power loss).
- Interfaces are split by client (Interface Segregation): the access flow writes events; the Sync Client reads pending events and marks them synced; the validator reads reference data; the sync download replaces reference data.

### Turnstile Adapter (FR-AT-02, AD-08, Liskov)

- `TurnstileGateway` with one operation: unlock a gate for an event, returning `UNLOCKED`, `FAILED` or `UNKNOWN`.
- `HttpTurnstileGateway`: `java.net.http` POST to the configured Turnstile API base URL, idempotency key header, configurable timeout. A timeout maps to `UNKNOWN`. The real Turnstile API contract is not in the document; this request shape is our assumption and is stated as such in the docs.
- `SimulatedTurnstileGateway`: configurable to succeed, fail or time out. It must behave like the real one from the caller's view, which is the Liskov example from Section 6.2.
- `RetryingTurnstileGateway` (our call): a decorator that wraps any `TurnstileGateway` and retries an `UNKNOWN` result a configurable number of times with the same idempotency key, as the M2 handling table asks. Retrying is added without editing the adapters (Open/Closed). `TurnstileGateway` keeps its single operation; querying turnstile status is not used, because the document offers retry or query and retry needs no extra API.
- If the result is still `UNKNOWN` after retries, the gate raises a guard alert (M2: notify the security guard).

### Alerts (our call)

- The gate keeps a list of active alerts: `UNLOCK_UNKNOWN` for an event, offline longer than the configured maximum, Local Store pending events above the configured threshold.
- Alerts go out with the gate status upload, which Gate Monitoring uses to notify guards (FR-GS-03). The demo prints them to the console.
- The backup time source at each factory (M2) is infrastructure, not gate code. The code only applies the configured clock tolerance.

### Accepted limit: same screenshot at a second gate (our call)

Used nonces are stored per gate, and gates do not talk to each other offline. A copied code can therefore pass once at a different gate inside its validity window (about 30 s plus tolerance). We accept this, as the M2 document already accepts stale revocations offline. The short window keeps the risk small. Tests cover replay at one gate, and both `solid.md` and the reflection state the limit.

### Sync Client (NFR-07, AD-07, M2 outbox)

- Reads pending events in batches, submits them to `EventIntake`, and marks as synced only the events central acknowledges.
- A failure mid-batch leaves unacknowledged events pending for the next run. Central dedupes by event ID, so a resend is harmless.
- Downloads reference data and replaces the local copy.
- Uploads gate status (online, last sync time, pending count) for Gate Monitoring.
- `InMemoryEventIntake` fake: dedupes by ID and can be set to fail after N events.

### Demo

A `main` class wires the real Local Store, the simulated turnstile and the in-memory central, issues a few phone-style QR codes, and prints the result of each scan: valid, replayed screenshot, expired, revoked, turnstile timeout, then a sync.

### CI/CD

- `.github/workflows/ci.yml`: on `pull_request` and on `push` to `main`, set up Temurin JDK 17 with Maven cache, run `./mvnw -B verify` in `gate-station/`.
- On push to `main`, upload the built jar as a workflow artifact (the delivery half).
- Merges are blocked only when `main` has a branch protection rule that requires the CI check. That is a GitHub repository setting, applied once the GitHub repo exists.

### Repository setup (decided)

- Local git repository only. No remote exists yet; the team creates the GitHub repo and pushes.
- Commits use the machine's configured git identity, with no AI attribution.
- Default branch `main`. Work happens on a feature branch and is merged locally into `main`; once pushed to GitHub, later changes go through pull requests checked by CI.
- After pushing, the team adds a branch protection rule on `main` requiring the CI check. The README gives the steps.

### Repository layout

```
tmc/
  BTL ARCHITECTURE.txt
  CLAUDE.md
  README.md
  .gitignore
  .github/workflows/ci.yml
  gate-station/
    pom.xml, mvnw, mvnw.cmd, .mvn/wrapper/
    src/main/java/...
    src/test/java/...
  docs/
    solid.md
    reflection.md
    superpowers/specs/...
```

### SOLID write-up (`docs/solid.md`)

One section per principle. Each section names the classes involved, shows a short excerpt or class sketch, explains what change it protects against in TMC terms (for example, a new turnstile vendor or a new access rule), and cites the requirement or decision. It ends with a short, honest note on where the code bends a principle and why.

### Team reflection (`docs/reflection.md`)

Written as "we". Topics: decisions that held up (Modular Monolith over Service-Based, edge validation), the M1 to M2 change from server-side QR Display to device-bound QR, trade-offs accepted (stale revocations offline, fail-secure turnstile, phone handover not detectable), what building the gate code showed about the design, and what we would do differently. Grounded only in the project document, the code, and team facts supplied by the user. Target 1 to 3 pages.

The reflection is written last. Before writing it, the team supplies a few real facts (what was hard, what was argued about, what surprised them, what they would change). Without them, the parts that depend on team experience are left as clearly marked gaps, never invented.

## Left to the builder

- Package names and internal class breakdown beyond the names above.
- Exact SQLite schema and the QR payload field order.
- Batch size and retry interval for sync (configurable, with sensible defaults).
- Test structure and naming.
