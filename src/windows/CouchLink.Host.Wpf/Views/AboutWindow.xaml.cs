using System.Windows;
using CouchLink.Host.Wpf.ViewModels;
using CouchLink.Host.Wpf.Services;

namespace CouchLink.Host.Wpf.Views;

public partial class AboutWindow : Window
{
    private readonly GitHubUpdateService _updateService = new();
    private GitHubUpdateService.UpdateInfo? _availableUpdate;
    public AboutWindow()
    {
        InitializeComponent();
    }



    private async void CheckUpdates_Click(object sender, RoutedEventArgs e)
    {
        CheckUpdatesButton.IsEnabled = false;
        InstallUpdateButton.IsEnabled = false;
        UpdateStatusText.Text = "Checking GitHub Releases…";
        try
        {
            _availableUpdate = await _updateService.CheckAsync();
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
        catch (Exception ex)
        {
            UpdateStatusText.Text = $"Update check failed: {ex.Message}";
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

        System.Windows.Clipboard.SetText(viewModel.DiagnosticsText);
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
