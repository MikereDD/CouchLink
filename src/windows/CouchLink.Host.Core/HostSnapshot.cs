namespace CouchLink.Host.Core;

public enum HostNetworkSelectionAvailability
{
    NotStarted,
    Available,
    UnavailableRequiresReselection,
}

public enum HostNetworkSelectionMode
{
    Automatic,
    Manual,
}

public sealed record HostNetworkCandidate(
    string Id,
    string DisplayName,
    string Address);

/// <summary>
/// The complete advertised-network selection for the current host session.
/// </summary>
public sealed record HostNetworkSelectionSnapshot(
    IReadOnlyList<HostNetworkCandidate> Candidates,
    string? SelectedCandidateId,
    string? SelectedLabel,
    string? SelectedAddress,
    HostNetworkSelectionMode? SelectionMode,
    HostNetworkSelectionAvailability Availability);

public sealed record HostSnapshot(
    string HostName,
    string HostId,
    string HostVersion,
    string HostState,
    HostNetworkSelectionSnapshot NetworkSelection,
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
