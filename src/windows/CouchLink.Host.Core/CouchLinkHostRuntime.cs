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
    private readonly PairingStore _pairingStore = new();
    private readonly PairingCoordinator _pairingCoordinator = new();
    private CancellationTokenSource? _cancellationTokenSource;
    private DiscoveryAdvertiser? _advertiser;
    private SessionServer? _sessionServer;
    private Task? _advertiserTask;
    private Task? _sessionTask;
    private string _lastEvent = "Host has not started.";
    private string _hostState = "DesktopReady";
    private string _pairingCode = "------";
    private string _connectedDevice = "None";

    public CouchLinkHostRuntime()
    {
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
            _sessionServer = new SessionServer(
                _hostId,
                _hostName,
                () => _hostState,
                RecordEvent,
                _pairingStore,
                _pairingCoordinator,
                SetPairingCode,
                SetConnectedDevice);
            _advertiser = new DiscoveryAdvertiser(CreateAdvertisement);
            _sessionTask = ObserveTaskAsync(_sessionServer.RunAsync(_cancellationTokenSource.Token), "Session listener");
            _advertiserTask = ObserveTaskAsync(_advertiser.RunAsync(_cancellationTokenSource.Token), "Discovery advertiser");
            _lastEvent = "Discovery advertising and launcher-control session listening started.";
        }
        RaiseSnapshotChanged();
        return Task.CompletedTask;
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

    private DiscoveryAdvertisement CreateAdvertisement() => new(
        HostConstants.ProductName,
        ProtocolEnvelope.CurrentProtocolVersion,
        _hostId,
        _hostName,
        HostConstants.HostVersion,
        _hostState,
        GetPrimaryAddress(),
        HostConstants.SessionPort,
        PairingRequired: true,
        GetPrimaryMacAddress(),
        DateTimeOffset.UtcNow);

    private HostSnapshot CreateSnapshot()
    {
        bool running;
        string lastEvent;
        SessionServer? server;
        lock (_sync)
        {
            running = _cancellationTokenSource is not null;
            lastEvent = _lastEvent;
            server = _sessionServer;
        }
        return new HostSnapshot(
            _hostName, _hostId, HostConstants.HostVersion, _hostState, GetPrimaryAddress(),
            HostConstants.DiscoveryPort, HostConstants.SessionPort, running,
            server?.ConnectedClients ?? 0, _connectedDevice, _pairingStore.Count, _pairingStore.GetDevices(),
            _pairingCode, lastEvent, DateTimeOffset.UtcNow);
    }

    private static string GetPrimaryAddress() =>
        NetworkAddressHelper.GetPrimaryNetworkIdentity()?.Address.ToString() ??
        NetworkAddressHelper.GetActiveIpv4Addresses().FirstOrDefault().Address?.ToString() ?? "Unavailable";

    private static string GetPrimaryMacAddress() =>
        NetworkAddressHelper.GetPrimaryNetworkIdentity()?.MacAddress ?? string.Empty;

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
