namespace CouchLink.Host.Core.Networking;

/// <summary>Owns the one interface identity selected for a host runtime session.</summary>
internal sealed class SessionInterfaceSelection
{
    private IReadOnlyList<NetworkIdentity> _candidates = [];
    private string? _selectedAdapterId;
    private HostNetworkSelectionMode? _selectionMode;

    public NetworkIdentity? CurrentIdentity { get; private set; }
    public bool RequiresManualReselection { get; private set; }
    public HostNetworkSelectionSnapshot Snapshot => CreateSnapshot();

    public void Start(IEnumerable<NetworkInterfaceInventoryEntry> inventory) =>
        Start(NetworkAddressHelper.GetEligibleNetworkIdentities(inventory));

    public void Start(IEnumerable<NetworkIdentity> inventory)
    {
        _candidates = inventory.ToArray();
        CurrentIdentity = _candidates.FirstOrDefault();
        _selectedAdapterId = CurrentIdentity?.AdapterId;
        _selectionMode = CurrentIdentity is null ? null : HostNetworkSelectionMode.Automatic;
        // Startup with no eligible interface is not a loss: keep waiting for the first one.
        RequiresManualReselection = false;
    }

    public NetworkIdentity? Refresh(IEnumerable<NetworkInterfaceInventoryEntry> inventory) =>
        Refresh(NetworkAddressHelper.GetEligibleNetworkIdentities(inventory));

    public NetworkIdentity? Refresh(IEnumerable<NetworkIdentity> inventory)
    {
        _candidates = inventory.ToArray();
        if (RequiresManualReselection)
        {
            return null;
        }

        if (_selectedAdapterId is null)
        {
            CurrentIdentity = _candidates.FirstOrDefault();
            if (CurrentIdentity is not null)
            {
                _selectedAdapterId = CurrentIdentity.AdapterId;
                _selectionMode = HostNetworkSelectionMode.Automatic;
            }
            return CurrentIdentity;
        }

        CurrentIdentity = _candidates.FirstOrDefault(
            identity => string.Equals(identity.AdapterId, _selectedAdapterId, StringComparison.Ordinal));
        if (CurrentIdentity is null)
        {
            _selectedAdapterId = null;
            _selectionMode = null;
            RequiresManualReselection = true;
        }

        return CurrentIdentity;
    }

    public bool Select(string? candidateId, IEnumerable<NetworkInterfaceInventoryEntry> inventory) =>
        Select(candidateId, NetworkAddressHelper.GetEligibleNetworkIdentities(inventory));

    public bool Select(string? candidateId, IEnumerable<NetworkIdentity> inventory)
    {
        if (string.IsNullOrWhiteSpace(candidateId))
        {
            return false;
        }

        NetworkIdentity[] candidates = inventory.ToArray();
        NetworkIdentity? candidate = candidates.FirstOrDefault(
            identity => string.Equals(identity.AdapterId, candidateId, StringComparison.Ordinal));
        if (candidate is null)
        {
            return false;
        }

        _candidates = candidates;
        CurrentIdentity = candidate;
        _selectedAdapterId = candidate.AdapterId;
        _selectionMode = HostNetworkSelectionMode.Manual;
        RequiresManualReselection = false;
        return true;
    }

    public void Stop()
    {
        _candidates = [];
        _selectedAdapterId = null;
        _selectionMode = null;
        CurrentIdentity = null;
        RequiresManualReselection = false;
    }

    private HostNetworkSelectionSnapshot CreateSnapshot()
    {
        HostNetworkSelectionAvailability availability = CurrentIdentity is not null
            ? HostNetworkSelectionAvailability.Available
            : RequiresManualReselection
                ? HostNetworkSelectionAvailability.UnavailableRequiresReselection
                : HostNetworkSelectionAvailability.NotStarted;
        return new HostNetworkSelectionSnapshot(
            _candidates.Select(static identity => new HostNetworkCandidate(
                identity.AdapterId, identity.DisplayName, identity.Address.ToString())).ToArray(),
            CurrentIdentity?.AdapterId,
            CurrentIdentity?.DisplayName,
            CurrentIdentity?.Address.ToString(),
            _selectionMode,
            availability);
    }
}
