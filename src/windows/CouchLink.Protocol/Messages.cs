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
    string[] Capabilities);
public sealed record PingMessage(long Sequence);
public sealed record PongMessage(long Sequence, DateTimeOffset ReceivedAtUtc);

// Input (keyboard, mouse, shortcuts) is delivered over the phone's Bluetooth HID
// connection, which works at the Windows sign-in screen and never depends on this
// host. The host is a post-login command layer: it only launches, focuses, and
// closes applications, so the wire protocol carries launcher actions, not input.
public sealed record LauncherActionMessage(string Launcher, string Action);
public sealed record LauncherResultMessage(
    string Launcher,
    string Action,
    bool Success,
    string State,
    string Message);

public sealed record ProtocolError(string Code, string Message, Guid? RelatedMessageId = null);

public sealed record AudioOutputListRequest();
public sealed record AudioOutputDeviceMessage(string Id, string Name, bool IsDefault);
public sealed record AudioOutputListMessage(bool Success, AudioOutputDeviceMessage[] Devices, string? Error);
public sealed record AudioOutputSetMessage(string EndpointId);
public sealed record AudioOutputResultMessage(bool Success, string EndpointId, string? Name, string Message);
