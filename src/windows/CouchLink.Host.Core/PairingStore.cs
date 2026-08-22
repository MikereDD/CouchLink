using System.Security.Cryptography;
using System.Text.Json;

namespace CouchLink.Host.Core;

internal sealed class PairingStore
{
    private readonly object _sync = new();
    private readonly string _path;
    private readonly string _machinePath;
    private readonly Dictionary<string, TrustedDevice> _devices;

    internal PairingStore(bool inMemory)
    {
        _path = string.Empty;
        _machinePath = string.Empty;
        _devices = new(StringComparer.Ordinal);
    }

    public PairingStore()
    {
        string directory = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),
            "CouchLink");
        Directory.CreateDirectory(directory);
        _path = Path.Combine(directory, "trusted-devices.json");
        string machineDirectory = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData), "CouchLink");
        Directory.CreateDirectory(machineDirectory);
        _machinePath = Path.Combine(machineDirectory, "trusted-devices.json");
        _devices = Load();
        SaveMachineCopy();
    }

    public int Count
    {
        get { lock (_sync) return _devices.Count; }
    }

    public IReadOnlyList<TrustedDeviceInfo> GetDevices()
    {
        lock (_sync)
        {
            return _devices.Values
                .OrderBy(device => device.ClientName, StringComparer.OrdinalIgnoreCase)
                .Select(device => new TrustedDeviceInfo(device.ClientId, device.ClientName, device.PairedAtUtc, device.LastConnectedAtUtc))
                .ToArray();
        }
    }

    public bool IsTrusted(string clientId, string? token)
    {
        if (string.IsNullOrWhiteSpace(clientId) || string.IsNullOrWhiteSpace(token)) return false;
        try
        {
            lock (_sync)
            {
                return _devices.TryGetValue(clientId, out TrustedDevice? device)
                    && CryptographicOperations.FixedTimeEquals(
                        Convert.FromBase64String(device.Token),
                        Convert.FromBase64String(token));
            }
        }
        catch (FormatException)
        {
            return false;
        }
    }

    public string Trust(string clientId, string clientName)
    {
        byte[] bytes = RandomNumberGenerator.GetBytes(32);
        string token = Convert.ToBase64String(bytes);
        DateTimeOffset now = DateTimeOffset.UtcNow;
        lock (_sync)
        {
            _devices[clientId] = new TrustedDevice(clientId, clientName, token, now, now);
            Save();
        }
        return token;
    }

    public void MarkConnected(string clientId)
    {
        lock (_sync)
        {
            if (!_devices.TryGetValue(clientId, out TrustedDevice? device)) return;
            _devices[clientId] = device with { LastConnectedAtUtc = DateTimeOffset.UtcNow };
            Save();
        }
    }

    public bool Revoke(string clientId)
    {
        lock (_sync)
        {
            bool removed = _devices.Remove(clientId);
            if (removed) Save();
            return removed;
        }
    }

    public int RevokeAll()
    {
        lock (_sync)
        {
            int count = _devices.Count;
            _devices.Clear();
            Save();
            return count;
        }
    }

    private Dictionary<string, TrustedDevice> Load()
    {
        try
        {
            if (!File.Exists(_path)) return new(StringComparer.Ordinal);
            Dictionary<string, TrustedDevice>? loaded = JsonSerializer.Deserialize<Dictionary<string, TrustedDevice>>(File.ReadAllText(_path));
            return loaded is null ? new(StringComparer.Ordinal) : new(loaded, StringComparer.Ordinal);
        }
        catch
        {
            return new(StringComparer.Ordinal);
        }
    }

    private void Save()
    {
        string temp = _path + ".tmp";
        File.WriteAllText(temp, JsonSerializer.Serialize(_devices, new JsonSerializerOptions { WriteIndented = true }));
        File.Move(temp, _path, true);
        SaveMachineCopy();
    }

    private void SaveMachineCopy()
    {
        try
        {
            string temp = _machinePath + ".tmp";
            File.WriteAllText(temp, JsonSerializer.Serialize(_devices, new JsonSerializerOptions { WriteIndented = true }));
            File.Move(temp, _machinePath, true);
        }
        catch
        {
            // The desktop host remains usable even if the machine trust mirror cannot be written.
        }
    }

    internal sealed record TrustedDevice(
        string ClientId,
        string ClientName,
        string Token,
        DateTimeOffset PairedAtUtc,
        DateTimeOffset? LastConnectedAtUtc = null);
}

public sealed record TrustedDeviceInfo(
    string ClientId,
    string ClientName,
    DateTimeOffset PairedAtUtc,
    DateTimeOffset? LastConnectedAtUtc);
