namespace CouchLink.Protocol;

public sealed record DiscoveryAdvertisement(
    string Product,
    int ProtocolVersion,
    string HostId,
    string HostName,
    string HostVersion,
    string HostState,
    string Address,
    int SessionPort,
    bool PairingRequired,
    string MacAddress,
    DateTimeOffset SentAtUtc);

public sealed record HelloMessage(
    string ClientId,
    string ClientName,
    string ClientPlatform,
    string ClientVersion,
    string? PairingToken = null);

public sealed record HelloAckMessage(
    string HostId,
    string HostName,
    string HostVersion,
    string HostState,
    bool PairingRequired,
    bool Trusted);

public sealed record PairRequestMessage(string ClientId, string ClientName, string PairingCode);
public sealed record PairResultMessage(bool Success, string? PairingToken, string Message);
public sealed record SessionReadyMessage(
    string HostId,
    string HostName,
    string HostState,
    int HeartbeatSeconds,
    string[] Capabilities,
    bool RemoteInputEnabled);
public sealed record PingMessage(long Sequence);
public sealed record PongMessage(long Sequence, DateTimeOffset ReceivedAtUtc, bool RemoteInputEnabled);

public sealed record MouseMoveMessage(int DeltaX, int DeltaY);
public sealed record MouseButtonMessage(string Button, string Action);
public sealed record MouseScrollMessage(int Delta);
public sealed record KeyboardTextMessage(string Text);
public sealed record KeyPressMessage(string Key);
public sealed record ShortcutMessage(string Shortcut);
public sealed record LauncherActionMessage(string Launcher, string Action);

public sealed record ProtocolError(string Code, string Message, Guid? RelatedMessageId = null);
