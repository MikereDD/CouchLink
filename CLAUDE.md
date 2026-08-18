# CouchLink — Claude Code

Read `CONVENTIONS.md` before any Git or GitHub operation.

## Project

CouchLink is a local-first Android remote for Windows gaming and Google TV control.

Stack: Kotlin, Jetpack Compose, Android SDK 28–36, C#, .NET 8, and WPF.

## Commands

| Action | Command |
| --- | --- |
| Android build | `cd src/android; .\gradlew.bat clean :app:assembleDebug` |
| Windows build/typecheck | `cd src/windows; dotnet build .\CouchLink.sln -c Release` |
| Windows run | `dotnet run --project .\src\windows\CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj -c Release` |
| Windows test | `cd src/windows; dotnet test .\CouchLink.sln -c Release` |
| Windows lint | `dotnet format src/windows/CouchLink.sln --verify-no-changes` |
| Preflight | `dotnet format src/windows/CouchLink.sln --verify-no-changes && cd src/android && .\gradlew.bat clean :app:assembleDebug && cd ../windows && dotnet test .\CouchLink.sln -c Release && dotnet build .\CouchLink.sln -c Release` |

## Architecture

Android keeps Bluetooth HID, Windows-host, and Google TV paths independent.

Windows separates WPF UI, host core, protocol contracts, release security, and updater projects.

## Conventions

- Keep Android feature packages and Windows project boundaries intact.
- Preserve Bluetooth fallback and local-only behavior.
- Preserve protocol-v1 compatibility across Android and Windows.
- Use Conventional Commits: `<type>(<scope>): <description>`.

## Never

- NEVER create or modify files outside the repository root unless specifically instructed.
- NEVER create a root-level feature folder.
- NEVER commit until lint, test, and build gates pass.
- NEVER push with uncommitted or untracked files.
- NEVER weaken pairing, token validation, update checks, or release signing.
- NEVER add cloud services, telemetry, advertising, or unnecessary permissions.
- NEVER make Bluetooth HID depend on host or TV availability.
- NEVER commit signing keys, passwords, tokens, generated binaries, or local configuration.

## Agent Rules

- Read `specs/` before writing code.
- Write plans and specifications under `specs/`.
- Bigpowers skills are optional; use them when they help feature work or bug fixes.
- If using bigpowers, use `plan-work` before feature code and `investigate-bug` before a bug fix.
- Run applicable verification after every change.
- Stop forward work on red Preflight or CI.
- For reproducible failures, use a focused fix; when using bigpowers, choose `quick-fix` or `fix-bug`.
- Keep code changes focused and minimal.
- ALWAYS use a feature branch or worktree for feature work.
