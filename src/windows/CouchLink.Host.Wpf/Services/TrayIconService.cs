using System.Diagnostics;
using System.Drawing;
using System.Windows;
using Forms = System.Windows.Forms;

namespace CouchLink.Host.Wpf.Services;

public sealed class TrayIconService : IDisposable
{
    private readonly Forms.NotifyIcon _notifyIcon;
    private readonly Action _showWindow;
    private readonly Action _exitApplication;

    public TrayIconService(Action showWindow, Action exitApplication)
    {
        _showWindow = showWindow;
        _exitApplication = exitApplication;
        var menu = new Forms.ContextMenuStrip();
        menu.Items.Add("Open CouchLink", null, (_, _) => _showWindow());
        menu.Items.Add("Launch Steam Big Picture", null, (_, _) => LaunchSteam());
        menu.Items.Add(new Forms.ToolStripSeparator());
        menu.Items.Add("Exit CouchLink", null, (_, _) => System.Windows.Application.Current.Dispatcher.Invoke(_exitApplication));

        _notifyIcon = new Forms.NotifyIcon
        {
            Text = "CouchLink Host",
            Icon = LoadIcon(),
            Visible = true,
            ContextMenuStrip = menu
        };
        _notifyIcon.DoubleClick += (_, _) => _showWindow();
    }

    public void ShowStartedBalloon(bool minimized)
    {
        if (!minimized) return;
        _notifyIcon.BalloonTipTitle = "CouchLink Host";
        _notifyIcon.BalloonTipText = "CouchLink is running in the system tray.";
        _notifyIcon.ShowBalloonTip(2500);
    }

    private static Icon LoadIcon()
    {
        var resource = System.Windows.Application.GetResourceStream(new Uri("pack://application:,,,/Assets/couchlink.ico"));
        return resource is null ? SystemIcons.Application : new Icon(resource.Stream);
    }

    private static void LaunchSteam()
    {
        try { Process.Start(new ProcessStartInfo("steam://open/bigpicture") { UseShellExecute = true }); }
        catch { }
    }

    public void Dispose()
    {
        _notifyIcon.Visible = false;
        _notifyIcon.Dispose();
    }
}
