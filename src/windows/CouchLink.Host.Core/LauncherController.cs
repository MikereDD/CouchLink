using System.Diagnostics;
using Microsoft.Win32;

namespace CouchLink.Host.Core;

internal sealed class LauncherController
{
    public string Execute(string launcher, string action)
    {
        launcher = launcher.Trim().ToLowerInvariant();
        action = action.Trim().ToLowerInvariant();
        if (launcher == "steam")
        {
            if (action is "bigpicture" or "launch")
            {
                Process.Start(new ProcessStartInfo("explorer.exe", "steam://open/bigpicture") { UseShellExecute = true });
                return "Steam Big Picture requested.";
            }
        }

        if (launcher == "xbox" && action is ("launch" or "focus"))
        {
            Process.Start(new ProcessStartInfo(
                "explorer.exe",
                "shell:AppsFolder\\Microsoft.GamingApp_8wekyb3d8bbwe!Microsoft.Xbox.App")
            {
                UseShellExecute = true
            });
            return "Xbox app requested.";
        }

        string? executable = FindExecutable(launcher);
        if (string.IsNullOrWhiteSpace(executable)) return $"{DisplayName(launcher)} was not found.";

        Process? running = FindRunning(launcher);
        if (action == "close")
        {
            if (running is null) return $"{DisplayName(launcher)} is not running.";
            running.CloseMainWindow();
            return $"Close requested for {DisplayName(launcher)}.";
        }

        if (running is not null)
        {
            Process.Start(new ProcessStartInfo("cmd.exe", $"/c start \"\" \"{executable}\"") { CreateNoWindow = true, UseShellExecute = false });
            return $"{DisplayName(launcher)} brought forward.";
        }

        Process.Start(new ProcessStartInfo(executable) { UseShellExecute = true, WorkingDirectory = Path.GetDirectoryName(executable)! });
        return $"{DisplayName(launcher)} launched.";
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
            "amazon" => ["Amazon Games UI"],
            "epic" => ["EpicGamesLauncher"],
            _ => [],
        };
        foreach (string name in names)
        {
            Process? process = Process.GetProcessesByName(name).FirstOrDefault();
            if (process is not null) return process;
        }
        return null;
    }

    private static string? FindExecutable(string launcher)
    {
        string pf = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFiles);
        string pfx86 = Environment.GetFolderPath(Environment.SpecialFolder.ProgramFilesX86);
        string local = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        string[] candidates = launcher switch
        {
            "steam" => [Path.Combine(pfx86, "Steam", "steam.exe"), Path.Combine(pf, "Steam", "steam.exe")],
            "gog" => [Path.Combine(pfx86, "GOG Galaxy", "GalaxyClient.exe"), Path.Combine(pf, "GOG Galaxy", "GalaxyClient.exe")],
            "xbox" => [],
            "ea" => [Path.Combine(pf, "Electronic Arts", "EA Desktop", "EA Desktop", "EADesktop.exe")],
            "ubisoft" => [Path.Combine(pfx86, "Ubisoft", "Ubisoft Game Launcher", "UbisoftConnect.exe"), Path.Combine(pf, "Ubisoft", "Ubisoft Game Launcher", "UbisoftConnect.exe")],
            "rockstar" => [Path.Combine(pf, "Rockstar Games", "Launcher", "Launcher.exe"), Path.Combine(pfx86, "Rockstar Games", "Launcher", "Launcher.exe")],
            "amazon" => [Path.Combine(local, "Amazon Games", "App", "Amazon Games.exe"), Path.Combine(pf, "Amazon Games", "Amazon Games.exe")],
            "epic" => [Path.Combine(pfx86, "Epic Games", "Launcher", "Portal", "Binaries", "Win64", "EpicGamesLauncher.exe"), Path.Combine(pf, "Epic Games", "Launcher", "Portal", "Binaries", "Win64", "EpicGamesLauncher.exe")],
            _ => [],
        };
        return candidates.FirstOrDefault(File.Exists) ?? FindFromUninstallRegistry(launcher);
    }

    private static string? FindFromUninstallRegistry(string launcher)
    {
        string needle = launcher switch
        {
            "gog" => "gog galaxy", "xbox" => "xbox", "ea" => "ea app", "ubisoft" => "ubisoft connect",
            "rockstar" => "rockstar games launcher", "amazon" => "amazon games",
            "epic" => "epic games launcher", "steam" => "steam", _ => launcher,
        };
        string[] roots = [
            @"SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
            @"SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall"
        ];
        foreach (RegistryKey hive in new[] { Registry.LocalMachine, Registry.CurrentUser })
        foreach (string root in roots)
        using (RegistryKey? key = hive.OpenSubKey(root))
        {
            if (key is null) continue;
            foreach (string childName in key.GetSubKeyNames())
            using (RegistryKey? child = key.OpenSubKey(childName))
            {
                string display = child?.GetValue("DisplayName") as string ?? string.Empty;
                if (!display.Contains(needle, StringComparison.OrdinalIgnoreCase)) continue;
                string icon = (child?.GetValue("DisplayIcon") as string ?? string.Empty).Trim('"');
                int comma = icon.IndexOf(',');
                if (comma >= 0) icon = icon[..comma];
                if (File.Exists(icon)) return icon;
            }
        }
        return null;
    }

    private static string DisplayName(string launcher) => launcher switch
    {
        "gog" => "GOG Galaxy", "xbox" => "Xbox", "ea" => "EA app", "ubisoft" => "Ubisoft Connect",
        "rockstar" => "Rockstar Games Launcher", "amazon" => "Amazon Games",
        "epic" => "Epic Games Launcher", "steam" => "Steam", _ => launcher,
    };
}
