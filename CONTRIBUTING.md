# Contributing

## Branches

Start every change from `main` on its own branch. Use a prefix that names the area: `ci/`, `docs/`, `chore/`, `feat/`, or `fix/`.

## Checks

Run the same command as CI from `mobile-app/`:

```
./gradlew assembleDebug test
```

Use `gradlew.bat` on Windows. CI (`.github/workflows/ci.yml`) runs the command on every pull request and on every push to `main`.

## Pull requests

Open the pull request against `main`. Keep it to one change. Explain why in the commit body, not only what changed.

Do not add a dependency without a clear need.

## Merging

Merge only after the `android` check passes. Use a merge commit. Delete the branch after merge when it is no longer needed.
