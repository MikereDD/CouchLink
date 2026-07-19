using System.Windows;
using System.Windows.Input;
using CouchLink.Host.Wpf.ViewModels;

namespace CouchLink.Host.Wpf.Views;

public partial class MainWindow : Window
{
    private readonly Action _hideToTray;
    private readonly Action _exitApplication;
    private bool _allowClose;

    public MainWindow(MainWindowViewModel viewModel, Action hideToTray, Action exitApplication)
    {
        InitializeComponent();
        DataContext = viewModel;
        _hideToTray = hideToTray;
        _exitApplication = exitApplication;
    }

    private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        if (e.ClickCount == 2)
        {
            WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;
            return;
        }
        DragMove();
    }

    private void Minimize_Click(object sender, RoutedEventArgs e) => _hideToTray();

    private void MaximizeRestore_Click(object sender, RoutedEventArgs e)
        => WindowState = WindowState == WindowState.Maximized ? WindowState.Normal : WindowState.Maximized;

    private void Close_Click(object sender, RoutedEventArgs e) => _exitApplication();

    protected override void OnClosing(System.ComponentModel.CancelEventArgs e)
    {
        if (!_allowClose)
        {
            e.Cancel = true;
            _exitApplication();
            return;
        }
        base.OnClosing(e);
    }

    public void AllowClose() => _allowClose = true;
}
