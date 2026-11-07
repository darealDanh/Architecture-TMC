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

## Protecting `main` on GitHub

After pushing the repository, make the CI check required so a pull request cannot be merged while the build or tests fail:

1. Settings → Branches → Add branch protection rule.
2. Branch name pattern: `main`.
3. Enable "Require a pull request before merging" and "Require status checks to pass before merging".
4. Select the `build` check and save.
