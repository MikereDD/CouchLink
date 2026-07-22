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
        // Environment.ProcessPath points to dotnet.exe when CouchLink is launched
        // through `dotnet run`. Register the generated apphost beside the entry
        // assembly instead so Windows can launch CouchLink directly at sign-in.
        string entryAssembly = Assembly.GetEntryAssembly()?.Location
            ?? throw new InvalidOperationException("Unable to resolve CouchLink entry assembly.");

        string appHost = Path.ChangeExtension(entryAssembly, ".exe");
        if (File.Exists(appHost))
            return $"\"{appHost}\"";

        string? processPath = Environment.ProcessPath;
        if (!string.IsNullOrWhiteSpace(processPath) &&
            !Path.GetFileName(processPath).Equals("dotnet.exe", StringComparison.OrdinalIgnoreCase))
        {
            return $"\"{processPath}\"";
        }

        // Framework-dependent fallback for unusual build layouts without apphost.
        string dotnet = string.IsNullOrWhiteSpace(processPath) ? "dotnet" : processPath;
        return $"\"{dotnet}\" \"{entryAssembly}\"";
    }
}
