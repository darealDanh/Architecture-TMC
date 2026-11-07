# SOLID in the TMC Mobile App

Section 6.2 of our architecture document lists the SOLID principles as design guidelines, and CO-3 makes them a constraint. This document shows where each principle appears in the TMC Mobile App, an Android app in Kotlin that lives in `mobile-app/`, and where we did not follow it fully.

The app does what the M2 design asks of the phone:
- It creates a key pair in the Android Keystore on first sign-in.
- It gets a device certificate from the server.
- It signs a fresh QR pass every 15 seconds without a network.
- It shows the employee's attendance, cached for offline use.

The server is a fake that runs inside the app. Every class named below exists in `mobile-app/`. The excerpts are copied from the code, shortened only where marked with `...`.

The repository also contains `gate-station/`, an earlier Java module for the gate side. This document is about the app.

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
| `SignInAndEnroll` | The sign-in and first enrollment steps change |
| `GeneratePass` | The way one pass is built and signed changes |
| `ObservePassState` | The refresh timing or expiry handling changes |
| `GetMonthlyAttendance` | The way a month is assembled changes |
| `RenewCertificate`, `GetDeviceStatus`, `SignOut` | Their single action changes |

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

The QR format lives in `PassPayloadCodec`. The countdown lives in `ObservePassState`. Drawing the ring lives in `CountdownRing` in the UI. Changing the refresh interval touches one constant. Changing the payload format touches one object.

ViewModels hold screen state and nothing else. `SignInViewModel` turns the outcome of `SignInAndEnroll` into what the screen shows, and the sign-in logic stays in the use case:

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
- `AndroidKeystoreDeviceKeyStore`, the real hardware-backed store, runs it in the instrumented test `AndroidKeystoreContractTest` on a phone or emulator.

Both pass. Because they do, every domain test that uses the in-memory store also tells us something true about the real one.

The server works the same way:
- `FakeTmcServer` (the app's fake) and `StubServer` (a small test double) both implement `AuthApi`, `EnrollmentApi` and `AttendanceApi`.
- The use cases run against either one unchanged.
- An HTTP client for the real tmc-central would be a third implementation.

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

`RenewCertificate` sees only `EnrollmentApi`. `OfflineFirstAttendanceRepository` sees only `AttendanceApi`.

The device key is split the same way:
- `DeviceKeyStore` creates, reads and deletes the key.
- `PayloadSigner` has one method, `sign`.

`GeneratePass` runs every 15 seconds and receives only a `PayloadSigner`, so pass generation cannot delete or replace the device key. Only `SignInAndEnroll`, `RenewCertificate` and `SignOut` get a `DeviceKeyStore`.

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

The build enforces this, not just a convention. `:core:domain` is a plain Kotlin module with no Android dependency, so a use case that imports Room, the Keystore or a Compose class does not compile. The domain's 34 unit tests run on a normal JVM, with no emulator and no Android framework.

Concrete classes meet only in the Hilt modules in `app/di`. The fake server is named in exactly one file:

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

- **The gate does not accept the app's codes yet.** The app signs with ECDSA P-256 in a `v2` format, because the Android Keystore supports P-256 on every phone the app targets (Android 8 and later), while Keystore support for the Ed25519 keys our gate module expects arrived only in recent Android versions. The format keeps the same structure and uses standard encodings, so a gate could accept `v2` by adding one validation rule. That rule does not exist yet.
- **`FakeTmcServer` implements three interfaces.** The real server would too. The split matters on the client side, where each use case receives only the one it needs.
- **`DeviceKeyStoreContract` is a plain class of check functions, not an abstract test class.** Domain tests use JUnit 6 and Android instrumented tests use JUnit 4, so a shared abstract test class would only work for one of them. Each test class calls the contract's functions itself.
- **`AndroidKeystoreDeviceKeyStore` and `AndroidKeystorePayloadSigner` must use the same key alias.** They are separate classes for Interface Segregation, but they share a key. Nothing but the shared default value keeps them in step.
- **Wiring the domain takes more code.** The domain does not depend on Hilt, so its classes carry no `@Inject` annotations. `DomainModule` builds every use case by hand. This is the cost of keeping the domain free of frameworks.
- **ViewModels catch specific exceptions.** A new failure type from a use case needs a new `catch` in the ViewModel that shows it. A sealed result type would make that impossible to forget, at the cost of more types.
