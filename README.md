# TMC Gate Station

Software architecture course project for TMC Manufacturing: QR code access control and attendance for two factories. The full design is in `BTL ARCHITECTURE.txt`.

This repository implements one module of that design, the Gate Station Software that runs at every gate. It validates device-bound QR codes offline, records each access event before unlocking the turnstile, and synchronises events with the central system without losing or duplicating them.

## Layout

| Path | Contents |
|---|---|
| `gate-station/` | Java 17 Maven project |
| `docs/solid.md` | How the code applies the SOLID principles |
| `docs/reflection.md` | Team reflection |
| `docs/superpowers/` | Design and implementation plan |
| `.github/workflows/ci.yml` | Build and test on every pull request and push to `main` |

## Build and test

Only a JDK 17 or newer is needed. The Maven Wrapper downloads Maven on first use.

```
cd gate-station
./mvnw verify        # Linux, macOS, WSL, Git Bash
mvnw.cmd verify      # Windows
```

## Demo

```
java -jar gate-station/target/gate-station-0.1.0.jar
```

The demo runs one gate at Binh Duong against a simulated turnstile and an in-memory central system. It stores events in `gate-station-demo.db` in the current directory.

```
Initial sync                   ONLINE    uploaded 0
E001 shows a fresh code        ACCEPTED  UNLOCKED
Same code scanned again        REJECTED  REPLAYED
E001 code from 40 s ago        REJECTED  QR_EXPIRED
E002 (revoked) shows a code    REJECTED  REVOKED
Turnstile stops answering      ACCEPTED  UNLOCK_UNKNOWN
Sync while WAN is down         OFFLINE   uploaded 0
E001 scans while offline       ACCEPTED  UNLOCKED
Sync after WAN returns         ONLINE    uploaded 6
central stored 6 events, 0 still pending at the gate
active alert: UNLOCK_UNKNOWN 759075d1-6308-4769-b09b-d122bccae123
```

Rejected scans are stored and synced too, because the audit log needs every attempt (FR-AT-04). The turnstile that stops answering is retried twice with the same event ID before the gate records `UNLOCK_UNKNOWN` and raises an alert for the security guard.

## Protecting `main` on GitHub

After pushing the repository, make the CI check required so a pull request cannot be merged while the build or tests fail:

1. Settings → Branches → Add branch protection rule.
2. Branch name pattern: `main`.
3. Enable "Require a pull request before merging" and "Require status checks to pass before merging".
4. Select the `build` check and save.
