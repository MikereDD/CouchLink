# CouchLink Conventions

## Git Safety

- NEVER create or modify files outside the repository root unless specifically instructed.
- NEVER create a root-level feature folder.
- ALWAYS use a feature branch or worktree for feature work.
- NEVER commit until lint, test, and build gates pass.
- NEVER push with uncommitted or untracked files.

## Commit Messages

Use Conventional Commits.

Format: `<type>(<scope>): <description>`.

Use an imperative summary after the colon.

## Project Boundaries

- Keep Bluetooth HID, Windows-host, and Google TV paths independent.
- Keep Android code within its existing feature packages.
- Keep Windows code within its existing project boundaries.
- Preserve protocol-v1 compatibility across Android and Windows.
- Preserve local-only behavior and Bluetooth fallback.
- Update documentation and the changelog when behavior changes.

## Always Green / Shift Left

Run Preflight before forward work.

Run applicable verification after every change.

Preflight includes Windows lint, test, and Release build/typecheck gates plus the Android debug build. From the repository root, run each gate with fail-fast `&&` chaining; do not use semicolon chaining that can mask an earlier native-command failure:

`dotnet format src/windows/CouchLink.sln --verify-no-changes && cd src/android && .\gradlew.bat clean :app:assembleDebug && cd ../windows && dotnet test .\CouchLink.sln -c Release && dotnet build .\CouchLink.sln -c Release`

Windows lint: `dotnet format src/windows/CouchLink.sln --verify-no-changes`.

Windows test: `cd src/windows; dotnet test .\CouchLink.sln -c Release`.

Windows build/typecheck: `cd src/windows; dotnet build .\CouchLink.sln -c Release`.

Run `gh pr checks` before integration only when the user confirms CI has been configured.

Stop forward work when Preflight or CI fails.

## Discovered Defects

Treat every reproducible gate failure as a defect.

Use a focused fix. When using bigpowers, choose `quick-fix` for a trivial data-only fix or `fix-bug` when investigation or a regression test is required.

Write a BUG specification when reproduction is blocked.

Use a separate Conventional Commit for each discovered fix.

## Banned Dismissive Phrases

| Banned phrase | Required action |
| --- | --- |
| Pre-existing | Reproduce and use fix-or-log. |
| Unrelated to this session | Reproduce and use fix-or-log. |
| Not introduced by my changes | Reproduce and use fix-or-log. |
| Out of scope | Use quick-fix or fix-bug. |

## Planning Output

Write planning output under `specs/`.

Read `specs/state.yaml` before starting planned work.

Add a runnable `verify:` command to every implementation task.

Keep story status in `specs/execution-status.yaml`.

## Testing and Verification

Run the documented Android and Windows builds after relevant changes.

Follow `docs/building/TESTING.md` for device and integration checks.

Add a focused regression check for every bug fix.

## Defensive Code

Use explicit timeouts for network and update I/O.

Use bounded retries for transient local-network operations.

Use circuit breakers around repeated remote operation failures.

Keep Bluetooth HID usable when host or TV paths fail.

## Security and Privacy

NEVER weaken pairing, token validation, update verification, or release signing.

NEVER add cloud services, telemetry, advertising, or unnecessary permissions.

NEVER commit signing keys, passwords, pairing tokens, or local configuration.

## Agent Workflow

Bigpowers skills are optional; use them when they help feature work or bug fixes.

If using bigpowers, use `plan-work` before feature code, `investigate-bug` before a bug fix, and `verify-work` before declaring implementation complete.

Keep changes focused and minimal.
