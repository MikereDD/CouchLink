using System.Text.Json;

namespace CouchLink.Host.Core;

internal sealed class MachinePermissionStore
{
    private readonly string _path;
    private readonly object _sync = new();

    public MachinePermissionStore()
    {
        string directory = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData),
            "CouchLink");
        Directory.CreateDirectory(directory);
        _path = Path.Combine(directory, "machine-permissions.json");
    }

    public bool ReadRemoteInputEnabled()
    {
        lock (_sync)
        {
            try
            {
                if (!File.Exists(_path)) return false;
                MachinePermissions? state = JsonSerializer.Deserialize<MachinePermissions>(File.ReadAllText(_path));
                return state?.RemoteInputEnabled == true && state.PreLoginControlEnabled;
            }
            catch
            {
                return false;
            }
        }
    }

    public void WriteRemoteInputEnabled(bool enabled)
    {
        lock (_sync)
        {
            var state = new MachinePermissions(
                RemoteInputEnabled: enabled,
                PreLoginControlEnabled: enabled,
                UpdatedAtUtc: DateTimeOffset.UtcNow);
            string temporary = _path + ".tmp";
            File.WriteAllText(temporary, JsonSerializer.Serialize(state, new JsonSerializerOptions { WriteIndented = true }));
            File.Move(temporary, _path, true);
        }
    }

    private sealed record MachinePermissions(
        bool RemoteInputEnabled,
        bool PreLoginControlEnabled,
        DateTimeOffset UpdatedAtUtc);
}
