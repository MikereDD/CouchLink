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
    private static readonly string BackupPath = Path.Combine(Folder, "host-settings.backup.json");
    private static readonly string CorruptPath = Path.Combine(Folder, "host-settings.corrupt.json");
    private static readonly string LogPath = Path.Combine(Folder, "host-settings.log");
    private const string RunKeyPath = @"Software\Microsoft\Windows\CurrentVersion\Run";
    private const string RunValueName = "CouchLink Host";

    public bool StartWithWindows { get; set; }
    public bool StartMinimized { get; set; }
    public bool CloseToTray { get; set; } = false;
    public string? SelectedNetworkInterfaceId { get; set; }
    public string? FavoriteHeadphonesEndpointId { get; set; }
    public string? FavoriteHeadphonesName { get; set; }
    public string? FavoriteDisplayEndpointId { get; set; }
    public string? FavoriteDisplayName { get; set; }

    public static HostPreferences Load()
    {
        try
        {
            if (File.Exists(FilePath))
            {
                HostPreferences? loaded = JsonSerializer.Deserialize<HostPreferences>(File.ReadAllText(FilePath));
                if (loaded is not null)
                    return loaded;

                LogFailure("Preferences file deserialized to null; preserving it and using defaults.", null);
                PreserveCorruptFile();
            }
        }
        catch (Exception ex)
        {
            // Never silently reset settings: keep the unreadable file for inspection.
            LogFailure("Failed to load preferences; preserving the file and using defaults.", ex);
            PreserveCorruptFile();
        }
        return new HostPreferences();
    }

    public void Save()
    {
        try
        {
            Directory.CreateDirectory(Folder);
            string json = JsonSerializer.Serialize(this, new JsonSerializerOptions { WriteIndented = true });

            // Write to a sibling temp file first, then swap it into place so a crash
            // or power loss mid-write can never truncate the live settings file.
            string tempPath = Path.Combine(Folder, $"host-settings.{Guid.NewGuid():N}.tmp");
            File.WriteAllText(tempPath, json);

            if (File.Exists(FilePath))
                File.Replace(tempPath, FilePath, BackupPath, ignoreMetadataErrors: true);
            else
                File.Move(tempPath, FilePath);
        }
        catch (Exception ex)
        {
            LogFailure("Failed to save preferences.", ex);
        }

        try
        {
            ApplyStartupRegistration();
        }
        catch (Exception ex)
        {
            LogFailure("Failed to update startup registration.", ex);
        }
    }

    private static void PreserveCorruptFile()
    {
        try
        {
            if (!File.Exists(FilePath))
                return;
            if (File.Exists(CorruptPath))
                File.Delete(CorruptPath);
            File.Move(FilePath, CorruptPath);
        }
        catch (Exception ex)
        {
            LogFailure("Failed to preserve corrupt preferences file.", ex);
        }
    }

    private static void LogFailure(string message, Exception? ex)
    {
        try
        {
            Directory.CreateDirectory(Folder);
            string detail = ex is null ? string.Empty : $" :: {ex.GetType().Name}: {ex.Message}";
            File.AppendAllText(LogPath, $"{DateTimeOffset.Now:O}  {message}{detail}{Environment.NewLine}");
        }
        catch
        {
            // Logging must never take down preference handling.
        }
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
