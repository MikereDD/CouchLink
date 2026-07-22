# CouchLink v0.1-dev.11.2.3 — Pre-Login Permission Sync Test

1. Uninstall the prior Boot Service before building.
2. Build Windows and Android.
3. Launch the desktop host once.
4. Enable **Remote Input** in the host.
5. Confirm `C:\ProgramData\CouchLink\machine-permissions.json` reports both permission fields as `true`.
6. Install the Boot Service and confirm its heartbeat reports `PreLoginControlEnabled: true`.
7. Reboot and remain at the Windows sign-in screen.
8. Open Android CouchLink. It should connect and report input authorization instead of input locked.
9. Secure Windows password/PIN entry is not implemented in this milestone; input commands must be rejected safely.
10. Sign in normally and verify automatic handoff to the Session Host and Steam Big Picture behavior.
