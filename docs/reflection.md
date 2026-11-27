# Team Reflection

In M1 we wrote the requirements, compared six architecture styles, and designed the logical and physical architecture of the TMC system. In M2 we reworked the design around two issues: connection loss in the middle of a transaction, and copied QR codes. For the final milestone we first built the Gate Station Software in Java. It turned out too small to show much, so we dropped it and built the Mobile App in Kotlin, which is our code deliverable. The gate prototype is no longer in the repository, but some of what it taught us is below. This reflection covers what held up, what changed, and what building the code taught us about our own design.

We split the work by section rather than by milestone. One or two people owned each part (requirements, style comparison, logical and physical architecture, views), and we met once a week to read each other's sections aloud before merging them into the shared document. The diagrams were drawn in draw.io and kept in a shared Drive folder, which is why the links in the document point there.

## Decisions that held up

**Validating at the gate (AD-04).** This is the decision we are most confident in. Before we compared any styles, Section 3 already said that every candidate would need gate stations that validate QR codes and store events locally. Looking back, that sentence carried more weight than the whole style comparison. NFR-01 (2 seconds) and NFR-06 (gates keep working offline) are met by keeping the scan-to-unlock path inside the factory, whatever runs in the data centre. In our gate prototype, the whole path from the scan to the turnstile call used only the local SQLite store and the factory LAN, and it admitted an employee while central was unreachable.

**Modular Monolith for the central system.** We picked it over Service-Based because the load is small: about 800 scans in 30 minutes per factory, which the physical architecture works out as roughly 2.2 events per second even at a five-fold burst. Building the gate made this choice look even safer, because the central system is not on the 2-second path at all. It receives events in batches, whenever the Sync Client gets through. A slower central system would delay reports, not gates.

**Write before unlock (AD-06) and unique event IDs (AD-07).** These two rules turned out to be enough for the "no loss, no duplicates" requirement. The event is written with status `PENDING` before the turnstile is called, so a kiosk that crashes mid-unlock finds the event on restart and marks it `UNLOCK_UNKNOWN`. Each event has a UUID from the gate, so when a connection drops before central's acknowledgement arrives, resending the batch is harmless. A test in the gate prototype cut the connection after four of ten events and checked that central ended with exactly ten.

## What changed between M1 and M2

In M1 the QR code was generated on the server (the QR Display component) and refreshed every few seconds in the app or web portal. We treated the phone as a screen. M2 made us walk through a single transaction link by link, and the weakest link was not the WAN or the turnstile. It was an employee standing at the gate with no mobile signal, who would have no code to show. A screenshot of a server-generated code could also be sent to someone else and used within its validity window.

Moving code generation onto the phone fixed both problems. The server issues a device certificate once, and the phone signs a fresh code every 15 to 30 seconds without a network. The gate checks both signatures offline. This changed the module view: Device Enrollment replaced QR Display, and the Mobile App gained a QR Generator and a Key Store. The lesson for us was that drawing the phone in the context diagram as an "employee channel" hid the fact that it is part of the access path and needs the same offline thinking as the kiosk.

## Trade-offs we accepted

- **Stale data offline.** A gate knows revocations and employee status only up to its last sync. A revoked employee can still enter a gate that has been offline since the revocation.
- **Fail-secure turnstile.** If the turnstile does not answer, the gate does not open itself. The guard lets people through and records a manual entry. This is slower at shift change but keeps TMC out of controlling hardware it does not own (CO-1).
- **Phone handover.** If an employee gives their whole phone to someone else, the code is genuine and the gate cannot tell. Face capture would solve it but conflicts with the privacy requirement.
- **Same screenshot at a second gate.** We found this one while writing the design for the code. Used nonces are stored per gate, so a copied code could pass once at a different gate within about 35 seconds. We accepted it because the window is short and closing it would require gates to talk to each other or to central on the critical path.

## What building the code showed us

### The mobile app

**The platform chose our algorithm.** Our M2 design and the gate prototype both use Ed25519 signatures. When we built the app, we found that the Android Keystore, where the private key has to live so it never leaves the phone, supports P-256 on every phone we target but Ed25519 only on recent Android versions. We switched the phone to ECDSA P-256 and versioned the QR format as `v2`. The cost is that our gate prototype rejected the app's codes. Making it accept them would have meant touching the codec, two signature rules and the stored server key, because Ed25519 was assumed in all four. We designed the key handling on paper without checking what the device could actually store, and the hardware settled the question for us.

**Module boundaries changed a design decision for us.** We split the app into Gradle modules, so the domain cannot import anything from Android. Partway through, the app module tried to build the Room database and DataStore itself, and the build failed, because those libraries are private to the data module. The quick fix was to expose them. Instead we added a small `LocalStorage` class to the data module, which hands out only the domain interfaces. The boundary pushed us toward the better design. With packages inside one module, as in the gate prototype, nothing would have stopped us.

**Fakes tested our logic, but only a real device tested our assumptions.** All 78 unit tests passed, and the first thing the app did on the emulator was report "Can't reach the server" while the emulator was online. The cause was a missing `INTERNET` permission. The fake server runs inside the app, so we never thought the app needed network access, but Android hides the network from apps without it. A second bug appeared when we turned on airplane mode: the "Online" label sometimes stayed on, because our code asked Android for the current network inside the very callback that reports it is gone. Both bugs were in the one class whose only job is talking to Android, which is exactly the class the fakes replace. We added instrumented tests that toggle airplane mode on the emulator to cover it.

**"The key is in secure hardware" depends on the phone.** The design says the private key never leaves secure hardware. On the emulator, the Device screen shows "Hardware-backed: No", because the emulator's keystore is software only. We show this honestly instead of hiding it, and it reminded us that some security properties in the design are properties of the device, not of our code.

**The QR code got bigger than we pictured.** A `v2` pass carries a certificate, a public key and two signatures, which comes to over 400 characters, so the QR code is dense. We had to enlarge the countdown ring so the code fits inside it. Nobody had asked how big the code would be when we designed it in M2.

### The gate prototype

**Our dependency check missed the inside of a component.** The module view has a dependency matrix with every entry below the diagonal, so we believed the design had no cycles. That matrix covers modules, not the packages inside the Gate Station Software. When we built it, we created two package cycles without noticing. The first came from putting the storage interfaces next to their SQLite implementations. The second came from validation classes using types from the access flow, which itself uses validation. Both were fixed by moving each interface into the package that calls it, and both fixes were small. But neither cycle would have shown up in any diagram we drew.

**Vague sentences in the design became decisions in the code.** The M2 table says "retry with an idempotency key or query the turnstile status". A design can say "or"; code cannot. We chose retry, because querying status would add a second method to the turnstile interface. Similarly, the project description says TMC talks to the Turnstile Control System "through its API", but nothing describes that API. We had to invent a request format (`POST /gates/{id}/unlock` with an `Idempotency-Key` header) and mark it as an assumption. At design time, "through its API" felt like enough.

**Our own numbers do not add up.** The physical architecture gives the turnstile call a budget of 1.0 second within the 2-second limit. The prototype used a 1-second turnstile timeout and two retries, both of which we chose. In the worst case, three attempts that each time out take about 3 seconds, which breaks NFR-01. We did not notice this while writing either the design or the plan, only when we put the two numbers side by side. We lowered the per-attempt timeout to 0.3 seconds, so three attempts fit within the 1.0-second turnstile budget, and added a test that failed if the timeout and the retry count are ever changed in a way that breaks it again. Whether 0.3 seconds is enough for the real Turnstile Control System on a factory LAN is something we could only find out on site.

**We did not measure the local write.** AD-06 adds a disk write to every scan, and we configured SQLite to flush to disk on every commit, which is the safe setting. We never timed this on kiosk-class hardware, so the 0.1-second budget for the local write in Section 5.6 is still an estimate.

## What we would do differently

- Do the timing arithmetic with the actual configuration values, not just the design budget, before choosing defaults.
- Write down every external API we depend on as an explicit assumption in M1, with a guessed request and response, so the gap is visible early.
- Extend the dependency check below module level for any component we plan to build.
- Check what the target platform supports, such as key types and permissions, before fixing a design decision that depends on it.
- Run the code on a real device or emulator from the first week, not only in unit tests.

- Start a small piece of code during M1 instead of at the end. Most of what we learned in the last milestone would have been cheaper to learn while the design was still changing.
- Split the work by flow instead of by section. Because each of us owned a section, the access flow was described in four places (requirements, logical architecture, physical architecture, C&C view) by different people, and the descriptions drifted. One person owning the access flow end to end would have kept it consistent.

## The hardest part

The style comparison in Section 3 took the longest and produced the most argument. Two of us preferred Service-Based, because independent scaling of the Access Control Service looked like the obvious answer to the shift-change peak. What settled it was doing the arithmetic. Once we worked out that the peak is a few events per second, the argument for distributed services fell apart, and we chose the Modular Monolith. In hindsight we spent too long comparing styles in general before checking the one number that decided it.

What surprised us most was how much of the design was decided by the edge, not the centre. We expected the central architecture to be the main topic of the project. In the end, the requirements that were hardest to meet (2 seconds, offline operation, no lost records) were all solved at the gate, and the central style mattered mainly for maintainability.
