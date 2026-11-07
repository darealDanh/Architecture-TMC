# TMC Access Control and Attendance

Software architecture class project. `BTL ARCHITECTURE.txt` is the source of truth for requirements (FR-*, NFR-*), architecture decisions (AD-01 to AD-13), design principles (Section 6.2) and the M2 issues. Code and documents must stay consistent with it.

## Scope

- `gate-station/`: the Gate Station Software module (QR Validator, Local Store, Turnstile Adapter, Sync Client), Java 17, Maven, JUnit 5, SQLite.
- `docs/`: the SOLID write-up and the reflection.

## Engineering standards

- Write code as a senior engineer would ship it: small classes, one responsibility each, intention-revealing names, no dead code, no speculative abstractions.
- No comments in code. Names and structure carry the meaning. No Javadoc, no inline comments, no commented-out code.
- Follow SOLID as described in Section 6.2. Depend on interfaces such as `TurnstileGateway` and `EventIntake`, not concrete classes. New rules and adapters are new classes, not edits to existing ones.
- Every behaviour is covered by tests. `mvn verify` must pass before any commit.
- Do not add dependencies without a clear need.

## Writing standards

- Documents are plain, direct English. No filler, no marketing tone, no AI-style phrasing (no "delve", "robust", "seamless", "it's worth noting", no forced triads, no summary closers).
- Every SOLID claim points to a real class in the repo and to the requirement or decision it serves.

## Git

- Never add Claude or any AI as an author or co-author. No `Co-Authored-By` trailers, no "Generated with" lines in commits or pull requests.
- Commit messages: short imperative subject line, optional body explaining why.
- Work on branches and merge to `main` through pull requests. CI must pass before merging.

## CI

- GitHub Actions runs `mvn -B verify` in `gate-station/` on every pull request and every push to `main`.
