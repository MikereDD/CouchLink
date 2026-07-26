using System.Collections.ObjectModel;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Threading;
using CouchLink.Host.Core;
using CouchLink.Host.Wpf.Services;

namespace CouchLink.Host.Wpf.Views;

public partial class AudioOutputWindow : Window
{
    private readonly AudioOutputController _audio = new();
    private readonly HostPreferences _preferences;
    private readonly ObservableCollection<AudioDeviceItem> _devices = new();
    private AudioOutputDevice[] _activeDevices = Array.Empty<AudioOutputDevice>();
    private bool _busy;
    private readonly DispatcherTimer _liveSyncTimer;
    private string _deviceFingerprint = string.Empty;

    public AudioOutputWindow(HostPreferences preferences)
    {
        InitializeComponent();
        _preferences = preferences;
        DeviceList.ItemsSource = _devices;
        _liveSyncTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(2) };
        _liveSyncTimer.Tick += async (_, _) => await RefreshDevicesIfChangedAsync();
        Activated += async (_, _) => await RefreshDevicesAsync();
        Loaded += async (_, _) =>
        {
            await RefreshDevicesAsync();
            _liveSyncTimer.Start();
        };
        Closed += (_, _) => _liveSyncTimer.Stop();
    }

    private async void Refresh_Click(object sender, RoutedEventArgs e) => await RefreshDevicesAsync();

    // Window dragging is handled by the WindowChrome caption region (CaptionHeight);
    // no manual DragMove handler is required.

    private void Minimize_Click(object sender, RoutedEventArgs e) => WindowState = WindowState.Minimized;
    private void Close_Click(object sender, RoutedEventArgs e) => Close();

    private async Task RefreshDevicesAsync()
    {
        if (_busy) return;
        _busy = true;
        try { await LoadDevicesAsync(); }
        finally { _busy = false; }
    }


    private async Task RefreshDevicesIfChangedAsync()
    {
        if (_busy || !IsVisible || WindowState == WindowState.Minimized) return;
        _busy = true;
        try
        {
            AudioOutputListResult result = await Task.Run(() => _audio.List());
            if (!result.Success) return;
            string fingerprint = CreateFingerprint(result.Devices);
            if (!string.Equals(fingerprint, _deviceFingerprint, StringComparison.Ordinal))
                ApplyDeviceResult(result);
        }
        finally { _busy = false; }
    }

    // Enumeration runs on a background thread; the await resumes on the WPF
    // dispatcher, so every control update below is marshaled back to the UI thread.
    private async Task LoadDevicesAsync()
    {
        AudioOutputListResult result = await Task.Run(() => _audio.List());
        if (!result.Success)
        {
            StatusText.Text = result.Error ?? "Unable to enumerate audio outputs.";
            return;
        }

        ApplyDeviceResult(result);
    }

    private void ApplyDeviceResult(AudioOutputListResult result)
    {
        _activeDevices = result.Devices;
        _deviceFingerprint = CreateFingerprint(_activeDevices);
        SuggestFavoritesIfNeeded();
        _devices.Clear();
        foreach (AudioOutputDevice device in _activeDevices)
            _devices.Add(new AudioDeviceItem(device.Id, CleanName(device.Name), device.IsDefault));

        AudioOutputDevice? current = _activeDevices.FirstOrDefault(d => d.IsDefault);
        CurrentOutputText.Text = current is null ? "No active output reported" : CleanName(current.Name);
        UpdateFavoriteCards();
        StatusText.Text = $"{_activeDevices.Length} active output{(_activeDevices.Length == 1 ? string.Empty : "s")}.";
    }

    private static string CreateFingerprint(IEnumerable<AudioOutputDevice> devices) =>
        string.Join("\n", devices.OrderBy(d => d.Id, StringComparer.OrdinalIgnoreCase)
            .Select(d => $"{d.Id}|{d.Name}|{d.IsDefault}"));

    private void SuggestFavoritesIfNeeded()
    {
        bool changed = false;
        if (string.IsNullOrWhiteSpace(_preferences.FavoriteHeadphonesEndpointId))
        {
            AudioOutputDevice? headphones = _activeDevices.FirstOrDefault(d =>
                d.Name.Contains("Headphones", StringComparison.OrdinalIgnoreCase) ||
                d.Name.Contains("Headset Earphone", StringComparison.OrdinalIgnoreCase));
            if (headphones is not null)
            {
                _preferences.FavoriteHeadphonesEndpointId = headphones.Id;
                _preferences.FavoriteHeadphonesName = headphones.Name;
                changed = true;
            }
        }
        if (string.IsNullOrWhiteSpace(_preferences.FavoriteDisplayEndpointId))
        {
            AudioOutputDevice? display = _activeDevices.FirstOrDefault(d =>
                d.Name.Contains("NVIDIA High Definition Audio", StringComparison.OrdinalIgnoreCase) ||
                d.Name.Contains("Display Audio", StringComparison.OrdinalIgnoreCase) ||
                d.Name.Contains("HDMI", StringComparison.OrdinalIgnoreCase));
            if (display is not null)
            {
                _preferences.FavoriteDisplayEndpointId = display.Id;
                _preferences.FavoriteDisplayName = display.Name;
                changed = true;
            }
        }
        if (changed) _preferences.Save();
    }

    private void UpdateFavoriteCards()
    {
        UpdateFavorite(HeadphonesButton, HeadphonesName, HeadphonesStatus,
            _preferences.FavoriteHeadphonesEndpointId, _preferences.FavoriteHeadphonesName);
        UpdateFavorite(DisplayButton, DisplayName, DisplayStatus,
            _preferences.FavoriteDisplayEndpointId, _preferences.FavoriteDisplayName);
    }

    private void UpdateFavorite(System.Windows.Controls.Button button, TextBlock nameBlock, TextBlock statusBlock, string? id, string? savedName)
    {
        if (string.IsNullOrWhiteSpace(id))
        {
            nameBlock.Text = "Not assigned";
            statusBlock.Text = "CHOOSE OUTPUT";
            button.IsEnabled = false;
            return;
        }
        AudioOutputDevice? active = _activeDevices.FirstOrDefault(d => string.Equals(d.Id, id, StringComparison.OrdinalIgnoreCase));
        nameBlock.Text = CleanName(active?.Name ?? savedName ?? "Saved output");
        statusBlock.Text = active is null ? "UNAVAILABLE" : active.IsDefault ? "CURRENT OUTPUT" : "TAP TO SWITCH";
        statusBlock.Foreground = active is null ? System.Windows.Media.Brushes.IndianRed : (System.Windows.Media.Brush)FindResource(active.IsDefault ? "SuccessBrush" : "MutedTextBrush");
        button.IsEnabled = active is not null;
    }

    private async void Headphones_Click(object sender, RoutedEventArgs e) => await SwitchToAsync(_preferences.FavoriteHeadphonesEndpointId);
    private async void Display_Click(object sender, RoutedEventArgs e) => await SwitchToAsync(_preferences.FavoriteDisplayEndpointId);

    private async void Device_Click(object sender, RoutedEventArgs e)
    {
        if (sender is System.Windows.Controls.Button button && button.Tag is string id) await SwitchToAsync(id);
    }

    private async Task SwitchToAsync(string? endpointId)
    {
        if (string.IsNullOrWhiteSpace(endpointId)) return;
        if (_busy) return;
        _busy = true;
        IsEnabled = false;
        try
        {
            // Switching (and its internal re-enumeration) runs off the UI thread.
            AudioOutputSetResult result = await Task.Run(() => _audio.SetDefault(endpointId));
            StatusText.Text = result.Message;
            await LoadDevicesAsync();
        }
        finally
        {
            IsEnabled = true;
            _busy = false;
        }
    }

    private void EditHeadphones_Click(object sender, RoutedEventArgs e) => ShowAssignmentMenu((System.Windows.Controls.Button)sender, true);
    private void EditDisplay_Click(object sender, RoutedEventArgs e) => ShowAssignmentMenu((System.Windows.Controls.Button)sender, false);

    private void ShowAssignmentMenu(System.Windows.Controls.Button owner, bool headphones)
    {
        ContextMenu menu = new();
        foreach (AudioOutputDevice device in _activeDevices)
        {
            MenuItem item = new() { Header = CleanName(device.Name), Tag = device };
            item.Click += (_, _) => AssignFavorite((AudioOutputDevice)item.Tag, headphones);
            menu.Items.Add(item);
        }
        menu.Items.Add(new Separator());
        MenuItem clear = new() { Header = "Clear assignment" };
        clear.Click += (_, _) => ClearFavorite(headphones);
        menu.Items.Add(clear);
        owner.ContextMenu = menu;
        menu.PlacementTarget = owner;
        menu.IsOpen = true;
    }

    private void AssignFavorite(AudioOutputDevice device, bool headphones)
    {
        if (headphones)
        {
            _preferences.FavoriteHeadphonesEndpointId = device.Id;
            _preferences.FavoriteHeadphonesName = device.Name;
        }
        else
        {
            _preferences.FavoriteDisplayEndpointId = device.Id;
            _preferences.FavoriteDisplayName = device.Name;
        }
        _preferences.Save();
        UpdateFavoriteCards();
    }

    private void ClearFavorite(bool headphones)
    {
        if (headphones)
        {
            _preferences.FavoriteHeadphonesEndpointId = null;
            _preferences.FavoriteHeadphonesName = null;
        }
        else
        {
            _preferences.FavoriteDisplayEndpointId = null;
            _preferences.FavoriteDisplayName = null;
        }
        _preferences.Save();
        UpdateFavoriteCards();
    }

    private static string CleanName(string name)
    {
        string cleaned = name
            .Replace(" (NVIDIA High Definition Audio)", string.Empty, StringComparison.OrdinalIgnoreCase)
            .Replace(" (SteelSeries Sonar Virtual Audio Device)", string.Empty, StringComparison.OrdinalIgnoreCase)
            .Replace(" (Realtek(R) Audio)", " (Realtek)", StringComparison.OrdinalIgnoreCase);
        return cleaned;
    }

    private sealed record AudioDeviceItem(string Id, string DisplayName, bool IsDefault)
    {
        public System.Windows.Media.Brush IndicatorBrush => IsDefault ? System.Windows.Media.Brushes.LimeGreen : System.Windows.Media.Brushes.Transparent;
        public System.Windows.Media.Brush CardBrush => IsDefault
            ? new LinearGradientBrush(System.Windows.Media.Color.FromRgb(53, 32, 20), System.Windows.Media.Color.FromRgb(27, 18, 13), 0)
            : new LinearGradientBrush(System.Windows.Media.Color.FromRgb(21, 29, 37), System.Windows.Media.Color.FromRgb(13, 19, 25), 90);
        public System.Windows.Media.Brush BorderBrush => IsDefault
            ? new SolidColorBrush(System.Windows.Media.Color.FromRgb(255, 132, 31))
            : new SolidColorBrush(System.Windows.Media.Color.FromRgb(53, 65, 76));
        public Visibility CurrentVisibility => IsDefault ? Visibility.Visible : Visibility.Collapsed;
    }
}
