using System.IO;
using System.Reflection;
using Microsoft.Win32;
using System.Text.Json;

namespace CouchLink.Host.Wpf.Services;

public sealed class HostPreferences
{
    private static readonly string Folder = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.ApplicationData), "CouchLink");
    private static readonly string FilePath = Path.Combine(Folder, "host-settings.json");
    private const string RunKeyPath = @"Software\Microsoft\Windows\CurrentVersion\Run";
    private const string RunValueName = "CouchLink Host";

    public bool StartWithWindows { get; set; }
    public bool StartMinimized { get; set; }
    public bool CloseToTray { get; set; } = false;
    public string? FavoriteHeadphonesEndpointId { get; set; }
    public string? FavoriteHeadphonesName { get; set; }
    public string? FavoriteDisplayEndpointId { get; set; }
    public string? FavoriteDisplayName { get; set; }

    public static HostPreferences Load()
    {
        try
        {
            if (File.Exists(FilePath))
                return JsonSerializer.Deserialize<HostPreferences>(File.ReadAllText(FilePath)) ?? new();
        }
        catch { }
        return new HostPreferences();
    }

    public void Save()
    {
        Directory.CreateDirectory(Folder);
        File.WriteAllText(FilePath, JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = true }));
        ApplyStartupRegistration();
    }

    public void RepairStartupRegistration()
    {
        if (StartWithWindows)
            ApplyStartupRegistration();
    }

    private void ApplyStartupRegistration()
    {
        using RegistryKey key = Registry.CurrentUser.CreateSubKey(RunKeyPath);
        if (StartWithWindows)
        {
            string command = ResolveStartupCommand();
            string args = StartMinimized ? " --minimized" : string.Empty;
            key.SetValue(RunValueName, command + args);
        }
        else key.DeleteValue(RunValueName, false);
    }

    private static string ResolveStartupCommand()
    {
        // Published and normal apphost builds run from CouchLink.Host.exe,
        // including single-file publishes. Prefer the actual process path.
        string? processPath = Environment.ProcessPath;
        if (!string.IsNullOrWhiteSpace(processPath) &&
            !Path.GetFileName(processPath).Equals("dotnet.exe", StringComparison.OrdinalIgnoreCase))
        {
            return $"\"{processPath}\"";
        }

        // During `dotnet run`, Environment.ProcessPath is dotnet.exe. Resolve
        // the apphost or DLL from the application base directory instead of
        // Assembly.Location, which is empty for single-file applications.
        string entryName = Assembly.GetEntryAssembly()?.GetName().Name
            ?? throw new InvalidOperationException("Unable to resolve CouchLink entry assembly name.");

        string appHost = Path.Combine(AppContext.BaseDirectory, entryName + ".exe");
        if (File.Exists(appHost))
            return $"\"{appHost}\"";

        // Framework-dependent fallback for unusual build layouts without apphost.
        string entryAssembly = Path.Combine(AppContext.BaseDirectory, entryName + ".dll");
        string dotnet = string.IsNullOrWhiteSpace(processPath) ? "dotnet" : processPath;
        return $"\"{dotnet}\" \"{entryAssembly}\"";
    }
}
