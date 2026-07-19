## v0.1-dev.12.1 — Virtual HID Safety Review

- Restricted the virtual HID device object to LocalSystem and administrators with `SDDL_DEVOBJ_SYS_ALL_ADM_ALL`.
- Serialized Boot Service HID submissions so reports remain ordered and the device handle cannot be disposed during `DeviceIoControl`.
- Confirmed the driver intentionally uses VHF's default buffering policy; without `EvtVhfReadyForNextReadReport`, the transfer buffer may be reused after `VhfReadReportSubmit` returns.
- Synchronized Windows, Boot Service, Android, UI, and protocol client metadata to `0.1-dev.12.1`; Android `versionCode` is 28.
- Added a focused VM-first validation checklist. No compiled driver or signing material is included.

## v0.1-dev.12 — Virtual HID Pre-Login Input

- Implements the KMDF + Microsoft VHF **Virtual HID source driver** (`Driver.c`)
  with a relative mouse (report ID 1) and boot-compatible keyboard (report ID 2).
- Locks the user↔kernel interface to a single IOCTL and a fixed 12-byte,
  version-checked `COUCHLINK_HID_SUBMIT` struct in `CouchLinkHidProtocol.h`.
- Adds `VirtualHidBridge` in the Boot Service: finds and opens the driver, and
  turns mouse/keyboard/scroll/text/key/shortcut messages into HID reports,
  including an ASCII→HID keymap for typing PINs and passwords.
- Wires the pre-login broker to inject input when the driver is present **and**
  `machine-permissions.json` authorizes it; otherwise returns
  `prelogin_input_unavailable`. Advertises `prelogin_input, mouse, keyboard`.
- Releases held buttons/keys on client disconnect and service stop.
- Adds `CouchLink.VirtualHid.vcxproj` to the solution (x64/ARM64) and a full
  `BUILD-SIGN-TEST.md` VM-first build/sign/validate guide.
- No compiled/signed `.sys` ships; the driver is built and signed by the user in
  a disposable VM. Android client needs no changes — its existing pre-login
  touchpad/keyboard light up once the host advertises input.

## v0.1-dev.11.3.1 — Status-Only Pre-Login Baseline

- Removed the unsupported Session 0 pre-login input controller.
- Boot Service now advertises status-only capabilities before login.
- Pre-login input commands return `prelogin_status_only`.
- Adopted the canonical Virtual HID / Credential Provider boundary in project docs.
- Preserved trusted discovery, machine state, wake, and desktop handoff.
- Added a VM-first test checklist and retained the WDK prerequisite checker.

## v0.1-dev.11.3 — Virtual HID Research Foundation

- Retires the unsupported Session 0 user32 lock-screen input approach.
- Adds a VM-first Virtual HID architecture and security boundary.
- Adds a versioned mouse-report protocol header.
- Adds Visual Studio/WDK prerequisite diagnostics.
- Intentionally ships no kernel binary, INF, catalog, or installation action.

## v0.1-dev.11.2.6.2 — Secure-Screen Pointer Fallback

- Uses absolute cursor positioning for pre-login movement before falling back to legacy `mouse_event` and `SendInput`.
- Preserves trusted pre-login clicks, scrolling, reconnect recovery, and the once-per-session Steam Big Picture trigger.
- Adds a focused login-screen pointer validation checklist.

## v0.1-dev.11.2.6.2 — Pointer Controller Compile Hotfix

- Fixed C# tuple type inference in `PreLoginInputController.MouseButton`.
- Uses explicit unsigned fallback tuple values so the Boot Service compiles cleanly.
- No behavioral changes beyond the compile repair.

## v0.1-dev.11.2.6.2 — Android Reconnect & Pre-Login Pointer Recovery

- Added endpoint fallback across the remembered port, desktop Session Host (`45821`), and Boot Service broker (`45822`).
- Prevented Android from becoming permanently idle after a failed or stale session.
- Added endpoint-specific reconnect status so the active target is visible during testing.
- Added trusted pre-login mouse movement, button, and scroll commands to the Boot Service broker.
- Exposed the real Touchpad screen during pre-login sessions for operating Windows Accessibility and the On-Screen Keyboard.
- Preserved fail-safe socket invalidation and once-per-session Steam Big Picture behavior.

## v0.1-dev.11.2.6.2 — Android Reconnect & Pre-Login Pointer Recovery

- Prevented Android `SocketException: Broken pipe` failures from escaping network-send coroutines and crashing the Compose UI.
- Invalidates and closes stale pre-login sockets safely so the persistent-session loop can reconnect to the Boot Service or desktop Session Host.
- Keeps touchpad movement and mouse-button sends fail-safe during the pre-login-to-desktop handoff.
- Limits automatic Steam Big Picture launch to the first trusted Android connection in the current Windows host session; later app resumes and reconnects no longer relaunch it.
- Preserves manual Steam Big Picture launch behavior.

## v0.1-dev.11.2.4 — Android Reconnect & Pre-Login Pointer Recovery

- Added a restricted Boot Service keyboard path that converts pre-login PIN digits into physical virtual-key events rather than Unicode text events.
- Added pre-login Enter, Backspace, Escape, Tab, Delete, and arrow-key handling.
- PIN data is neither logged nor persisted; non-digit pre-login text is discarded.
- Advertises the `prelogin_pin_keys` capability to trusted Android clients.

## v0.1-dev.11.2.3 — Pre-Login Permission Sync

- Persisted Remote Input authorization in `C:\ProgramData\CouchLink\machine-permissions.json`.
- Synchronized desktop-host permission changes for the Boot Service.
- Pre-login sessions now report trusted authorization separately from secure-desktop input support.
- Added permission state to Boot Service heartbeat diagnostics.
- Preserved safe rejection of secure-desktop input until the Credential Provider milestone.

# CouchLink Changelog

## v0.1-dev.11.2.3 — Pre-Login Permission Sync

- Kept TCP 45822 listening for the full Boot Service lifetime instead of only while Explorer was absent.
- Added broker endpoint, client count, state, and last-error diagnostics to the machine heartbeat.
- Added automatic Windows Firewall repair for inbound TCP 45822 on private networks.
- Closed pre-login status sessions when the desktop becomes available so Android reconnects to the Session Host.
- Preserved status-only pre-login security; secure-desktop input remains disabled.

## v0.1-dev.11.2.1 — Pre-Login Broker Compile Hotfix

- Fixed invalid C# `using` statement scope in `PreLoginBroker.HandleClientAsync`.
- Restored the network stream scope for pre-login authentication, heartbeat, and status handling.
- No protocol or feature behavior changed from v0.1-dev.11.2.

## v0.1-dev.11.2 — Pre-Login Connectivity Foundation

- Added Boot Service discovery before Windows login over UDP 45820.
- Added a trusted, status-only pre-login session broker on TCP 45822.
- Mirrored paired-device tokens into the machine CouchLink data directory.
- Added automatic Android handoff from the pre-login broker to the desktop Session Host after login.
- Added a dedicated Android “Connected before login” state.
- Kept secure-desktop input and credential submission disabled by design.

## v0.1-dev.11.1 — Startup Registration Hotfix

- Fixed Start with Windows when CouchLink preferences were saved from `dotnet run`.
- Startup registration now targets the generated `CouchLink.Host.exe` apphost instead of `dotnet.exe`.
- Added a safe framework-dependent fallback for build layouts without an apphost.
- CouchLink repairs stale or moved startup registrations automatically whenever the host launches.
- Preserved headless tray startup, Boot Service heartbeat, Android compatibility, and Big Picture launch behavior.

## v0.1-dev.11 — Boot Service Foundation

- Added `CouchLink.BootService`, an automatically started Windows service foundation.
- Added `CouchLink.SessionHost`, separating desktop-session orchestration from host core networking and input.
- Added Boot Service heartbeat and machine-state reporting under `%ProgramData%\CouchLink`.
- Added dashboard health reporting for Boot Service and Session Host independently.
- Added development install, uninstall, and status PowerShell tools.
- Preserved Android compatibility, tray startup, trusted pairing, launchers, mouse, keyboard, and Steam Big Picture behavior.
- Pre-login input is intentionally not included in this milestone.

## v0.1-dev.10.1 — Headless Tray Startup

- Prevented the WPF dashboard from being constructed during minimized startup, eliminating the black ghost window.
- Added lazy dashboard creation from the tray icon.
- Preserved background discovery, trusted reconnect, and Steam Big Picture launch without a visible window.
- Refined Android bottom navigation with icons and non-wrapping single-line labels.

# CouchLink Changelog

## v0.1-dev.10 — Living-Room Readiness

- Added Wake-on-LAN adapter advertisement and Android wake control.
- Added an Android Settings screen with persistent haptics and natural-scrolling preferences.
- Added Windows startup controls for launch at sign-in, minimized startup, and automatic Steam Big Picture launch after a trusted connection.
- Preserved trusted pairing, premium launcher identity, tray operation, trusted-device management, mouse, and keyboard behavior.

## v0.1-dev.9.7 — Authentic Launcher Icons (Android-only)

- Replaced every launcher placeholder with a recognizable official brand glyph.
- Added normalized Steam, GOG, Xbox, EA, Ubisoft, Rockstar, Epic, and Amazon Games icon assets.
- Preserved the approved Steam/GOG featured layout and 3 × 2 launcher grid.
- Added consistent graphite framing, optical sizing, padding, and brand accents.
- Updated Android version metadata to `0.1-dev.9.7` (`versionCode` 16).
- Compatible with the existing v0.1-dev.9.6 Windows host; no Windows rebuild is required.

## v0.1-dev.9.6 — Xbox Launcher Layout

- Added Xbox as a first-class supported launcher.
- Added a premium Xbox launcher icon and launch action.
- Changed the Android launcher hierarchy to: Steam full width, GOG Galaxy full width, then Xbox/EA/Ubisoft and Rockstar/Epic/Amazon.
- Added Windows host handling for launching the Xbox app.
- Preserved all pairing, trusted-device, tray, mouse, keyboard, and reconnect behavior.

# CouchLink Changelog

## v0.1-dev.9.4 — Re-Pair & Exit Repair

- Fresh Android installations now actively request a new pairing code.
- Reinstalling or clearing Android app data no longer leaves the client stuck at discovery.
- The Windows close button now exits CouchLink instead of silently hiding it.
- Tray Exit uses an explicit graceful shutdown path.
- Minimize continues to hide CouchLink to the tray while keeping the host active.

# Changelog


## v0.1-dev.9.3 — Startup & Tray Compile Repair

- Added missing `System.IO` import for host preferences.
- Fully qualified WPF `Application` and `MessageBox` references when Windows Forms tray support is enabled.
- Preserved startup, tray, networking, launcher, mouse, and keyboard behavior.

## v0.1-dev.9.2 — Startup & Tray Compile Hotfix

- Resolves the WPF/Windows Forms `Application` type ambiguity introduced by tray support.
- Fully qualifies the WPF application base class as `System.Windows.Application`.
- Keeps the version badge XML-safe.
- No runtime behavior changes.

## v0.1-dev.9 — Startup & Tray

- Added system-tray operation, close-to-tray, tray controls, and startup-ready command-line behavior.
- Preserved all premium launcher and remote-input features.

## v0.1-dev.8.1.1 — Build Hotfix

- Fixed invalid XAML caused by an unescaped ampersand in the Windows version badge.
- Marked CouchLink.Host.Core as Windows-targeted to remove CA1416 registry API warnings.
- Updated Windows and Android metadata to v0.1-dev.8.1.1.

## v0.1-dev.8.1 — Layout & Version Correction

- Added extra bottom clearance to Android scrollable screens so Command Deck and Keyboard controls can scroll fully above the fixed navigation bar.
- Corrected Windows host display metadata to v0.1-dev.8.1.
- Updated Android package metadata to v0.1-dev.8.1.
- No launcher, networking, pairing, mouse, or keyboard behavior changed.

## v0.1-dev.8 — Launcher Identity & Control Polish

- Replaced temporary letter tiles with seven crisp, normalized premium launcher identity assets.
- Added tactile layered launcher tiles with controlled depth, borders, highlights, and shadowing.
- Upgraded the Steam primary tile with a large high-detail icon treatment and stronger visual hierarchy.
- Rebuilt the Command Deck with dedicated iconography and tactile premium controls.
- Preserved launcher actions, Steam Big Picture, remote input, pairing, trusted sessions, and reconnect behavior.
- Advanced Android and Windows host version metadata to v0.1-dev.8.

## v0.1-dev.7.3.1 — Source Metadata Refresh

- Updated the Android project README to describe the current premium control client rather than the obsolete v0.1-dev.2 discovery prototype.
- Updated the repository README current-milestone description to match the working remote-input and launcher feature set.
- Renamed the Android Studio project display name from `CouchLinkRemote` to `CouchLink Remote`.
- Advanced Android version metadata to `0.1-dev.7.3.1` / version code 13.
- No runtime control, networking, launcher, or input behavior changed.

## v0.1-dev.7.3 — Canonical Logo Scale

- Replaced the undersized generated CL mark with the user-approved cropped mockup logo as the canonical visual reference.
- Increased the Android app-header logo from 48 dp to 64 dp.
- Rebuilt Android adaptive and legacy launcher assets with substantially less empty padding for better installer, update-card, launcher, and recent-app visibility.
- Increased the Windows custom-title-bar mark from 26 px to 38 px and expanded the title bar to 50 px.
- Rebuilt the Windows executable icon with larger CL occupancy for taskbar and Alt+Tab readability.
- Preserved all v0.1-dev.7.2 functionality.

## v0.1-dev.7.2 — Windows Identity Polish

- Replaced the standard white Windows title bar with a custom graphite CouchLink title bar.
- Added the approved metallic CL identity to the Windows window, executable, taskbar, and Alt+Tab.
- Replaced the Android placeholder CL tile with the refined approved CouchLink monogram.
- Preserved normal drag, double-click maximize, minimize, maximize/restore, close, and resize behavior.

# Changelog

## 0.1-dev.7.1 — Mobile Layout Polish

- Added Android status-bar inset handling so CouchLink no longer overlaps the system clock, network, or battery indicators.
- Replaced the large persistent session card with a compact expandable connection strip.
- Reduced permanent vertical UI overhead and increased usable Touchpad space.
- Improved Steam, launcher, Command Deck, and disabled-button contrast.
- Preserved all v0.1-dev.7 launcher, input, session, and reconnect behavior.

## 0.1-dev.7 — Premium Control Surface

- Introduced the first premium Android navigation structure: Home, Touchpad, and Keyboard.
- Added the approved graphite, orange, and CL-monogram visual direction.
- Added Steam-first launcher control and quick keys for GOG, EA, Ubisoft, Rockstar, Amazon Games, and Epic.
- Added Command Deck shortcuts for Alt+Tab, Task Manager, Show Desktop, and Close Window.
- Added launcher discovery from common installation paths and the Windows uninstall registry.
- Preserved trusted sessions, remote-input locking, pointer control, scrolling, drag lock, and keyboard input.

## 0.1-dev.6 — Input Refinement

- Added synchronized Windows remote-input lock status to Android heartbeats.
- Added adjustable pointer sensitivity and light speed-based acceleration.
- Added two-finger vertical scrolling.
- Added drag lock with safe release on disconnect or host lock.
- Added Unicode text input and first special-key controls.
- Added Escape, Tab, Backspace, Enter, and arrow keys.
- Expanded remote input from mouse-only to mouse and keyboard.

## v0.1-dev.5 — First Input

- Added authenticated relative pointer movement.
- Added left-click, right-click, and vertical scroll commands.
- Added a local Windows safety lock for remote input; it defaults to disabled after every host launch.
- Added bounded mouse deltas and scroll values.
- Added emergency mouse-button release on disconnect, timeout, shutdown, and input disable.
- Added the first Android touchpad surface and dedicated mouse controls.
- Kept keyboard input disabled for the next milestone.

## v0.1-dev.3 — Pairing

- Added six-digit approval-code pairing.
- Added persistent trusted-device tokens on Windows and Android.
- Added trusted hello verification after pairing.
- Added pairing code and trusted-device count to the Windows dashboard.
- Preserved UDP source-address selection for VPN and virtual-adapter safety.
- Remote input remains disabled for this milestone.


## v0.1-dev.2.3 — Reachable-address hotfix

- Fixed Android discovery selecting an unreachable VPN or virtual-adapter address advertised by the Windows host.
- Android now connects to the source address of the UDP discovery packet, which is the Windows interface that actually reached the phone.
- Retains the advertised address only as a fallback.
- Updated Android version metadata to `0.1-dev.2.3` (`versionCode` 4).
- No input or pairing behavior changed.

## v0.1-dev.2.2 — Android JVM target hotfix

- Aligned Java and Kotlin Android compilation on JVM 17.
- Added Android `compileOptions` for Java 17.
- Added Kotlin JVM toolchain 17 configuration.
- Updated Android version metadata to `0.1-dev.2.2` (`versionCode` 3).
- No protocol, discovery, or Windows-host behavior changed.

## v0.1-dev.2.1 — WPF startup hotfix

- Fixed a startup crash caused by the `HostId` binding on `Run.Text` defaulting to TwoWay against a read-only view-model property.
- Explicitly sets the binding to `Mode=OneWay`.
- No protocol, discovery, or networking behavior changed.

## 0.1-dev.2 — Discovery

- Added live UDP discovery advertisements on port 45820.
- Added subnet-directed and global broadcast targets.
- Added the framed TCP session listener on port 45821.
- Added `hello` / `hello_ack` and `ping` / `pong` handling.
- Added host identity, state, endpoint, connection count, and live event reporting.
- Updated the WPF dashboard to the approved graphite and ember-orange visual direction.
- Added a PowerShell discovery and hello-exchange test utility.
- Added the first Android Compose discovery client source.
- Kept all remote input disabled until pairing and authentication are implemented.

## 0.1-dev.1 — First Link

- Created the CouchLink repository scaffold.
- Added Windows protocol, host-core, and WPF dashboard project boundaries.
- Added the initial framed JSON protocol contract.
- Added architecture, roadmap, and decision records.
- Established Netzach as the initial development host.

## 0.1-dev.4 — Persistent Session

- Keeps authenticated Android sessions open after trusted hello.
- Adds `session_ready` capability negotiation.
- Adds five-second ping/pong heartbeat monitoring.
- Times out silent clients after twenty seconds.
- Adds Android automatic reconnect after network interruption.
- Shows the active device and live client count in the Windows dashboard.
- Preserves safe separation from remote-input injection.

## v0.1-dev.9.5.1 — Trusted Device Management

- Added a visible pairing-code panel to the Windows host.
- Added Pair New Device mode and pending-pairing reset.
- Added trusted-device listing with paired timestamps.
- Added Revoke Selected and Revoke All controls with confirmation.
- Revoked devices are disconnected and must pair again.
- Added safe handling for invalid or stale pairing tokens.

## v0.1-dev.9.5.1 — Pairing UI Build Hotfix

- Removed unsupported WPF `CharacterSpacing` from the pairing-code text.
- No pairing, trust-management, networking, Android, or tray behavior changed.
