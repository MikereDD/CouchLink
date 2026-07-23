using System.ComponentModel;
using System.Runtime.CompilerServices;
using System.Text;
using System.Windows;
using System.Windows.Input;
using CouchLink.Host.Core;
using CouchLink.Host.Wpf.Services;

namespace CouchLink.Host.Wpf.ViewModels;

public sealed class MainWindowViewModel : INotifyPropertyChanged, IAsyncDisposable
{
    private readonly CouchLinkHostRuntime _runtime = new();
    private HostSnapshot _snapshot;
    private TrustedDeviceInfo? _selectedTrustedDevice;
    private readonly HostPreferences _preferences = HostPreferences.Load();

    public MainWindowViewModel()
    {
        _snapshot = _runtime.Snapshot;
        _runtime.SnapshotChanged += OnSnapshotChanged;
        PairNewDeviceCommand = new RelayCommand(_runtime.PrepareForPairing);
        RevokeSelectedDeviceCommand = new RelayCommand(RevokeSelectedDevice);
        RevokeAllDevicesCommand = new RelayCommand(RevokeAllDevices);
    }

    public event PropertyChangedEventHandler? PropertyChanged;
    public ICommand PairNewDeviceCommand { get; }
    public ICommand RevokeSelectedDeviceCommand { get; }
    public ICommand RevokeAllDevicesCommand { get; }
    public string HostName => _snapshot.HostName;
    public string HostId => _snapshot.HostId;
    public string HostVersion => _snapshot.HostVersion;
    public string HostState => _snapshot.HostState;
    public string DiscoveryEndpoint => $"UDP {_snapshot.DiscoveryPort}";
    public string SessionEndpoint => $"{_snapshot.PrimaryAddress}:{_snapshot.SessionPort}";
    public string ServiceStatus => _snapshot.IsRunning ? "Listening" : "Stopped";
    public string VersionBadge => $"{HostVersion} — Local-first Windows launcher host";
    public string ProtocolVersion => "1";
    public string BuildChannel => HostVersion.Contains("dev", StringComparison.OrdinalIgnoreCase)
        ? "Development"
        : "Release";
    public string PrivacySummary =>
        "Local-first control. Pairing and launcher traffic stay on the local network; no cloud account is required.";
    public string DiagnosticsText
    {
        get
        {
            StringBuilder text = new();
            text.AppendLine("CouchLink Host diagnostics");
            text.AppendLine($"Host version: {HostVersion}");
            text.AppendLine($"Build channel: {BuildChannel}");
            text.AppendLine($"Protocol version: {ProtocolVersion}");
            text.AppendLine($"Host name: {HostName}");
            text.AppendLine($"Host ID: {HostId}");
            text.AppendLine($"Host state: {HostState}");
            text.AppendLine($"Discovery endpoint: {DiscoveryEndpoint}");
            text.AppendLine($"Session endpoint: {SessionEndpoint}");
            text.AppendLine($"Host listener: {ServiceStatus}");
            text.AppendLine($"Connected clients: {ConnectedClients}");
            text.AppendLine($"Connected device: {ConnectedDevice}");
            text.AppendLine($"Trusted devices: {TrustedDevices}");
            text.Append($"Last host event: {LastEvent}");
            return text.ToString();
        }
    }

    public string DiscoveryStatus => _snapshot.IsRunning ? "Advertising CouchLink Host" : "Not advertising";
    public string ConnectedClients => _snapshot.ConnectedClients.ToString();
    public string ConnectedDevice => _snapshot.ConnectedDevice;
    public string TrustedDevices => _snapshot.TrustedDevices.ToString();
    public IReadOnlyList<TrustedDeviceInfo> TrustedDeviceList => _snapshot.TrustedDeviceList;
    public TrustedDeviceInfo? SelectedTrustedDevice
    {
        get => _selectedTrustedDevice;
        set { _selectedTrustedDevice = value; OnPropertyChanged(); }
    }
    public string PairingCodeDisplay => _snapshot.PairingCode;
    public string PairingHelpText => _snapshot.PairingCode switch
    {
        "WAITING" => "Waiting for an untrusted CouchLink Remote to request a code…",
        "------" => "Choose Pair New Device, then open CouchLink Remote.",
        _ => "Enter this six-digit code on the Android device. It expires after five minutes."
    };
    public string LastEvent => _snapshot.LastEvent;
    public bool StartWithWindows
    {
        get => _preferences.StartWithWindows;
        set { _preferences.StartWithWindows = value; _preferences.Save(); OnPropertyChanged(); }
    }
    public bool StartMinimized
    {
        get => _preferences.StartMinimized;
        set { _preferences.StartMinimized = value; _preferences.Save(); OnPropertyChanged(); }
    }

    private void RevokeSelectedDevice()
    {
        TrustedDeviceInfo? device = SelectedTrustedDevice;
        if (device is null)
        {
            System.Windows.MessageBox.Show("Select a trusted device first.", "CouchLink", MessageBoxButton.OK, MessageBoxImage.Information);
            return;
        }
        MessageBoxResult result = System.Windows.MessageBox.Show(
            $"Revoke {device.ClientName}? The device will need to pair again.",
            "Revoke trusted device", MessageBoxButton.YesNo, MessageBoxImage.Warning);
        if (result != MessageBoxResult.Yes) return;
        _runtime.RevokeTrustedDevice(device.ClientId);
        SelectedTrustedDevice = null;
    }

    private void RevokeAllDevices()
    {
        if (_snapshot.TrustedDevices == 0) return;
        MessageBoxResult result = System.Windows.MessageBox.Show(
            "Revoke every trusted CouchLink device? All devices will need to pair again.",
            "Revoke all trusted devices", MessageBoxButton.YesNo, MessageBoxImage.Warning);
        if (result != MessageBoxResult.Yes) return;
        _runtime.RevokeAllTrustedDevices();
        SelectedTrustedDevice = null;
    }

    public async Task StartAsync() => await _runtime.StartAsync();

    private void OnSnapshotChanged(object? sender, HostSnapshot snapshot)
    {
        System.Windows.Application.Current.Dispatcher.Invoke(() =>
        {
            _snapshot = snapshot;
            OnPropertyChanged(string.Empty);
        });
    }

    private void OnPropertyChanged([CallerMemberName] string? propertyName = null) =>
        PropertyChanged?.Invoke(this, new PropertyChangedEventArgs(propertyName));

    public async ValueTask DisposeAsync()
    {
        _runtime.SnapshotChanged -= OnSnapshotChanged;
        await _runtime.DisposeAsync();
    }
}
