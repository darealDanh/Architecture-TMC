# TMC Mobile App

Software architecture course project for TMC Manufacturing: QR code access control and attendance for two factories. The full design is in the team's architecture report.

This repository implements the Mobile App from that design, an Android app in Kotlin with Jetpack Compose. It follows the M2 solution for copied QR codes:
- On first sign-in, the phone creates a private key in the Android Keystore and gets a device certificate from the server.
- It signs a fresh QR pass every 15 seconds, with or without a network.
- It shows the employee's attendance, and keeps a copy for offline use.

The server is a fake that runs inside the app. The code is structured to show the SOLID principles. See `docs/solid.md`.

## Layout

| Path | Contents |
|---|---|
| `mobile-app/` | Android app, a Gradle build with five modules |
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
