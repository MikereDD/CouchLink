using System.Windows;
using CouchLink.Host.Wpf.Services;
using CouchLink.Host.Wpf.ViewModels;
using CouchLink.Host.Wpf.Views;

namespace CouchLink.Host.Wpf;

public partial class App : System.Windows.Application
{
    private readonly MainWindowViewModel _viewModel = new();
    private readonly HostPreferences _preferences = HostPreferences.Load();
    private TrayIconService? _tray;
    private MainWindow? _window;
    private bool _exiting;

    protected override async void OnStartup(StartupEventArgs e)
    {
        base.OnStartup(e);

        // Repair stale Run entries created while developing through `dotnet run`.
        // This also updates the entry automatically after moving to a new build.
        _preferences.RepairStartupRegistration();

        _tray = new TrayIconService(ShowDashboard, _viewModel.ToggleRemoteInput,
            () => _viewModel.RemoteInputEnabled, ExitApplication);

        try
        {
            await _viewModel.StartAsync();
        }
        catch (Exception ex)
        {
            System.Windows.MessageBox.Show(
                $"CouchLink could not start its discovery service.\n\n{ex.Message}",
                "CouchLink Host", MessageBoxButton.OK, MessageBoxImage.Error);
            ExitApplication();
            return;
        }

        bool minimizedArgument = e.Args.Any(a =>
            a.Equals("--minimized", StringComparison.OrdinalIgnoreCase));
        bool startHeadless = minimizedArgument || _preferences.StartMinimized;

        if (startHeadless)
        {
            // Deliberately do not construct a WPF Window here. Creating custom
            // chrome and hiding it after startup can leave a black compositor
            // surface on the desktop.
            _tray.ShowStartedBalloon(true);
        }
        else
        {
            ShowDashboard();
        }
    }

    private void ShowDashboard()
    {
        Dispatcher.Invoke(() =>
        {
            if (_window is null)
            {
                _window = new MainWindow(_viewModel, HideDashboard, ExitApplication);
                _window.Closed += (_, _) => _window = null;
            }

            _window.ShowInTaskbar = true;
            _window.Show();
            if (_window.WindowState == WindowState.Minimized)
                _window.WindowState = WindowState.Normal;
            _window.Activate();
        });
    }

    private void HideDashboard()
    {
        Dispatcher.Invoke(() =>
        {
            if (_window is null) return;
            _window.Hide();
            _window.ShowInTaskbar = false;
        });
    }

    private async void ExitApplication()
    {
        if (_exiting) return;
        _exiting = true;

        _tray?.Dispose();
        _tray = null;

        if (_window is not null)
        {
            _window.AllowClose();
            _window.Close();
            _window = null;
        }

        await _viewModel.DisposeAsync();
        Shutdown();
    }
}
