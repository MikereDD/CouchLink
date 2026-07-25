# CouchLink Windows Launcher Host

<p align="center">
  <img src="CouchLink.Host.Wpf/Assets/couchlink.png" alt="CouchLink Host" width="150">
</p>

<p align="center">
  <strong>Optional local-first launcher control for CouchLink Android</strong><br>
  Version <code>1.1</code> · Protocol <code>1</code> · .NET <code>8</code>
</p>

## Responsibilities

The host discovers and pairs trusted Android remotes, launches or focuses installed game launchers, closes them when requested, and reports live state and results back to Android. It also provides tray behavior, startup preferences, diagnostics, and the desktop dashboard.

Keyboard, mouse, touchpad, shortcuts, and Windows sign-in input do **not** pass through this host. Those remain on Android's independent Bluetooth HID route.

## Projects

| Project | Responsibility |
|---|---|
| `CouchLink.Host.Wpf` | Dashboard, tray, preferences, About, and diagnostics |
| `CouchLink.Host.Core` | Discovery, trusted pairing, launcher control, and host state |
| `CouchLink.Protocol` | Framed protocol envelopes and message contracts |

## Build and run

```powershell
dotnet restore .\CouchLink.sln
dotnet build .\CouchLink.sln -c Release
dotnet run --project .\CouchLink.Host.Wpf\CouchLink.Host.Wpf.csproj
```

## Runtime endpoints

| Endpoint | Purpose |
|---|---|
| UDP `45820` | Host discovery advertisement |
| TCP `45821` | Trusted pairing, heartbeat, and launcher actions |

## Supported launchers

Steam, GOG Galaxy, Xbox, EA app, Ubisoft Connect, Rockstar Games Launcher, Epic Games Launcher, and Amazon Games. The host focuses an existing launcher where possible rather than starting duplicates.

## Security boundary

The host accepts trusted token-based sessions paired with a six-digit desktop code. It performs no Windows input injection, installs no virtual HID driver, does not bypass Windows authentication, and does not expose a cloud service.

## Documentation

- [Repository overview](../../README.md)
- [Installation and pairing](../../docs/release/INSTALLATION.md)
- [Architecture](../../docs/architecture/ARCHITECTURE.md)
- [Build instructions](../../docs/building/BUILDING.md)
- [Testing](../../docs/building/TESTING.md)
- [Release notes](../../docs/release/RELEASE-NOTES-v1.1.md)
- [Apache 2.0 license](../../LICENSE)
- [Trademark and brand policy](../../TRADEMARKS.md)

## License and branding

The Windows host source is licensed under the [Apache License 2.0](../../LICENSE). CouchLink names, logos, icons, artwork, screenshots, and official branding are covered separately by the [CouchLink Trademark and Brand Policy](../../TRADEMARKS.md).
