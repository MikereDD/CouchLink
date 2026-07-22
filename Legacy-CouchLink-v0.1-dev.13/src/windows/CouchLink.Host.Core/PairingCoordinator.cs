using System.Collections.Concurrent;
using System.Security.Cryptography;

namespace CouchLink.Host.Core;

internal sealed class PairingCoordinator
{
    private readonly ConcurrentDictionary<string, PendingPairing> _pending = new(StringComparer.Ordinal);

    public string CreateOrRefresh(string clientId, string clientName)
    {
        string code = RandomNumberGenerator.GetInt32(0, 1_000_000).ToString("D6");
        _pending[clientId] = new PendingPairing(clientName, code, DateTimeOffset.UtcNow.AddMinutes(5));
        return code;
    }

    public void Clear() => _pending.Clear();

    public bool Validate(string clientId, string code, out string clientName)
    {
        clientName = string.Empty;
        if (!_pending.TryGetValue(clientId, out PendingPairing? pending)) return false;
        if (pending.ExpiresAtUtc < DateTimeOffset.UtcNow)
        {
            _pending.TryRemove(clientId, out _);
            return false;
        }
        if (!string.Equals(pending.Code, code, StringComparison.Ordinal)) return false;
        clientName = pending.ClientName;
        _pending.TryRemove(clientId, out _);
        return true;
    }

    private sealed record PendingPairing(string ClientName, string Code, DateTimeOffset ExpiresAtUtc);
}
