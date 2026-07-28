using System.IO;
using System.Net;
using System.Net.Http;
using System.Text.RegularExpressions;
using System.Windows;
using CouchLink.Host.Wpf.Services;
using CouchLink.Host.Wpf.ViewModels;

namespace CouchLink.Host.Wpf.Views;

public partial class AboutWindow : Window
{
    private readonly GitHubUpdateService _updateService = new();
    private GitHubUpdateService.UpdateInfo? _availableUpdate;
    private CancellationTokenSource? _operationCts;
    private string? _lastUpdateError;
    private bool _testChannel;
    private bool _operationRunning;

    private static readonly string ChannelPreferencePath = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
        "CouchLink",
        "update-channel.txt");

    public AboutWindow()
    {
        InitializeComponent();
        Closing += (_, _) => _operationCts?.Cancel();
        LoadUpdateChannel();
    }

    private void LoadUpdateChannel()
    {
        try
        {
            _testChannel = File.Exists(ChannelPreferencePath) &&
                File.ReadAllText(ChannelPreferencePath).Trim()
                    .Equals("test", StringComparison.OrdinalIgnoreCase);
        }
        catch (Exception ex)
        {
            _testChannel = false;
            _lastUpdateError = ex.ToString();
        }

        UpdateChannelComboBox.SelectedIndex = _testChannel ? 1 : 0;
        ApplyUpdateChannelText();
    }

    private void UpdateChannel_SelectionChanged(
        object sender,
        System.Windows.Controls.SelectionChangedEventArgs e)
    {
        if (!IsInitialized || _operationRunning)
        {
            return;
        }

        _testChannel = UpdateChannelComboBox.SelectedIndex == 1;
        try
        {
            Directory.CreateDirectory(Path.GetDirectoryName(ChannelPreferencePath)!);
            File.WriteAllText(ChannelPreferencePath, _testChannel ? "test" : "stable");
        }
        catch (Exception ex)
        {
            _lastUpdateError = ex.ToString();
            UpdateStatusText.Text = "The update channel could not be saved. CouchLink will continue using it for this session.";
        }

        _availableUpdate = null;
        InstallUpdateButton.IsEnabled = false;
        ApplyUpdateChannelText();
    }

    private void ApplyUpdateChannelText()
    {
        UpdateChannelDescription.Text = _testChannel
            ? "Selected prereleases for volunteer testers."
            : "Stable releases only.";
        ReturnStableButton.Visibility = _testChannel
            ? Visibility.Visible
            : Visibility.Collapsed;
        UpdateStatusText.Text = _testChannel
            ? "Test updates are checked against official CouchLink prereleases."
            : "Stable updates are checked against the official MikereDD/CouchLink GitHub releases.";
        UpdateStageText.Text = "Idle";
        UpdateProgressBar.Value = 0;
    }

    private async void CheckUpdates_Click(object sender, RoutedEventArgs e)
    {
        if (_operationRunning)
        {
            _operationCts?.Cancel();
            return;
        }

        BeginOperation("CANCEL CHECK");
        InstallUpdateButton.IsEnabled = false;
        UpdateStatusText.Text = "Checking GitHub Releases…";
        UpdateStageText.Text = "Checking";
        UpdateProgressBar.Value = 0;
        _lastUpdateError = null;

        try
        {
            _availableUpdate = await _updateService.CheckAsync(
                _testChannel,
                _operationCts!.Token);

            if (_availableUpdate is null)
            {
                UpdateStatusText.Text = "CouchLink is up to date.";
                UpdateStageText.Text = "Check complete";
                UpdateNotesText.Visibility = Visibility.Collapsed;
                return;
            }

            UpdateStatusText.Text =
                $"CouchLink {_availableUpdate.Version} is available " +
                $"({_availableUpdate.HostSize / (1024.0 * 1024.0):0.0} MB).";
            UpdateStageText.Text = "Update available";
            UpdateNotesText.Text = CleanReleaseNotes(_availableUpdate.ReleaseNotes);
            UpdateNotesText.Visibility = string.IsNullOrWhiteSpace(_availableUpdate.ReleaseNotes)
                ? Visibility.Collapsed
                : Visibility.Visible;
            InstallUpdateButton.IsEnabled = true;
        }
        catch (OperationCanceledException)
        {
            UpdateStatusText.Text = "Update check cancelled.";
            UpdateStageText.Text = "Cancelled";
        }
        catch (FileNotFoundException ex)
        {
            _lastUpdateError = ex.ToString();
            UpdateStatusText.Text = _testChannel
                ? "No published CouchLink test release is available yet."
                : "No published CouchLink release is available yet.";
            UpdateStageText.Text = "Check complete";
            UpdateNotesText.Visibility = Visibility.Collapsed;
        }
        catch (HttpRequestException ex) when (ex.StatusCode == HttpStatusCode.NotFound)
        {
            _lastUpdateError = ex.ToString();
            UpdateStatusText.Text = _testChannel
                ? "No published CouchLink test release is available yet."
                : "No published CouchLink release is available yet.";
            UpdateStageText.Text = "Check complete";
            UpdateNotesText.Visibility = Visibility.Collapsed;
        }
        catch (HttpRequestException ex)
        {
            _lastUpdateError = ex.ToString();
            UpdateStatusText.Text = "CouchLink could not reach GitHub. Check your internet connection and try again.";
            UpdateStageText.Text = "Failed";
            UpdateNotesText.Visibility = Visibility.Collapsed;
        }
        catch (Exception ex)
        {
            _lastUpdateError = ex.ToString();
            UpdateStatusText.Text = "CouchLink could not check for updates. Copy diagnostics for technical details.";
            UpdateStageText.Text = "Failed";
            UpdateNotesText.Visibility = Visibility.Collapsed;
        }
        finally
        {
            EndOperation();
        }
    }

    private async void InstallUpdate_Click(object sender, RoutedEventArgs e)
    {
        if (_availableUpdate is null || _operationRunning)
        {
            return;
        }

        System.Windows.MessageBoxResult answer = System.Windows.MessageBox.Show(
            this,
            $"Download and install CouchLink {_availableUpdate.Version}?\n\n" +
            "The Host will close briefly and restart after the verified update is installed.",
            "CouchLink update",
            System.Windows.MessageBoxButton.YesNo,
            System.Windows.MessageBoxImage.Information);

        if (answer != System.Windows.MessageBoxResult.Yes)
        {
            return;
        }

        BeginOperation("CANCEL DOWNLOAD");
        InstallUpdateButton.IsEnabled = false;
        UpdateStatusText.Text = "Downloading and verifying the Windows update…";
        UpdateStageText.Text = "Starting secure download";
        UpdateProgressBar.Value = 0;
        UpdateProgressBar.Visibility = Visibility.Visible;
        UpdateStageText.Visibility = Visibility.Visible;

        try
        {
            var progress = new Progress<GitHubUpdateService.UpdateProgress>(value =>
            {
                UpdateStageText.Text = $"{value.Stage} · {value.Percent}%";
                UpdateProgressBar.Value = value.Percent;
            });

            await _updateService.DownloadAndLaunchAsync(
                _availableUpdate,
                progress,
                _operationCts!.Token);

            UpdateStatusText.Text = "Update verified. CouchLink will now close and restart.";
            if (System.Windows.Application.Current is App app)
            {
                app.BeginUpdateShutdown();
            }
            else
            {
                System.Windows.Application.Current.Shutdown();
            }
        }
        catch (OperationCanceledException)
        {
            UpdateStatusText.Text = "Update download cancelled.";
            UpdateStageText.Text = "Cancelled";
            InstallUpdateButton.IsEnabled = _availableUpdate is not null;
        }
        catch (Exception ex)
        {
            _lastUpdateError = ex.ToString();
            UpdateStatusText.Text = "Update installation failed. Copy the test report for technical details.";
            UpdateStageText.Text = "Failed";
            InstallUpdateButton.IsEnabled = true;
        }
        finally
        {
            EndOperation();
        }
    }

    private void BeginOperation(string cancelText)
    {
        _operationCts?.Dispose();
        _operationCts = new CancellationTokenSource();
        _operationRunning = true;
        CheckUpdatesButton.Content = cancelText;
        CheckUpdatesButton.IsEnabled = true;
        UpdateChannelComboBox.IsEnabled = false;
        ReturnStableButton.IsEnabled = false;
    }

    private void EndOperation()
    {
        _operationRunning = false;
        CheckUpdatesButton.Content = "CHECK FOR UPDATES";
        CheckUpdatesButton.IsEnabled = true;
        UpdateChannelComboBox.IsEnabled = true;
        ReturnStableButton.IsEnabled = true;
        _operationCts?.Dispose();
        _operationCts = null;
    }

    private void ReturnStable_Click(object sender, RoutedEventArgs e)
    {
        if (_operationRunning)
        {
            return;
        }

        UpdateChannelComboBox.SelectedIndex = 0;
        UpdateStatusText.Text = "Stable channel restored. Check again to look for the latest stable release.";
        UpdateStageText.Text = "Idle";
        UpdateProgressBar.Value = 0;
    }

    private void CopyTestReport_Click(object sender, RoutedEventArgs e) =>
        CopyTextToClipboard(BuildUpdateReport(), "CouchLink updater test report");

    private string BuildUpdateReport()
    {
        return string.Join(Environment.NewLine, new[]
        {
            "CouchLink Updater Test Report",
            $"Host version: {CouchLink.Host.Core.HostConstants.HostVersion}",
            $"Channel: {(_testChannel ? "Test" : "Stable")}",
            $"Available version: {_availableUpdate?.Version ?? "None"}",
            $"Status: {UpdateStatusText.Text}",
            $"Stage: {UpdateStageText.Text}",
            $"Progress: {(int)UpdateProgressBar.Value}%",
            $"Timestamp: {DateTimeOffset.Now:O}",
            string.IsNullOrWhiteSpace(_lastUpdateError) ? "Error: None" : $"Error: {_lastUpdateError}",
        });
    }

    private static string CleanReleaseNotes(string markdown)
    {
        if (string.IsNullOrWhiteSpace(markdown))
        {
            return string.Empty;
        }

        string text = markdown.Replace("\r", string.Empty);
        text = Regex.Replace(text, @"^#{1,6}\s*", string.Empty, RegexOptions.Multiline);
        text = Regex.Replace(text, @"^\s*[-*+]\s+", "• ", RegexOptions.Multiline);
        text = Regex.Replace(text, @"`([^`]+)`", "$1");
        text = Regex.Replace(text, @"\[([^]]+)\]\([^)]+\)", "$1");
        text = text.Replace("**", string.Empty).Replace("__", string.Empty);
        return text.Trim();
    }

    private void CopyDiagnostics_Click(object sender, RoutedEventArgs e)
    {
        if (DataContext is not MainWindowViewModel viewModel)
        {
            return;
        }

        string diagnostics = viewModel.DiagnosticsText + Environment.NewLine +
            $"Update channel: {(_testChannel ? "Test" : "Stable")}";
        if (!string.IsNullOrWhiteSpace(_lastUpdateError))
        {
            diagnostics += $"{Environment.NewLine}{Environment.NewLine}" +
                "Updater error:" + Environment.NewLine +
                _lastUpdateError;
        }

        CopyTextToClipboard(diagnostics, "CouchLink diagnostics");
    }

    private void CopyTextToClipboard(string text, string description)
    {
        try
        {
            System.Windows.Clipboard.SetText(text);
            System.Windows.MessageBox.Show(
                this,
                $"{description} were copied to the clipboard.",
                "CouchLink",
                System.Windows.MessageBoxButton.OK,
                System.Windows.MessageBoxImage.Information);
        }
        catch (Exception ex)
        {
            _lastUpdateError = ex.ToString();
            System.Windows.MessageBox.Show(
                this,
                "Windows could not access the clipboard. Try again after closing other clipboard tools.",
                "CouchLink",
                System.Windows.MessageBoxButton.OK,
                System.Windows.MessageBoxImage.Warning);
        }
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
        {
            DragMove();
        }
    }

    private void Minimize_Click(object sender, RoutedEventArgs e) =>
        WindowState = WindowState.Minimized;

    private void MaximizeRestore_Click(object sender, RoutedEventArgs e) =>
        ToggleMaximizeRestore();

    private void ToggleMaximizeRestore() =>
        WindowState = WindowState == WindowState.Maximized
            ? WindowState.Normal
            : WindowState.Maximized;

    private void Close_Click(object sender, RoutedEventArgs e)
    {
        _operationCts?.Cancel();
        Close();
    }
}
