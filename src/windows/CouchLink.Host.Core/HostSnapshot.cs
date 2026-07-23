namespace CouchLink.Host.Core;

public sealed record HostSnapshot(
    string HostName,
    string HostId,
    string HostVersion,
    string HostState,
    string PrimaryAddress,
    int DiscoveryPort,
    int SessionPort,
    bool IsRunning,
    int ConnectedClients,
    string ConnectedDevice,
    int TrustedDevices,
    IReadOnlyList<TrustedDeviceInfo> TrustedDeviceList,
    string PairingCode,
    string LastEvent,
    DateTimeOffset UpdatedAtUtc);
