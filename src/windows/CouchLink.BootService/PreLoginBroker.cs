using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Security.Cryptography;
using System.Text.Json;
using CouchLink.Protocol;

namespace CouchLink.BootService;

internal sealed class PreLoginBroker : IAsyncDisposable
{
    private const int DiscoveryPort = 45820;
    private const int PreLoginPort = 45822;
    private readonly ILogger _logger;
    private readonly string _statusDirectory;
    private readonly string _hostId;
    private readonly Func<string> _machineStateProvider;
    private readonly VirtualHidBridge _hid;
    private readonly string _hostName = Environment.MachineName;
    private readonly UdpClient _udp = new(AddressFamily.InterNetwork) { EnableBroadcast = true };
    private readonly TcpListener _listener = new(IPAddress.Any, PreLoginPort);
    private int _connectedClients;

    public PreLoginBroker(ILogger logger, string statusDirectory, string hostId, Func<string> machineStateProvider, VirtualHidBridge hid)
    {
        _logger = logger;
        _statusDirectory = statusDirectory;
        _hostId = hostId;
        _machineStateProvider = machineStateProvider;
        _hid = hid;
    }

    /// <summary>
    /// Pre-login input requires BOTH the machine-level authorization flag AND a
    /// present, running Virtual HID driver. Either missing → status-only.
    /// </summary>
    private bool PreLoginInputReady => ReadPreLoginPermission() && _hid.IsAvailable;

    public string State { get; private set; } = "Starting";
    public string BoundEndpoint { get; private set; } = $"0.0.0.0:{PreLoginPort}";
    public string? LastError { get; private set; }
    public int ConnectedClients => Volatile.Read(ref _connectedClients);

    public async Task RunAsync(CancellationToken cancellationToken)
    {
        try
        {
            _listener.Start();
            State = "Listening";
            LastError = null;
            _logger.LogInformation("Persistent pre-login broker listening on {Endpoint}.", BoundEndpoint);

            Task advertise = AdvertiseAsync(cancellationToken);
            Task accept = AcceptAsync(cancellationToken);
            await Task.WhenAll(advertise, accept).ConfigureAwait(false);
        }
        catch (OperationCanceledException) when (cancellationToken.IsCancellationRequested)
        {
            State = "Stopping";
        }
        catch (Exception ex)
        {
            State = "Faulted";
            LastError = ex.ToString();
            _logger.LogError(ex, "Persistent pre-login broker failed.");
            throw;
        }
    }

    private async Task AdvertiseAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            string machineState = _machineStateProvider();
            if (machineState == "SignInRequired")
            {
                string address = GetPrimaryAddress();
                var advertisement = new DiscoveryAdvertisement(
                    "CouchLink", ProtocolEnvelope.CurrentProtocolVersion, _hostId, _hostName,
                    BootWorker.Version, machineState, address, PreLoginPort,
                    PairingRequired: true, GetPrimaryMacAddress(), DateTimeOffset.UtcNow);
                byte[] payload = JsonSerializer.SerializeToUtf8Bytes(advertisement, ProtocolJson.Options);
                foreach (IPAddress target in GetBroadcastTargets())
                {
                    try
                    {
                        await _udp.SendAsync(payload, new IPEndPoint(target, DiscoveryPort), cancellationToken).ConfigureAwait(false);
                    }
                    catch (SocketException ex)
                    {
                        _logger.LogDebug("Pre-login discovery send failed for {Target}: {Message}", target, ex.Message);
                    }
                }
            }

            await Task.Delay(2000, cancellationToken).ConfigureAwait(false);
        }
    }

    private async Task AcceptAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            TcpClient client = await _listener.AcceptTcpClientAsync(cancellationToken).ConfigureAwait(false);
            _ = HandleClientAsync(client, cancellationToken);
        }
    }

    private async Task HandleClientAsync(TcpClient client, CancellationToken cancellationToken)
    {
        Interlocked.Increment(ref _connectedClients);
        try
        {
            client.NoDelay = true;
            using (client)
            {
                using NetworkStream stream = client.GetStream();
                ProtocolEnvelope? helloEnvelope = await BootFrameCodec.ReadAsync(stream, cancellationToken).ConfigureAwait(false);
                if (helloEnvelope is null || helloEnvelope.Type != "hello") return;

                HelloMessage? hello = helloEnvelope.Payload.Deserialize<HelloMessage>(ProtocolJson.Options);
                if (hello is null) return;

                string machineState = _machineStateProvider();
                bool trusted = IsTrusted(hello.ClientId, hello.PairingToken);
                await BootFrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("hello_ack", new HelloAckMessage(
                    _hostId, _hostName, BootWorker.Version, machineState, PairingRequired: true, Trusted: trusted)), cancellationToken).ConfigureAwait(false);
                if (!trusted) return;

                bool inputReady = PreLoginInputReady;
                string[] capabilities = inputReady
                    ? ["prelogin_status", "machine_state", "wake_on_lan", "prelogin_input", "mouse", "keyboard"]
                    : ["prelogin_status", "machine_state", "wake_on_lan"];
                if (inputReady)
                    _hid.TryOpen(); // ensure the device handle is live for this session

                await BootFrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("session_ready", new SessionReadyMessage(
                    _hostId, _hostName, machineState, 3,
                    capabilities,
                    RemoteInputEnabled: inputReady)), cancellationToken).ConfigureAwait(false);

                while (!cancellationToken.IsCancellationRequested)
                {
                    ProtocolEnvelope? envelope = await BootFrameCodec.ReadAsync(stream, cancellationToken).ConfigureAwait(false);
                    if (envelope is null) break;

                    machineState = _machineStateProvider();
                    if (machineState != "SignInRequired")
                    {
                        await BootFrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("error", new ProtocolError(
                            "desktop_session_available",
                            "The Windows desktop session is available. Reconnect to the CouchLink Session Host.",
                            envelope.MessageId)), cancellationToken).ConfigureAwait(false);
                        break;
                    }

                    if (envelope.Type == "ping")
                    {
                        PingMessage? ping = envelope.Payload.Deserialize<PingMessage>(ProtocolJson.Options);
                        await BootFrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("pong", new PongMessage(
                            ping?.Sequence ?? 0, DateTimeOffset.UtcNow, RemoteInputEnabled: PreLoginInputReady)), cancellationToken).ConfigureAwait(false);
                    }
                    else if (IsInputMessage(envelope.Type))
                    {
                        if (!PreLoginInputReady)
                        {
                            await BootFrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("error", new ProtocolError(
                                "prelogin_input_unavailable",
                                "Pre-login input requires the CouchLink Virtual HID driver to be installed and pre-login control to be authorized.",
                                envelope.MessageId)), cancellationToken).ConfigureAwait(false);
                        }
                        else
                        {
                            DispatchInput(envelope);
                        }
                    }
                    else
                    {
                        await BootFrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("error", new ProtocolError(
                            "unsupported_prelogin_message",
                            "This message is not supported before login.",
                            envelope.MessageId)), cancellationToken).ConfigureAwait(false);
                    }
                }
            }
        }
        catch (Exception ex) when (ex is IOException or SocketException or OperationCanceledException or JsonException)
        {
            _logger.LogDebug("Pre-login client disconnected: {Message}", ex.Message);
        }
        finally
        {
            _hid.ReleaseAll();
            Interlocked.Decrement(ref _connectedClients);
        }
    }

    private static bool IsInputMessage(string type) => type switch
    {
        "mouse_move" or "mouse_button" or "mouse_scroll"
            or "keyboard_text" or "key_press" or "shortcut" => true,
        _ => false
    };

    private void DispatchInput(ProtocolEnvelope envelope)
    {
        try
        {
            switch (envelope.Type)
            {
                case "mouse_move":
                    MouseMoveMessage? move = envelope.Payload.Deserialize<MouseMoveMessage>(ProtocolJson.Options);
                    if (move is not null) _hid.MoveRelative(move.DeltaX, move.DeltaY);
                    break;
                case "mouse_button":
                    MouseButtonMessage? button = envelope.Payload.Deserialize<MouseButtonMessage>(ProtocolJson.Options);
                    if (button is not null) _hid.Button(button.Button, button.Action);
                    break;
                case "mouse_scroll":
                    MouseScrollMessage? scroll = envelope.Payload.Deserialize<MouseScrollMessage>(ProtocolJson.Options);
                    if (scroll is not null) _hid.Scroll(scroll.Delta);
                    break;
                case "keyboard_text":
                    KeyboardTextMessage? text = envelope.Payload.Deserialize<KeyboardTextMessage>(ProtocolJson.Options);
                    if (text is not null) _hid.SendText(text.Text);
                    break;
                case "key_press":
                    KeyPressMessage? key = envelope.Payload.Deserialize<KeyPressMessage>(ProtocolJson.Options);
                    if (key is not null) _hid.PressKey(key.Key);
                    break;
                case "shortcut":
                    ShortcutMessage? shortcut = envelope.Payload.Deserialize<ShortcutMessage>(ProtocolJson.Options);
                    if (shortcut is not null) _hid.PressShortcut(shortcut.Shortcut);
                    break;
            }
        }
        catch (JsonException ex)
        {
            _logger.LogDebug("Discarded malformed pre-login input message: {Message}", ex.Message);
        }
    }

    private bool ReadPreLoginPermission()
    {
        string path = Path.Combine(_statusDirectory, "machine-permissions.json");
        try
        {
            if (!File.Exists(path)) return false;
            using JsonDocument document = JsonDocument.Parse(File.ReadAllText(path));
            JsonElement root = document.RootElement;
            return root.TryGetProperty("RemoteInputEnabled", out JsonElement remote) && remote.GetBoolean()
                && root.TryGetProperty("PreLoginControlEnabled", out JsonElement preLogin) && preLogin.GetBoolean();
        }
        catch (Exception ex)
        {
            _logger.LogDebug("Could not read pre-login permission state: {Message}", ex.Message);
            return false;
        }
    }

    private bool IsTrusted(string clientId, string? token)
    {
        if (string.IsNullOrWhiteSpace(clientId) || string.IsNullOrWhiteSpace(token)) return false;
        string path = Path.Combine(_statusDirectory, "trusted-devices.json");
        try
        {
            if (!File.Exists(path)) return false;
            Dictionary<string, MachineTrustedDevice>? devices = JsonSerializer.Deserialize<Dictionary<string, MachineTrustedDevice>>(File.ReadAllText(path));
            if (devices is null || !devices.TryGetValue(clientId, out MachineTrustedDevice? device)) return false;
            return CryptographicOperations.FixedTimeEquals(Convert.FromBase64String(device.Token), Convert.FromBase64String(token));
        }
        catch
        {
            return false;
        }
    }

    private static IEnumerable<IPAddress> GetBroadcastTargets()
    {
        var targets = new HashSet<IPAddress> { IPAddress.Broadcast };
        foreach (NetworkInterface adapter in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (adapter.OperationalStatus != OperationalStatus.Up || adapter.NetworkInterfaceType is NetworkInterfaceType.Loopback or NetworkInterfaceType.Tunnel) continue;
            foreach (UnicastIPAddressInformation unicast in adapter.GetIPProperties().UnicastAddresses)
            {
                if (unicast.Address.AddressFamily != AddressFamily.InterNetwork || unicast.IPv4Mask is null) continue;
                byte[] address = unicast.Address.GetAddressBytes();
                byte[] mask = unicast.IPv4Mask.GetAddressBytes();
                targets.Add(new IPAddress(address.Zip(mask, (a, m) => (byte)(a | ~m)).ToArray()));
            }
        }
        return targets;
    }

    private static (string Address, string Mac)? GetPrimaryIdentity()
    {
        foreach (NetworkInterface adapter in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (adapter.OperationalStatus != OperationalStatus.Up || adapter.NetworkInterfaceType is NetworkInterfaceType.Loopback or NetworkInterfaceType.Tunnel) continue;
            string mac = string.Join(":", adapter.GetPhysicalAddress().GetAddressBytes().Select(b => b.ToString("X2")));
            foreach (UnicastIPAddressInformation unicast in adapter.GetIPProperties().UnicastAddresses)
            {
                if (unicast.Address.AddressFamily == AddressFamily.InterNetwork && !IPAddress.IsLoopback(unicast.Address))
                    return (unicast.Address.ToString(), mac);
            }
        }
        return null;
    }

    private static string GetPrimaryAddress() => GetPrimaryIdentity()?.Address ?? "Unavailable";
    private static string GetPrimaryMacAddress() => GetPrimaryIdentity()?.Mac ?? string.Empty;

    public ValueTask DisposeAsync()
    {
        State = "Stopped";
        _listener.Stop();
        _udp.Dispose();
        return ValueTask.CompletedTask;
    }

    private sealed record MachineTrustedDevice(string ClientId, string ClientName, string Token, DateTimeOffset PairedAtUtc, DateTimeOffset? LastConnectedAtUtc = null);
}

internal static class BootFrameCodec
{
    public static async Task<ProtocolEnvelope?> ReadAsync(Stream stream, CancellationToken cancellationToken)
    {
        byte[] lengthBytes = new byte[4];
        int first = await stream.ReadAsync(lengthBytes.AsMemory(0, 4), cancellationToken).ConfigureAwait(false);
        if (first == 0) return null;
        await ReadRemainderAsync(stream, lengthBytes, first, 4, cancellationToken).ConfigureAwait(false);
        int length = System.Buffers.Binary.BinaryPrimitives.ReadInt32BigEndian(lengthBytes);
        if (length is < 1 or > 1_048_576) throw new InvalidDataException("Invalid frame length.");
        byte[] payload = new byte[length];
        await ReadRemainderAsync(stream, payload, 0, length, cancellationToken).ConfigureAwait(false);
        return JsonSerializer.Deserialize<ProtocolEnvelope>(payload, ProtocolJson.Options);
    }

    public static async Task WriteAsync(Stream stream, ProtocolEnvelope envelope, CancellationToken cancellationToken)
    {
        byte[] payload = JsonSerializer.SerializeToUtf8Bytes(envelope, ProtocolJson.Options);
        byte[] length = new byte[4];
        System.Buffers.Binary.BinaryPrimitives.WriteInt32BigEndian(length, payload.Length);
        await stream.WriteAsync(length, cancellationToken).ConfigureAwait(false);
        await stream.WriteAsync(payload, cancellationToken).ConfigureAwait(false);
        await stream.FlushAsync(cancellationToken).ConfigureAwait(false);
    }

    private static async Task ReadRemainderAsync(Stream stream, byte[] buffer, int offset, int total, CancellationToken cancellationToken)
    {
        while (offset < total)
        {
            int read = await stream.ReadAsync(buffer.AsMemory(offset, total - offset), cancellationToken).ConfigureAwait(false);
            if (read == 0) throw new EndOfStreamException();
            offset += read;
        }
    }
}
