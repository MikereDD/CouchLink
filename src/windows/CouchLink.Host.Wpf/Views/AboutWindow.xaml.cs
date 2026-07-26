using System.Net;
using System.Windows;
using CouchLink.Host.Wpf.ViewModels;
using CouchLink.Host.Wpf.Services;
using System.Net.Http;
using System.IO;

namespace CouchLink.Host.Wpf.Views;

public partial class AboutWindow : Window
{
    private readonly GitHubUpdateService _updateService = new();
    private GitHubUpdateService.UpdateInfo? _availableUpdate;
    private string? _lastUpdateError;
    private bool _testChannel;
    private static readonly string ChannelPreferencePath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "CouchLink",
        "update-channel.txt");
    public AboutWindow()
    {
        InitializeComponent();
        LoadUpdateChannel();
    }

    private void LoadUpdateChannel()
    {
        _testChannel = File.Exists(ChannelPreferencePath) &&
            File.ReadAllText(ChannelPreferencePath).Trim()
                .Equals("test", StringComparison.OrdinalIgnoreCase);
        UpdateChannelComboBox.SelectedIndex = _testChannel ? 1 : 0;
        ApplyUpdateChannelText();
    }

    private void UpdateChannel_SelectionChanged(
        object sender,
        System.Windows.Controls.SelectionChangedEventArgs e)
    {
        if (!IsInitialized) return;
        _testChannel = UpdateChannelComboBox.SelectedIndex == 1;
        Directory.CreateDirectory(Path.GetDirectoryName(ChannelPreferencePath)!);
        File.WriteAllText(ChannelPreferencePath, _testChannel ? "test" : "stable");
        _availableUpdate = null;
        InstallUpdateButton.IsEnabled = false;
        ApplyUpdateChannelText();
    }

    private void ApplyUpdateChannelText()
    {
        UpdateChannelDescription.Text = _testChannel
            ? "Selected prereleases for volunteer testers."
            : "Stable releases only.";
        UpdateStatusText.Text = _testChannel
            ? "Test updates are checked against official CouchLink prereleases."
            : "Stable updates are checked against the official MikereDD/CouchLink GitHub releases.";
    }



    private async void CheckUpdates_Click(object sender, RoutedEventArgs e)
    {
        CheckUpdatesButton.IsEnabled = false;
        InstallUpdateButton.IsEnabled = false;
        UpdateStatusText.Text = "Checking GitHub Releases…";
        _lastUpdateError = null;
        try
        {
            _availableUpdate = await _updateService.CheckAsync(_testChannel);
            if (_availableUpdate is null)
            {
                UpdateStatusText.Text = "CouchLink is up to date.";
                UpdateNotesText.Visibility = Visibility.Collapsed;
                return;
            }
            UpdateStatusText.Text = $"CouchLink {_availableUpdate.Version} is available ({_availableUpdate.HostSize / (1024.0 * 1024.0):0.0} MB).";
            UpdateNotesText.Text = _availableUpdate.ReleaseNotes;
            UpdateNotesText.Visibility = string.IsNullOrWhiteSpace(_availableUpdate.ReleaseNotes)
                ? Visibility.Collapsed
                : Visibility.Visible;
            InstallUpdateButton.IsEnabled = true;
        }
        catch (HttpRequestException ex) when (ex.StatusCode == HttpStatusCode.NotFound)
        {
            _lastUpdateError = ex.ToString();
            UpdateStatusText.Text = _testChannel
                ? "No published CouchLink test release is available yet."
                : "No published CouchLink release is available yet.";
            UpdateNotesText.Visibility = Visibility.Collapsed;
        }
        catch (HttpRequestException ex)
        {
            _lastUpdateError = ex.ToString();
            UpdateStatusText.Text = "CouchLink could not reach GitHub. Check your internet connection and try again.";
            UpdateNotesText.Visibility = Visibility.Collapsed;
        }
        catch (Exception ex)
        {
            _lastUpdateError = ex.ToString();
            UpdateStatusText.Text = "CouchLink could not check for updates. Copy diagnostics for technical details.";
            UpdateNotesText.Visibility = Visibility.Collapsed;
        }
        finally
        {
            CheckUpdatesButton.IsEnabled = true;
        }
    }

    private async void InstallUpdate_Click(object sender, RoutedEventArgs e)
    {
        if (_availableUpdate is null) return;
        MessageBoxResult answer = System.Windows.MessageBox.Show(
            this,
            $"Download and install CouchLink {_availableUpdate.Version}?\n\nThe Host will close briefly and restart after the verified update is installed.",
            "CouchLink update",
            MessageBoxButton.YesNo,
            MessageBoxImage.Information);
        if (answer != MessageBoxResult.Yes) return;

        CheckUpdatesButton.IsEnabled = false;
        InstallUpdateButton.IsEnabled = false;
        UpdateStatusText.Text = "Downloading and verifying the Windows update…";
        try
        {
            await _updateService.DownloadAndLaunchAsync(_availableUpdate);
            UpdateStatusText.Text = "Update verified. CouchLink will now close and restart.";
            if (System.Windows.Application.Current is App app)
                app.BeginUpdateShutdown();
            else
                System.Windows.Application.Current.Shutdown();
        }
        catch (Exception ex)
        {
            UpdateStatusText.Text = $"Update installation failed: {ex.Message}";
            CheckUpdatesButton.IsEnabled = true;
            InstallUpdateButton.IsEnabled = true;
        }
    }

    private void CopyDiagnostics_Click(object sender, RoutedEventArgs e)
    {
        if (DataContext is not MainWindowViewModel viewModel)
            return;

        string diagnostics = viewModel.DiagnosticsText + Environment.NewLine +
            $"Update channel: {(_testChannel ? "Test" : "Stable")}";
        if (!string.IsNullOrWhiteSpace(_lastUpdateError))
        {
            diagnostics += $"{Environment.NewLine}{Environment.NewLine}" +
                "Updater error:" + Environment.NewLine +
                _lastUpdateError;
        }

        System.Windows.Clipboard.SetText(diagnostics);
        System.Windows.MessageBox.Show(
            this,
            "CouchLink diagnostics were copied to the clipboard.",
            "CouchLink",
            MessageBoxButton.OK,
            MessageBoxImage.Information);
    }

    private void TitleBar_MouseLeftButtonDown(
        object sender,
        System.Windows.Input.MouseButtonEventArgs e)
    {
        if (e.ClickCount == 2)
        {
            ToggleMaximizeRestore();
            return;
        }

        if (e.LeftButton == System.Windows.Input.MouseButtonState.Pressed)
            DragMove();
    }

    private void Minimize_Click(object sender, RoutedEventArgs e) =>
        WindowState = WindowState.Minimized;

    private void MaximizeRestore_Click(object sender, RoutedEventArgs e) =>
        ToggleMaximizeRestore();

    private void ToggleMaximizeRestore() =>
        WindowState = WindowState == WindowState.Maximized
            ? WindowState.Normal
            : WindowState.Maximized;

    private void Close_Click(object sender, RoutedEventArgs e) => Close();
}
