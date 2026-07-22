using System.Diagnostics;
using System.Runtime.InteropServices;
using CouchLink.Protocol;
using Microsoft.Win32;

namespace CouchLink.Host.Core;

internal sealed class LauncherController
{
    private const int SwRestore = 9;

    public LauncherResultMessage Execute(
        string launcher,
        string action)
    {
        launcher = launcher.Trim().ToLowerInvariant();
        action = action.Trim().ToLowerInvariant();

        if (string.IsNullOrWhiteSpace(action))
            action = "launch";

        string displayName = DisplayName(launcher);

        try
        {
            if (action == "close")
                return CloseLauncher(launcher, displayName);

            Process? running = FindRunning(launcher);

            if (running is not null)
            {
                bool focused = TryFocus(running);

                if (!focused)
                {
                    string? executable = FindExecutable(launcher);
                    if (!string.IsNullOrWhiteSpace(executable))
                        StartExecutable(executable);
                    else if (launcher == "xbox")
                        StartXbox();
                    else if (launcher == "steam")
                        StartSteamBigPicture();
                }

                return new LauncherResultMessage(
                    launcher,
                    action,
                    true,
                    "focused",
                    focused
                        ? $"{displayName} is already running — focused its window."
                        : $"{displayName} is already running — activation requested.");
            }

            if (launcher == "xbox")
            {
                StartXbox();
                return new LauncherResultMessage(
                    launcher,
                    action,
                    true,
                    "launched",
                    "Xbox app launch requested.");
            }

            string? path = FindExecutable(launcher);
            if (string.IsNullOrWhiteSpace(path))
            {
                return new LauncherResultMessage(
                    launcher,
                    action,
                    false,
                    "unavailable",
                    $"{displayName} is not installed or could not be located.");
            }

            if (launcher == "steam")
                StartSteamBigPicture();
            else
                StartExecutable(path);

            return new LauncherResultMessage(
                launcher,
                action,
                true,
                "launched",
                launcher == "steam"
                    ? "Steam Big Picture launch requested."
                    : $"{displayName} launched.");
        }
        catch (Exception ex) when (
            ex is InvalidOperationException or
            System.ComponentModel.Win32Exception or
            FileNotFoundException or
            DirectoryNotFoundException)
        {
            return new LauncherResultMessage(
                launcher,
                action,
                false,
                "failed",
                $"{displayName} could not be started: {ex.Message}");
        }
    }

    private static LauncherResultMessage CloseLauncher(
        string launcher,
        string displayName)
    {
        Process? running = FindRunning(launcher);

        if (running is null)
        {
            return new LauncherResultMessage(
                launcher,
                "close",
                false,
                "not_running",
                $"{displayName} is not running.");
        }

        bool requested = running.CloseMainWindow();

        return new LauncherResultMessage(
            launcher,
            "close",
            requested,
            requested ? "closing" : "failed",
            requested
                ? $"Close requested for {displayName}."
                : $"{displayName} did not expose a closable window.");
    }

    private static bool TryFocus(Process process)
    {
        process.Refresh();
        IntPtr window = process.MainWindowHandle;

        if (window == IntPtr.Zero)
            return false;

        ShowWindowAsync(window, SwRestore);
        return SetForegroundWindow(window);
    }

    private static void StartExecutable(string executable)
    {
        Process.Start(
            new ProcessStartInfo(executable)
            {
                UseShellExecute = true,
                WorkingDirectory =
                    Path.GetDirectoryName(executable)
                    ?? Environment.CurrentDirectory,
            });
    }

    private static void StartSteamBigPicture()
    {
        Process.Start(
            new ProcessStartInfo(
                "explorer.exe",
                "steam://open/bigpicture")
            {
                UseShellExecute = true,
            });
    }

    private static void StartXbox()
    {
        Process.Start(
            new ProcessStartInfo(
                "explorer.exe",
                "shell:AppsFolder\\Microsoft.GamingApp_8wekyb3d8bbwe!Microsoft.Xbox.App")
            {
                UseShellExecute = true,
            });
    }

    private static Process? FindRunning(string launcher)
    {
        string[] names = launcher switch
        {
            "steam" => ["steam"],
            "gog" => ["GalaxyClient"],
            "xbox" => ["XboxPcApp", "GamingApp"],
            "ea" => ["EADesktop"],
            "ubisoft" => ["upc", "UbisoftConnect"],
            "rockstar" => ["Launcher", "RockstarService"],
            "amazon" => ["Amazon Games UI", "Amazon Games"],
            "epic" => ["EpicGamesLauncher"],
            _ => [],
        };

        foreach (string name in names)
        {
            Process? process =
                Process.GetProcessesByName(name)
                    .FirstOrDefault(candidate =>
                    {
                        try
                        {
                            return !candidate.HasExited;
                        }
                        catch
                        {
                            return false;
                        }
                    });

            if (process is not null)
                return process;
        }

        return null;
    }

    private static string? FindExecutable(string launcher)
    {
        string pf =
            Environment.GetFolderPath(
                Environment.SpecialFolder.ProgramFiles);
        string pfx86 =
            Environment.GetFolderPath(
                Environment.SpecialFolder.ProgramFilesX86);
        string local =
            Environment.GetFolderPath(
                Environment.SpecialFolder.LocalApplicationData);

        string[] candidates = launcher switch
        {
            "steam" =>
            [
                Path.Combine(pfx86, "Steam", "steam.exe"),
                Path.Combine(pf, "Steam", "steam.exe"),
            ],
            "gog" =>
            [
                Path.Combine(pfx86, "GOG Galaxy", "GalaxyClient.exe"),
                Path.Combine(pf, "GOG Galaxy", "GalaxyClient.exe"),
            ],
            "xbox" => [],
            "ea" =>
            [
                Path.Combine(
                    pf,
                    "Electronic Arts",
                    "EA Desktop",
                    "EA Desktop",
                    "EADesktop.exe"),
            ],
            "ubisoft" =>
            [
                Path.Combine(
                    pfx86,
                    "Ubisoft",
                    "Ubisoft Game Launcher",
                    "UbisoftConnect.exe"),
                Path.Combine(
                    pf,
                    "Ubisoft",
                    "Ubisoft Game Launcher",
                    "UbisoftConnect.exe"),
            ],
            "rockstar" =>
            [
                Path.Combine(
                    pf,
                    "Rockstar Games",
                    "Launcher",
                    "Launcher.exe"),
                Path.Combine(
                    pfx86,
                    "Rockstar Games",
                    "Launcher",
                    "Launcher.exe"),
            ],
            "amazon" =>
            [
                Path.Combine(
                    local,
                    "Amazon Games",
                    "App",
                    "Amazon Games.exe"),
                Path.Combine(
                    pf,
                    "Amazon Games",
                    "Amazon Games.exe"),
            ],
            "epic" =>
            [
                Path.Combine(
                    pfx86,
                    "Epic Games",
                    "Launcher",
                    "Portal",
                    "Binaries",
                    "Win64",
                    "EpicGamesLauncher.exe"),
                Path.Combine(
                    pf,
                    "Epic Games",
                    "Launcher",
                    "Portal",
                    "Binaries",
                    "Win64",
                    "EpicGamesLauncher.exe"),
            ],
            _ => [],
        };

        return candidates.FirstOrDefault(File.Exists)
            ?? FindFromUninstallRegistry(launcher);
    }

    private static string? FindFromUninstallRegistry(
        string launcher)
    {
        string needle = launcher switch
        {
            "gog" => "gog galaxy",
            "xbox" => "xbox",
            "ea" => "ea app",
            "ubisoft" => "ubisoft connect",
            "rockstar" => "rockstar games launcher",
            "amazon" => "amazon games",
            "epic" => "epic games launcher",
            "steam" => "steam",
            _ => launcher,
        };

        string[] roots =
        [
            @"SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall",
            @"SOFTWARE\WOW6432Node\Microsoft\Windows\CurrentVersion\Uninstall",
        ];

        foreach (RegistryKey hive in new[]
                 {
                     Registry.LocalMachine,
                     Registry.CurrentUser,
                 })
        {
            foreach (string root in roots)
            {
                using RegistryKey? key =
                    hive.OpenSubKey(root);

                if (key is null)
                    continue;

                foreach (string childName in key.GetSubKeyNames())
                {
                    using RegistryKey? child =
                        key.OpenSubKey(childName);

                    string display =
                        child?.GetValue("DisplayName") as string
                        ?? string.Empty;

                    if (!display.Contains(
                            needle,
                            StringComparison.OrdinalIgnoreCase))
                    {
                        continue;
                    }

                    string icon =
                        (child?.GetValue("DisplayIcon") as string
                         ?? string.Empty)
                        .Trim('"');

                    int comma = icon.IndexOf(',');
                    if (comma >= 0)
                        icon = icon[..comma];

                    if (File.Exists(icon))
                        return icon;
                }
            }
        }

        return null;
    }

    private static string DisplayName(string launcher) =>
        launcher switch
        {
            "gog" => "GOG Galaxy",
            "xbox" => "Xbox",
            "ea" => "EA app",
            "ubisoft" => "Ubisoft Connect",
            "rockstar" => "Rockstar Games Launcher",
            "amazon" => "Amazon Games",
            "epic" => "Epic Games Launcher",
            "steam" => "Steam",
            _ => launcher,
        };

    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool ShowWindowAsync(
        IntPtr hWnd,
        int nCmdShow);
}
