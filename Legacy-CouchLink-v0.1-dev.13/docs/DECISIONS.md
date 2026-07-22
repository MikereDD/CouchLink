# CouchLink Decision Log

## D-001 — Product structure

CouchLink is a two-part product:

1. CouchLink Host for Windows
2. CouchLink Remote for Android

## D-002 — Product identity

CouchLink follows the Typezer∅ premium quality standard but is not named as a Studio Suite application.

## D-003 — Primary launcher

Steam is the primary/default launcher and receives the most prominent placement. Supported secondary launchers are GOG Galaxy, EA app, Ubisoft Connect, Rockstar Games Launcher, Amazon Games, and Epic Games Launcher.

## D-004 — Local-first operation

Normal operation must not require an internet connection, cloud account, subscription, or external relay.

## D-005 — Development hardware

Netzach is the initial Windows development and test host. The custom Steam PC becomes the reference living-room target when assembled.

## D-006 — First engineering order

Connection discovery, framing, identity, and pairing boundaries come before mouse and keyboard injection.

## D-007 — Pre-login pointer control requires a virtual HID driver

`SendInput` from `CouchLink.BootService` cannot deliver input to the interactive secure desktop. True lock-screen pointer/touchpad support requires a signed virtual HID driver, or the feature must be replaced by a Credential Provider for sign-in-only workflows. Pre-login remains status, wake, and reconnect only until signed driver work lands.
