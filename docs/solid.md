# SOLID in the TMC Mobile App

Section 6.2 of our architecture document lists the SOLID principles as design guidelines, and CO-3 makes them a constraint. This document shows where each principle appears in the TMC Mobile App, an Android app in Kotlin that lives in `mobile-app/`, and where we did not follow it fully.

The app does what the M2 design asks of the phone:
- It creates a key pair in the Android Keystore on first sign-in.
- It gets a device certificate from the server.
- It signs a fresh QR pass every 15 seconds without a network.
- It shows the employee's attendance, cached for offline use.

The server is a fake that runs inside the app. Every class named below exists in `mobile-app/`. The excerpts are copied from the code, shortened only where marked with `...`.

## Modules

| Module | Kind | Contents |
|---|---|---|
| `:core:domain` | Pure Kotlin | Entities, use cases, and the interfaces (ports) they need |
| `:core:data` | Android library | Room cache, DataStore session and certificate, network status, offline-first repository |
| `:core:security` | Android library | Android Keystore key store and signer |
| `:fake-server` | Pure Kotlin | In-process stand-in for tmc-central |
| `:app` | Android app | Compose screens, ViewModels, Hilt wiring |

These are the module dependencies, taken from the `build.gradle.kts` files:

```
:core:domain   -> (nothing in the project)
:core:data     -> :core:domain
:core:security -> :core:domain
:fake-server   -> :core:domain
:app           -> all of the above
```

## S: Single Responsibility

Each class has one reason to change. In the domain, every user action is one class:

| Class | Changes when |
|---|---|
| `SignInAndEnroll` | The order of sign-in and enrollment changes |
| `EnrollDevice` | The steps to get a device certificate change |
| `GeneratePass` | The way one pass is built and signed changes |
| `ObservePassState` | The refresh timing or expiry handling changes |
| `GetMonthlyAttendance` | The way a month is assembled changes |
| `RenewCertificate`, `GetDeviceStatus`, `SignOut` | Their single action changes |
| `IsDeviceEnrolled`, `GetSignedInEmployee` | What counts as enrolled, or where the employee comes from, changes |

`GeneratePass` builds one pass. It does not know about timers, storage or screens:

```kotlin
operator fun invoke(certificate: DeviceCertificate): Pass {
    val issuedAt = clock.instant()
    val nonce = nonces.next()
    val signature = signer.sign(PassPayloadCodec.signedContent(certificate, issuedAt, nonce))
    return Pass(
        payload = PassPayloadCodec.encode(certificate, issuedAt, nonce, signature),
        issuedAt = issuedAt,
        refreshAt = issuedAt.plus(refreshInterval),
    )
}
```

The QR format lives in `PassPayloadCodec`. `GeneratePass` sets each pass's `refreshAt`, 15 seconds after issue. `ObservePassState` checks once a second and asks for a new pass when `refreshAt` has passed. Drawing the ring lives in `CountdownRing` in the UI. Changing the payload format touches one object.

Sign-in and certificate renewal both enroll the device. `EnrollDevice` holds those steps once, and both use cases call it:

```kotlin
suspend operator fun invoke(token: String): DeviceCertificate =
    enrollment.issueCertificate(token, keyStore.createKey()).also { certificates.save(it) }
```

The start screen follows the same rule. `StartViewModel` used to read three stores and decide itself whether the phone was enrolled. That decision is now the use case `IsDeviceEnrolled`, and the ViewModel only maps the answer to a destination. `PassViewModel` gets the employee from `GetSignedInEmployee` instead of reading `SessionStore`.

ViewModels hold screen state and turn use case results into what the screen shows. `SignInViewModel` turns the outcome of `SignInAndEnroll` into what the screen shows, and the sign-in logic stays in the use case:

```kotlin
val error = try {
    signInAndEnroll(current.employeeId, current.password)
    null
} catch (e: InvalidCredentialsException) {
    WRONG_CREDENTIALS
} catch (e: ServerUnreachableException) {
    UNREACHABLE
}
```

## O: Open/Closed

The attendance screen flags days that need attention. Each flag is a separate rule:

```kotlin
fun interface AttendanceRule {

    fun flags(record: AttendanceRecord): List<DayFlag>
}
```

```kotlin
class MissingExitRule : AttendanceRule {

    override fun flags(record: AttendanceRecord): List<DayFlag> =
        if (record.entry != null && record.exit == null) listOf(FLAG) else emptyList()
    ...
}
```

`AttendanceSummarizer` runs every rule and never looks inside one:

```kotlin
.map { record -> AttendanceDay(record, rules.flatMap { it.flags(record) }, workedOn(record)) }
```

Suppose HR later wants late arrivals flagged. That is a new class added to the list. The test `newRuleAddsFlagWithoutChangingExistingRules` in `AttendanceSummarizerTest` does exactly this with a "Late" rule, and neither `MissingExitRule` nor `ManualEntryRule` changes. The severity on each `DayFlag` decides whether it counts toward "To review". A new rule also chooses its own severity, so the screen needs no change either.

`AttendanceSummarizer` does not pick its rules. The rule list lives in `DomainModule`, the composition root:

```kotlin
@Provides
fun attendanceRules(): List<AttendanceRule> = listOf(MissingExitRule(), ManualEntryRule())
```

A new default rule is a new class plus one entry in this list. `AttendanceSummarizer` does not change.

## L: Liskov Substitution

Section 6.2 says any implementation of an interface should work wherever the interface is expected. We check this with a shared contract, not by trusting it.

`DeviceKeyStoreContract` in the domain test fixtures holds the checks every key store must pass:

```kotlin
fun signaturesVerifyWithPublicKey() {
    store.createKey()
    val data = "pass content".toByteArray()

    val signature = signer.sign(data)

    assertTrue(EcdsaP256.verify(assertNotNull(store.publicKey()), data, signature))
}
```

Two implementations run it:
- `InMemoryDeviceKeyStore`, the software key used in unit tests, runs it in `DeviceUseCasesTest.inMemoryKeyStoreMeetsTheContract`.
- `AndroidKeystoreDeviceKeyStore`, the real Android Keystore, runs it in the instrumented test `AndroidKeystoreContractTest` on a phone or emulator. The key is hardware-backed on phones with secure hardware, but not on the emulator.

Both pass. Because they do, every domain test that uses the in-memory store also tells us something true about the real one.

The server works the same way:
- `FakeTmcServer` (the app's fake) and `StubServer` (a small test double) both implement `AuthApi`, `EnrollmentApi` and `AttendanceApi`.
- `ServerApiContract` holds what any server must do:
  - accept the right password and reject a wrong one;
  - fail as unreachable when offline;
  - issue a certificate that binds the device key and verifies with the server key;
  - reject a forged token;
  - return attendance only for the requested month.
- `StubServerContractTest` and `FakeTmcServerContractTest` run that contract against both servers, so the use case tests that run against `StubServer` hold for the fake the app ships with.
- An HTTP client for the real tmc-central would be a third implementation, and it would have to pass the same contract.

## I: Interface Segregation

The server is not one big interface. Each use case depends only on the part it uses:

```kotlin
interface AuthApi {

    suspend fun signIn(employeeId: String, password: String): Session
}

interface EnrollmentApi {

    suspend fun issueCertificate(token: String, devicePublicKey: ByteArray): DeviceCertificate
}

interface AttendanceApi {

    suspend fun monthlyAttendance(token: String, month: YearMonth): List<AttendanceRecord>
}
```

`SignInAndEnroll` sees only `AuthApi`. `EnrollDevice` sees only `EnrollmentApi`. `OfflineFirstAttendanceRepository` sees only `AttendanceApi`.

The device key is split the same way:
- `DeviceKeyStore` creates, reads and deletes the key.
- `PayloadSigner` has one method, `sign`.

`GeneratePass` runs every 15 seconds and receives only a `PayloadSigner`, so pass generation cannot delete or replace the device key. Only `EnrollDevice`, `IsDeviceEnrolled`, `SignOut` and `GetDeviceStatus` get a `DeviceKeyStore`.

## D: Dependency Inversion

The interfaces are owned by the domain (`com.tmc.mobile.domain.port`), and the outer modules implement them:

| Domain interface | Implementation | Module |
|---|---|---|
| `DeviceKeyStore`, `PayloadSigner` | `AndroidKeystoreDeviceKeyStore`, `AndroidKeystorePayloadSigner` | `:core:security` |
| `SessionStore`, `CertificateStore` | `DataStoreSessionStore`, `DataStoreCertificateStore` | `:core:data` |
| `AttendanceCache` | `RoomAttendanceCache` | `:core:data` |
| `NetworkStatus` | `ConnectivityNetworkStatus` | `:core:data` |
| `AttendanceRepository` | `OfflineFirstAttendanceRepository` | `:core:data` |
| `AuthApi`, `EnrollmentApi`, `AttendanceApi` | `FakeTmcServer` | `:fake-server` |

The build enforces this, not just a convention. `:core:domain` is a plain Kotlin module with no Android dependency, so a use case that imports Room, the Keystore or a Compose class does not compile. The domain's 36 unit tests run on a normal JVM, with no emulator and no Android framework.

Concrete classes are wired together in the Hilt modules in `app/di`, plus `LocalStorage` in `:core:data`, which builds the Room and DataStore classes privately. The fake server is named in exactly one file:

```kotlin
@Provides
@Singleton
fun server(network: NetworkStatus, clock: Clock): FakeTmcServer = FakeTmcServer(network, clock)

@Provides
fun authApi(server: FakeTmcServer): AuthApi = server
```

Connecting the app to a real tmc-central means replacing `ServerModule`. No screen, ViewModel or use case changes.

One moment while building showed the rule at work. The first version of `DataModule` in `:app` built the Room database and DataStore itself. The build failed, because `:core:data` keeps Room and DataStore as private implementation details, so `:app` could not see their types. The quick fix was to expose those libraries to `:app`. Instead, we added `LocalStorage` to `:core:data`. It builds the database and preferences internally and hands out only domain interfaces:

```kotlin
class LocalStorage(context: Context) {
    ...
    val sessionStore: SessionStore = DataStoreSessionStore(preferences)

    val certificateStore: CertificateStore = DataStoreCertificateStore(preferences)

    val attendanceCache: AttendanceCache = RoomAttendanceCache(database.attendanceDao())
}
```

`:app` now has no dependency on Room or DataStore at all.

## Where we bend the rules

- The app's QR format differs from the M2 design. M2 specifies Ed25519 signatures. The app signs with ECDSA P-256 in a `v2` format, because the Android Keystore supports P-256 on every phone the app targets (Android 8 and later), while Keystore support for Ed25519 arrived only in recent Android versions. The format keeps the same structure and uses standard encodings (X.509 public keys, DER signatures). A gate built to the M2 design would still reject these codes. Its codec, both signature checks and the stored server key all assume Ed25519, so the gate side is not closed to this change the way the attendance rules are.
- `FakeTmcServer` implements three interfaces. The real server would too. The split matters on the client side, where each use case receives only the one it needs.
- `DeviceKeyStoreContract` is a plain class of check functions, not an abstract test class. Domain tests use JUnit 6 and Android instrumented tests use JUnit 4, so a shared abstract test class would only work for one of them. Each test class calls the contract's functions itself.
- `AndroidKeystoreDeviceKeyStore` and `AndroidKeystorePayloadSigner` must use the same key alias. They are separate classes for Interface Segregation, but they share a key. Nothing but the shared default value keeps them in step.
- Wiring the domain takes more code. The domain does not depend on Hilt, so its classes carry no `@Inject` annotations. `DomainModule` builds every use case by hand. This is the cost of keeping the domain free of frameworks.
- `AttendanceViewModel` decides which months to offer (the current month and the two before it). That rule belongs in the domain, but it lives in the ViewModel.
- The 15-second refresh interval is a constant in `GeneratePass`, and `PassViewModel` reads it for the countdown ring. AD-13 asks for QR validity to be configuration.
- `DeviceCertificate` builds its own signed content, so the entity changes when the wire format changes.
- `PassPayloadCodec.decode` and `EcdsaP256.verify` are used only by tests. Verifying a pass is the gate's job.
- Session data and cached attendance are stored without encryption. AD-11 asks for personal data to be encrypted at rest.
- ViewModels catch specific exceptions to choose a message. Each ViewModel ends with a catch-all, so an unexpected failure shows "Something went wrong" instead of crashing. Even so, a new failure type that deserves its own message still needs a new `catch`. A sealed result type would make that impossible to forget, at the cost of more types.
