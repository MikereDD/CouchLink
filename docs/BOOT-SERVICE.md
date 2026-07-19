# CouchLink Boot Service Foundation

CouchLink v0.1-dev.11 introduces an explicit boundary between two Windows execution contexts.

## CouchLink Boot Service

`CouchLink.BootService` is an automatically started Windows service. In this milestone it has deliberately narrow responsibilities:

- start independently of a signed-in user
- publish a small health heartbeat under `%ProgramData%\CouchLink`
- report whether an interactive Windows desktop appears to be available
- provide the future home for boot-state, lock-state, and pre-login coordination

It does not accept remote input, store Windows credentials, or bypass Windows authentication.

## CouchLink Session Host

`CouchLink.SessionHost` owns the current user-session runtime boundary. It wraps the existing networking and input runtime and exposes combined Session Host and Boot Service health to the WPF dashboard.

The desktop-session side remains responsible for:

- discovery and trusted sessions
- pairing
- mouse and keyboard input
- launcher control
- tray integration
- premium dashboard UI

## Current communication model

The Boot Service writes an atomic JSON heartbeat file. The Session Host reads it every few seconds. This simple boundary is intentional for the foundation milestone. A later version can replace it with authenticated named-pipe IPC when commands must flow between the service and the signed-in session.

## Security boundary

The Boot Service is not a credential provider and is not a virtual input driver. Pre-login control remains a separate gated research project.
