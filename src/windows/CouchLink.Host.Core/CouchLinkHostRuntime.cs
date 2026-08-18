using System.Security.Cryptography;
using System.Text;
using CouchLink.Host.Core.Networking;
using CouchLink.Protocol;

namespace CouchLink.Host.Core;

public sealed class CouchLinkHostRuntime : IAsyncDisposable
{
    private readonly object _sync = new();
    private readonly string _hostName = Environment.MachineName;
    private readonly string _hostId;
    private readonly PairingStore _pairingStore;
    private readonly PairingCoordinator _pairingCoordinator = new();
    private readonly SessionInterfaceSelection _interfaceSelection = new();
    private readonly Func<IReadOnlyList<NetworkIdentity>> _networkIdentityProvider;
    private readonly bool _startNetworkServices;
    private CancellationTokenSource? _cancellationTokenSource;
    private DiscoveryAdvertiser? _advertiser;
    private SessionServer? _sessionServer;
    private Task? _advertiserTask;
    private Task? _sessionTask;
    private string _lastEvent = "Host has not started.";
    private string _hostState = "DesktopReady";
    private string _pairingCode = "------";
    private string _connectedDevice = "None";

    public CouchLinkHostRuntime() : this(NetworkAddressHelper.GetEligibleNetworkIdentities, new PairingStore(), startNetworkServices: true)
    {
    }

    internal CouchLinkHostRuntime(Func<IReadOnlyList<NetworkIdentity>> networkIdentityProvider)
        : this(networkIdentityProvider, new PairingStore(inMemory: true), startNetworkServices: false)
    {
    }

    private CouchLinkHostRuntime(
        Func<IReadOnlyList<NetworkIdentity>> networkIdentityProvider,
        PairingStore pairingStore,
        bool startNetworkServices)
    {
        _networkIdentityProvider = networkIdentityProvider;
        _pairingStore = pairingStore;
        _startNetworkServices = startNetworkServices;
        _hostId = CreateStableDevelopmentHostId(_hostName);
    }

    public event EventHandler<HostSnapshot>? SnapshotChanged;
    public HostSnapshot Snapshot => CreateSnapshot();

    public Task StartAsync()
    {
        lock (_sync)
        {
            if (_cancellationTokenSource is not null) return Task.CompletedTask;
            _cancellationTokenSource = new CancellationTokenSource();
            _interfaceSelection.Start(_networkIdentityProvider());
            if (_startNetworkServices)
            {
                _sessionServer = new SessionServer(
                    _hostId, _hostName, () => _hostState, RecordEvent, _pairingStore,
                    _pairingCoordinator, SetPairingCode, SetConnectedDevice);
                _advertiser = new DiscoveryAdvertiser(CreateAdvertisement);
                _sessionTask = ObserveTaskAsync(_sessionServer.RunAsync(_cancellationTokenSource.Token), "Session listener");
                _advertiserTask = ObserveTaskAsync(_advertiser.RunAsync(_cancellationTokenSource.Token), "Discovery advertiser");
            }
            _lastEvent = _interfaceSelection.CurrentIdentity is null
                ? "No eligible LAN interface is available. Waiting for a network interface."
                : "Discovery advertising and launcher-control session listening started.";
        }
        RaiseSnapshotChanged();
        return Task.CompletedTask;
    }

    public bool SelectAdvertisedInterface(string? candidateId)
    {
        bool selected;
        lock (_sync)
        {
            selected = _cancellationTokenSource is not null && _interfaceSelection.Select(
                candidateId, _networkIdentityProvider());
            if (selected)
            {
                _lastEvent = "Discovery advertising resumed with the selected LAN interface.";
            }
        }

        if (selected) RaiseSnapshotChanged();
        return selected;
    }

    public void PrepareForPairing()
    {
        _pairingCoordinator.Clear();
        lock (_sync)
        {
            _pairingCode = "WAITING";
            _lastEvent = "Pairing mode enabled. Open CouchLink Remote to request a new code.";
        }
        RaiseSnapshotChanged();
    }

    public bool RevokeTrustedDevice(string clientId)
    {
        bool removed = _pairingStore.Revoke(clientId);
        if (!removed) return false;
        _sessionServer?.DisconnectClient(clientId);
        lock (_sync)
        {
            _connectedDevice = "None";
            _lastEvent = "Trusted device revoked.";
        }
        RaiseSnapshotChanged();
        return true;
    }

    public int RevokeAllTrustedDevices()
    {
        int removed = _pairingStore.RevokeAll();
        _sessionServer?.DisconnectAllClients();
        _pairingCoordinator.Clear();
        lock (_sync)
        {
            _connectedDevice = "None";
            _pairingCode = "------";
            _lastEvent = removed == 0 ? "No trusted devices to revoke." : $"Revoked {removed} trusted device(s).";
        }
        RaiseSnapshotChanged();
        return removed;
    }

    public async Task StopAsync()
    {
        CancellationTokenSource? cancellation;
        Task[] tasks;
        lock (_sync)
        {
            cancellation = _cancellationTokenSource;
            if (cancellation is null) return;
            tasks = new[] { _advertiserTask, _sessionTask }.OfType<Task>().ToArray();
            _cancellationTokenSource = null;
            _advertiserTask = null;
            _sessionTask = null;
            _interfaceSelection.Stop();
        }
        cancellation.Cancel();
        try { await Task.WhenAll(tasks).ConfigureAwait(false); }
        catch (OperationCanceledException) { }
        finally
        {
            cancellation.Dispose();
            if (_advertiser is not null) { await _advertiser.DisposeAsync().ConfigureAwait(false); _advertiser = null; }
            if (_sessionServer is not null) { await _sessionServer.DisposeAsync().ConfigureAwait(false); _sessionServer = null; }
        }
        RecordEvent("Host stopped.");
    }

    internal DiscoveryAdvertisementTarget? CreateAdvertisement()
    {
        bool snapshotChanged = false;
        DiscoveryAdvertisementTarget? advertisement;
        lock (_sync)
        {
            if (_cancellationTokenSource is null) return null;

            HostNetworkSelectionSnapshot before = _interfaceSelection.Snapshot;
            NetworkIdentity? identity = _interfaceSelection.Refresh(_networkIdentityProvider());
            if (identity is null)
            {
                if (_interfaceSelection.RequiresManualReselection)
                {
                    _lastEvent = "Selected LAN interface is unavailable. Manual reselection is required.";
                }
                advertisement = null;
            }
            else
            {
                advertisement = new DiscoveryAdvertisementTarget(
                    new DiscoveryAdvertisement(
                        HostConstants.ProductName,
                        ProtocolEnvelope.CurrentProtocolVersion,
                        _hostId,
                        _hostName,
                        HostConstants.HostVersion,
                        _hostState,
                        identity.Address.ToString(),
                        HostConstants.SessionPort,
                        PairingRequired: true,
                        identity.MacAddress,
                        DateTimeOffset.UtcNow),
                    identity.Address,
                    identity.Broadcast);
            }

            HostNetworkSelectionSnapshot after = _interfaceSelection.Snapshot;
            snapshotChanged = before.Availability != after.Availability ||
                before.SelectedCandidateId != after.SelectedCandidateId ||
                !before.Candidates.SequenceEqual(after.Candidates);
        }

        if (snapshotChanged) RaiseSnapshotChanged();
        return advertisement;
    }

    private HostSnapshot CreateSnapshot()
    {
        lock (_sync)
        {
            return new HostSnapshot(
                _hostName, _hostId, HostConstants.HostVersion, _hostState, _interfaceSelection.Snapshot,
                HostConstants.DiscoveryPort, HostConstants.SessionPort, _cancellationTokenSource is not null,
                _sessionServer?.ConnectedClients ?? 0, _connectedDevice, _pairingStore.Count, _pairingStore.GetDevices(),
                _pairingCode, _lastEvent, DateTimeOffset.UtcNow);
        }
    }

    private void SetConnectedDevice(string deviceName) { lock (_sync) _connectedDevice = deviceName; RaiseSnapshotChanged(); }
    private void SetPairingCode(string code) { lock (_sync) _pairingCode = code; RaiseSnapshotChanged(); }
    private void RecordEvent(string message) { lock (_sync) _lastEvent = message; RaiseSnapshotChanged(); }

    private async Task ObserveTaskAsync(Task task, string component)
    {
        try { await task.ConfigureAwait(false); }
        catch (OperationCanceledException) { }
        catch (Exception ex) { RecordEvent($"{component} failed: {ex.Message}"); }
    }

    private void RaiseSnapshotChanged() => SnapshotChanged?.Invoke(this, CreateSnapshot());

    private static string CreateStableDevelopmentHostId(string hostName)
    {
        byte[] digest = SHA256.HashData(Encoding.UTF8.GetBytes($"CouchLink/dev/{hostName}"));
        return Convert.ToHexString(digest[..8]).ToLowerInvariant();
    }

    public async ValueTask DisposeAsync() => await StopAsync().ConfigureAwait(false);
}
