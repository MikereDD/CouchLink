# CouchLink Boot Service

Introduced as a foundation in `v0.1-dev.11` and promoted to active pre-login control in `v0.1-dev.12`, `CouchLink.BootService` is the machine-level half of the Windows architecture.

## Responsibilities

- Start automatically as LocalSystem before any user signs in.
- Publish an atomic health heartbeat under `%ProgramData%\CouchLink`.
- Determine whether Windows is locked, awaiting sign-in, or has a desktop available.
- Advertise the stable CouchLink host identity on UDP `45820`.
- Accept previously trusted clients on TCP `45822`.
- Enforce mirrored machine permissions.
- Submit approved pointer and keyboard reports through CouchLink Virtual HID.
- Signal Android when a signed-in desktop session becomes available.

## Explicit non-responsibilities

The service does **not**:

- pair new devices before login,
- store or validate Windows credentials,
- bypass Winlogon authentication,
- run launcher or desktop-only actions,
- expose unrestricted kernel input.

## Session Host relationship

The signed-in desktop path remains on TCP `45821` and owns pairing, launcher control, ordinary desktop input, tray behavior, and the WPF dashboard. `CouchLink.SessionHost` combines that runtime state with the Boot Service heartbeat for diagnostics.

## State and permission files

```text
%ProgramData%\CouchLink\boot-service-status.json
%ProgramData%\CouchLink\trusted-devices.json
%ProgramData%\CouchLink\machine-permissions.json
```

Writes are atomic where state consistency matters. Trusted identities and permission values originate from the signed-in host and are mirrored to the machine scope; the service does not invent trust.

## Current version

All managed Windows components report `0.1-dev.13`. The wire protocol remains version `1`.
