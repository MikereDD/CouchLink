# Architecture

## Windows

### CouchLink.Protocol

Shared wire-contract types. This project must remain free of Windows UI and input-injection dependencies.

### CouchLink.Host.Core

Long-running host behavior:

- discovery
- authenticated sessions
- launcher detection
- command routing
- clipboard bridge
- media and system actions
- diagnostics

### CouchLink.Host.Wpf

Premium Windows dashboard and tray application. It configures and observes the core host but does not own protocol logic.

## Android

The Android application will use Kotlin and Jetpack Compose. Its first screens will be:

1. Host discovery
2. Pairing
3. Connection status
4. Development touch surface

## Transport plan

### Discovery

UDP broadcast/multicast advertises a minimal host descriptor on the local network.

### Session transport

The first prototype uses a framed TCP stream to validate ordering, acknowledgements, reconnect behavior, and diagnostics. The protocol layer must allow a later low-latency transport without rewriting the UI.

### Security

The first public-capable build must use encrypted authenticated sessions and persistent per-device identity. Unauthenticated input injection is prohibited.
