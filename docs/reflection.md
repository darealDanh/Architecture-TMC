# Team Reflection

In M1 we wrote the requirements, compared six architecture styles, and designed the logical and physical architecture of the TMC system. In M2 we reworked the design around two issues: connection loss in the middle of a transaction, and copied QR codes. For the final milestone we built one module, the Gate Station Software, in Java. This reflection covers what held up, what changed, and what building the code taught us about our own design.

> [Team to fill: one or two sentences on how the work was split between members and how we worked together, for example meetings, shared documents, who drew which diagrams.]

## Decisions that held up

**Validating at the gate (AD-04).** This is the decision we are most confident in. Before we compared any styles, Section 3 already said that every candidate would need gate stations that validate QR codes and store events locally. Looking back, that sentence carried more weight than the whole style comparison. NFR-01 (2 seconds) and NFR-06 (gates keep working offline) are met by keeping the scan-to-unlock path inside the factory, whatever runs in the data centre. In the code, the whole path from `GateStation.handleScan` to the turnstile call uses only the local SQLite store and the factory LAN, and the demo shows a gate admitting an employee while central is unreachable.

**Modular Monolith for the central system.** We picked it over Service-Based because the load is small: about 800 scans in 30 minutes per factory, which the physical architecture works out as roughly 2.2 events per second even at a five-fold burst. Building the gate made this choice look even safer, because the central system is not on the 2-second path at all. It receives events in batches, whenever the Sync Client gets through. A slower central system would delay reports, not gates.

**Write before unlock (AD-06) and unique event IDs (AD-07).** These two rules turned out to be enough for the "no loss, no duplicates" requirement. The event is written with status `PENDING` before the turnstile is called, so a kiosk that crashes mid-unlock finds the event on restart and marks it `UNLOCK_UNKNOWN`. Each event has a UUID from the gate, so when a connection drops before central's acknowledgement arrives, resending the batch is harmless. The test `connectionDropMidBatchLosesNothingAndDuplicatesNothing` cuts the connection after four of ten events and checks that central ends with exactly ten.

## What changed between M1 and M2

In M1 the QR code was generated on the server (the QR Display component) and refreshed every few seconds in the app or web portal. We treated the phone as a screen. M2 made us walk through a single transaction link by link, and the weakest link was not the WAN or the turnstile. It was an employee standing at the gate with no mobile signal, who would have no code to show. A screenshot of a server-generated code could also be sent to someone else and used within its validity window.

Moving code generation onto the phone fixed both problems. The server issues a device certificate once, and the phone signs a fresh code every 15 to 30 seconds without a network. The gate checks both signatures offline. This changed the module view: Device Enrollment replaced QR Display, and the Mobile App gained a QR Generator and a Key Store. The lesson for us was that drawing the phone in the context diagram as an "employee channel" hid the fact that it is part of the access path and needs the same offline thinking as the kiosk.

## Trade-offs we accepted

- **Stale data offline.** A gate knows revocations and employee status only up to its last sync. A revoked employee can still enter a gate that has been offline since the revocation.
- **Fail-secure turnstile.** If the turnstile does not answer, the gate does not open itself. The guard lets people through and records a manual entry. This is slower at shift change but keeps TMC out of controlling hardware it does not own (CO-1).
- **Phone handover.** If an employee gives their whole phone to someone else, the code is genuine and the gate cannot tell. Face capture would solve it but conflicts with the privacy requirement.
- **Same screenshot at a second gate.** We found this one while writing the design for the code. Used nonces are stored per gate, so a copied code could pass once at a different gate within about 35 seconds. We accepted it because the window is short and closing it would require gates to talk to each other or to central on the critical path.

## What building the code showed us

**Our dependency check missed the inside of a component.** The module view has a dependency matrix with every entry below the diagonal, so we believed the design had no cycles. That matrix covers modules, not the packages inside the Gate Station Software. When we built it, we created two package cycles without noticing. The first came from putting the storage interfaces next to their SQLite implementations. The second came from validation classes using types from the access flow, which itself uses validation. Both were fixed by moving each interface into the package that calls it, and both fixes were small. But neither cycle would have shown up in any diagram we drew.

**Vague sentences in the design became decisions in the code.** The M2 table says "retry with an idempotency key or query the turnstile status". A design can say "or"; code cannot. We chose retry, because querying status would add a second method to the turnstile interface. Similarly, the project description says TMC talks to the Turnstile Control System "through its API", but nothing describes that API. We had to invent a request format (`POST /gates/{id}/unlock` with an `Idempotency-Key` header) and mark it as an assumption. At design time, "through its API" felt like enough.

**Our own numbers do not add up.** The physical architecture gives the turnstile call a budget of 1.0 second within the 2-second limit. The code uses a 1-second turnstile timeout and two retries, both of which we chose. In the worst case, three attempts that each time out take about 3 seconds, which breaks NFR-01. We did not notice this while writing either the design or the plan, only when we put the two numbers side by side. With retries, the per-attempt timeout needs to be about 0.3 seconds, or the retries need to move out of the employee's path.

**We did not measure the local write.** AD-06 adds a disk write to every scan, and we configured SQLite to flush to disk on every commit, which is the safe setting. We never timed this on kiosk-class hardware, so the 0.1-second budget for the local write in Section 5.6 is still an estimate.

## What we would do differently

- Do the timing arithmetic with the actual configuration values, not just the design budget, before choosing defaults.
- Write down every external API we depend on as an explicit assumption in M1, with a guessed request and response, so the gap is visible early.
- Extend the dependency check below module level for any component we plan to build.

> [Team to fill: what we would change about how we worked as a team, for example starting the code earlier, splitting tasks differently, or reviewing each other's sections.]

> [Team to fill: the hardest part of the project for us, and anything that surprised us that is not covered above.]
