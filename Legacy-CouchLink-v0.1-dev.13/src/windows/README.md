# CouchLink for Windows

<p align="center">
  <img src="CouchLink.Host.Wpf/Assets/couchlink.png" alt="CouchLink Host" width="160">
</p>

<p align="center">
  <strong>Windows host, session coordinator, boot service, and Virtual HID stack</strong><br>
  Version <code>0.1-dev.13</code> · Protocol <code>1</code> · .NET <code>8</code>
</p>

---

## Projects

| Project | Responsibility |
|---|---|
| `CouchLink.Host.Wpf` | Premium dashboard, tray integration, preferences, About, and diagnostics |
| `CouchLink.Host.Core` | Discovery, trusted sessions, remote input routing, launcher control, and host state |
| `CouchLink.SessionHost` | User-session lifecycle and combined Boot Service/desktop health |
| `CouchLink.BootService` | LocalSystem startup, pre-login broker, machine permissions, and Virtual HID bridge |
| `CouchLink.Protocol` | Shared framed protocol envelopes and message contracts |
| `CouchLink.VirtualHid` | KMDF/VHF virtual mouse and boot-keyboard driver for secure desktop input |

## Build managed components

From the repository root:

```powershell
dotnet build .\src\windows\CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj
```

Build the Boot Service directly when needed:

```powershell
dotnet build .\src\windows\CouchLink.BootService\CouchLink.BootService.csproj
```

## Virtual HID

The driver is not built by `dotnet build`. It requires Visual Studio with the Windows Driver Kit, test signing during development, and a deliberate install procedure. Read:

- [Virtual HID README](CouchLink.VirtualHid/README.md)
- [Build, Sign, and Test](CouchLink.VirtualHid/BUILD-SIGN-TEST.md)
- [Virtual HID Foundation](../../docs/VIRTUAL-HID-FOUNDATION.md)

## Runtime endpoints

| Endpoint | Purpose |
|---|---|
| UDP `45820` | Host discovery |
| TCP `45821` | Signed-in desktop session |
| TCP `45822` | Boot Service and pre-login session |

## Security boundary

CouchLink does not bypass Windows authentication. The Boot Service accepts only previously trusted devices, obeys machine-level permission gates, and submits fixed-size reports through the Virtual HID device. Pairing and credential storage remain desktop-only operations.
