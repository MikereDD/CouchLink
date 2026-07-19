# CouchLink v0.1-dev.11.2.4 — Android Reconnect & Pre-Login Pointer Recovery Test

1. Uninstall the previous Boot Service before building.
2. Build Windows and Android, run the desktop host once, enable Remote Input, then install the Boot Service.
3. Confirm TCP 45822 is listening and `PreLoginControlEnabled` is true.
4. Reboot and remain at the Windows sign-in screen.
5. Connect Android, open Touchpad, and click the PIN field.
6. Open Keyboard, enter PIN digits, then use Send Text. Digits should be injected as physical virtual-key presses.
7. Press Enter using the keyboard special-key control if submission is not automatic.

Security notes:
- The Boot Service accepts digits only on the pre-login text path.
- PIN text is not logged or persisted.
- This is an experimental compatibility path, not a Windows Credential Provider.
