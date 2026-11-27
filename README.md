# TMC Access Demo

Software architecture course project for TMC Manufacturing: QR code access control and attendance for two factories. The full design is in the team's architecture report.

This repository implements the Android Mobile App and a laptop-camera Gate Station demo. The app follows the M2 solution for copied QR codes:
- On first sign-in, the phone creates a private key in the Android Keystore and gets a device certificate from the server.
- It signs a fresh QR pass every 15 seconds, with or without a network.
- It shows the employee's attendance, and keeps a copy for offline use.

The server is a fake that runs inside the app. The gate uses its public verification key and demo employee directory; it never needs a connection to the phone. See `docs/solid.md` for the mobile app's design.

## Layout

| Path | Contents |
|---|---|
| `mobile-app/` | Android app and JVM gate station in one Gradle build |
| `mobile-app/gate-station/` | Local browser camera scanner, signed-pass validation, durable gate event log |
| `docs/solid.md` | How the app applies the SOLID principles |
| `docs/reflection.md` | Team reflection |
| `.github/workflows/ci.yml` | Builds and tests the app on every pull request and push to `main` |

## Running the app

1. Open the `mobile-app/` folder in Android Studio.
2. Wait for the Gradle sync to finish.
3. Pick a virtual device (we use `Medium_Phone_API_37.0`) and press Run.

Test accounts:

| Employee ID | Password | Name | Factory |
|---|---|---|---|
| E001 | `password` | Nguyen Van A | Binh Duong |
| E002 | `password` | Tran Thi B | Long An |

Things to try:
- Turn on airplane mode. The pass keeps refreshing and shows "Offline".
- Open Attendance offline. The current month shows saved data, and a month never opened shows "No data offline for this month".
- Sign out from the Device tab, then sign in as the other employee.

## Running the demo gate

The gate runs on the laptop. It uses the laptop webcam through a browser; Chrome or Edge works on `localhost`. Install Node.js and a JDK 17+ for the gate, then from the repository root:

```
cd mobile-app
npm ci --prefix gate-station
./gradlew :gate-station:run
```

On Windows PowerShell or cmd, use `gradlew.bat :gate-station:run`. Open `http://localhost:8765` on the laptop and click **Start camera**. Allow camera access, then hold the app's QR code inside the frame. The scanner stays ready for the next person; move each code away after the decision.

The default gate is **Binh Duong Entry**: E001 is accepted, E002 is rejected because E002 belongs to Long An. To run a Long An gate instead, use `./gradlew :gate-station:run --args=la`; `--args=bd` restores Binh Duong. A used pass is rejected as a replay until the app refreshes it (every 15 seconds). Keep the laptop clock and the device showing the app on the same time.

The gate verifies the certificate and phone signatures locally and saves every scan to `mobile-app/gate-station/gate-events.db` before reporting a decision. It serves the scanner library locally after the one-time `npm ci`, so scans do not need internet. **PASSED** means the pass was approved. No physical turnstile is connected; there is no central synchronization or production-grade encryption of the local event file.

## Building and testing from the command line

You need a JDK 17 or newer and the Android SDK. Android Studio installs both.

```
cd mobile-app
./gradlew assembleDebug test                # unit tests, no device needed
./gradlew connectedDebugAndroidTest         # instrumented tests, needs a running emulator or phone
```

Use `gradlew.bat` instead of `./gradlew` in Windows PowerShell or cmd. The instrumented tests check the real Android Keystore against the same contract as the in-memory test key store, and check that network detection follows airplane mode.

## Protecting `main` on GitHub

After pushing the repository, make the CI checks required so a pull request cannot be merged while a build or test fails:

1. Settings → Branches → Add branch protection rule.
2. Branch name pattern: `main`.
3. Enable "Require a pull request before merging" and "Require status checks to pass before merging".
4. Select the `android` check and save.

## Contributing

See `CONTRIBUTING.md` for the branch, check and merge rules.

