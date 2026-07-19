using CouchLink.Host.Core;

namespace CouchLink.SessionHost;

public sealed class SessionHostCoordinator : IAsyncDisposable
{
    private readonly CouchLinkHostRuntime _runtime = new();
    private readonly BootServiceStatusReader _bootReader = new();
    private PeriodicTimer? _statusTimer;
    private CancellationTokenSource? _statusCancellation;
    private Task? _statusTask;
    private BootServiceStatus _bootStatus = BootServiceStatus.Unavailable();

    public SessionHostCoordinator()
    {
        _runtime.SnapshotChanged += OnHostSnapshotChanged;
    }

    public event EventHandler<SessionHostSnapshot>? SnapshotChanged;

    public SessionHostSnapshot Snapshot => new(
        _runtime.Snapshot,
        _bootStatus,
        _runtime.Snapshot.IsRunning ? "Desktop session active" : "Desktop session stopped",
        DateTimeOffset.UtcNow);

    public async Task StartAsync()
    {
        await _runtime.StartAsync().ConfigureAwait(false);
        _bootStatus = _bootReader.Read();
        _statusCancellation = new CancellationTokenSource();
        _statusTimer = new PeriodicTimer(TimeSpan.FromSeconds(3));
        _statusTask = PollBootServiceAsync(_statusCancellation.Token);
        RaiseSnapshotChanged();
    }

    public void ToggleRemoteInput() => _runtime.ToggleRemoteInput();
    public void PrepareForPairing() => _runtime.PrepareForPairing();
    public bool RevokeTrustedDevice(string clientId) => _runtime.RevokeTrustedDevice(clientId);
    public int RevokeAllTrustedDevices() => _runtime.RevokeAllTrustedDevices();

    private async Task PollBootServiceAsync(CancellationToken cancellationToken)
    {
        try
        {
            while (_statusTimer is not null && await _statusTimer.WaitForNextTickAsync(cancellationToken).ConfigureAwait(false))
            {
                _bootStatus = _bootReader.Read();
                RaiseSnapshotChanged();
            }
        }
        catch (OperationCanceledException) { }
    }

    private void OnHostSnapshotChanged(object? sender, HostSnapshot snapshot) => RaiseSnapshotChanged();

    private void RaiseSnapshotChanged() => SnapshotChanged?.Invoke(this, Snapshot);

    public async ValueTask DisposeAsync()
    {
        _runtime.SnapshotChanged -= OnHostSnapshotChanged;
        _statusCancellation?.Cancel();
        if (_statusTask is not null)
        {
            try { await _statusTask.ConfigureAwait(false); }
            catch (OperationCanceledException) { }
        }
        _statusTimer?.Dispose();
        _statusCancellation?.Dispose();
        await _runtime.DisposeAsync().ConfigureAwait(false);
    }
}
