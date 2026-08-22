using System.Net;
using System.Net.Sockets;
using System.Text.Json;
using System.Collections.Concurrent;
using CouchLink.Protocol;

namespace CouchLink.Host.Core.Networking;

internal sealed class SessionServer : IAsyncDisposable
{
    private readonly TcpListener _listener = new(IPAddress.Any, HostConstants.SessionPort);
    private readonly string _hostId;
    private readonly string _hostName;
    private readonly Func<string> _hostState;
    private readonly Action<string> _eventSink;
    private readonly PairingStore _pairingStore;
    private readonly PairingCoordinator _pairingCoordinator;
    private readonly LauncherController _launcherController = new();
    private readonly AudioOutputController _audioOutputController = new();
    private readonly Action<string> _pairingCodeSink;
    private readonly Action<string> _connectedDeviceSink;
    private int _connectedClients;
    private readonly ConcurrentDictionary<string, TcpClient> _authenticatedClients = new(StringComparer.Ordinal);

    public SessionServer(
        string hostId,
        string hostName,
        Func<string> hostState,
        Action<string> eventSink,
        PairingStore pairingStore,
        PairingCoordinator pairingCoordinator,
        Action<string> pairingCodeSink,
        Action<string> connectedDeviceSink)
    {
        _hostId = hostId;
        _hostName = hostName;
        _hostState = hostState;
        _eventSink = eventSink;
        _pairingStore = pairingStore;
        _pairingCoordinator = pairingCoordinator;
        _pairingCodeSink = pairingCodeSink;
        _connectedDeviceSink = connectedDeviceSink;
    }

    public int ConnectedClients => Volatile.Read(ref _connectedClients);

    public async Task RunAsync(CancellationToken cancellationToken)
    {
        _listener.Start();
        try
        {
            while (!cancellationToken.IsCancellationRequested)
            {
                TcpClient client = await _listener.AcceptTcpClientAsync(cancellationToken).ConfigureAwait(false);
                _ = HandleClientAsync(client, cancellationToken);
            }
        }
        finally { _listener.Stop(); }
    }

    private async Task HandleClientAsync(TcpClient client, CancellationToken hostCancellationToken)
    {
        string endpoint = client.Client.RemoteEndPoint?.ToString() ?? "unknown client";
        var state = new ClientSessionState();
        Interlocked.Increment(ref _connectedClients);
        _eventSink($"Connection opened from {endpoint}");

        try
        {
            client.NoDelay = true;
            using (client)
            using (NetworkStream stream = client.GetStream())
            using (var idleTimeout = CancellationTokenSource.CreateLinkedTokenSource(hostCancellationToken))
            {
                idleTimeout.CancelAfter(TimeSpan.FromSeconds(HostConstants.ClientTimeoutSeconds));
                while (!idleTimeout.IsCancellationRequested)
                {
                    ProtocolEnvelope? envelope = await FrameCodec.ReadAsync(stream, idleTimeout.Token).ConfigureAwait(false);
                    if (envelope is null) break;
                    idleTimeout.CancelAfter(TimeSpan.FromSeconds(HostConstants.ClientTimeoutSeconds));
                    await ProcessMessageAsync(stream, envelope, state, idleTimeout.Token).ConfigureAwait(false);
                    if (state.Authenticated && !string.IsNullOrWhiteSpace(state.ClientId))
                        _authenticatedClients[state.ClientId] = client;
                }
            }
        }
        catch (OperationCanceledException) when (hostCancellationToken.IsCancellationRequested) { }
        catch (OperationCanceledException) { _eventSink($"Connection timed out: {endpoint}"); }
        catch (Exception ex) when (ex is IOException or SocketException or JsonException or InvalidDataException or FormatException)
        { _eventSink($"Connection ended: {endpoint} — {ex.Message}"); }
        finally
        {
            Interlocked.Decrement(ref _connectedClients);
            if (!string.IsNullOrWhiteSpace(state.ClientId)) _authenticatedClients.TryRemove(state.ClientId, out _);
            if (state.Authenticated) _connectedDeviceSink("None");
            _eventSink($"Connection closed: {endpoint}");
        }
    }

    public bool DisconnectClient(string clientId)
    {
        if (!_authenticatedClients.TryRemove(clientId, out TcpClient? client)) return false;
        try { client.Close(); }
        catch { }
        return true;
    }

    public void DisconnectAllClients()
    {
        foreach ((string clientId, TcpClient client) in _authenticatedClients.ToArray())
        {
            _authenticatedClients.TryRemove(clientId, out _);
            try { client.Close(); }
            catch { }
        }
    }

    private async Task ProcessMessageAsync(Stream stream, ProtocolEnvelope envelope, ClientSessionState state, CancellationToken cancellationToken)
    {
        if (envelope.ProtocolVersion != ProtocolEnvelope.CurrentProtocolVersion)
        {
            await SendErrorAsync(stream, "protocol_version_mismatch", $"Host supports protocol {ProtocolEnvelope.CurrentProtocolVersion}.", envelope.MessageId, cancellationToken).ConfigureAwait(false);
            return;
        }

        switch (envelope.Type)
        {
            case "hello":
                {
                    HelloMessage? hello = envelope.Payload.Deserialize<HelloMessage>(ProtocolJson.Options);
                    if (hello is null) { await SendErrorAsync(stream, "invalid_hello", "Hello payload is missing.", envelope.MessageId, cancellationToken); return; }
                    bool trusted = _pairingStore.IsTrusted(hello.ClientId, hello.PairingToken);
                    state.Authenticated = trusted;
                    state.ClientName = hello.ClientName;
                    state.ClientId = hello.ClientId;
                    if (!trusted)
                    {
                        string code = _pairingCoordinator.CreateOrRefresh(hello.ClientId, hello.ClientName);
                        _pairingCodeSink(code);
                        _eventSink($"Pairing requested by {hello.ClientName}. Code {code}");
                    }
                    else
                    {
                        _pairingCodeSink("------");
                        _pairingStore.MarkConnected(hello.ClientId);
                        _connectedDeviceSink(hello.ClientName);
                        _eventSink($"Persistent trusted session opened for {hello.ClientName}");
                    }
                    await FrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("hello_ack",
                        new HelloAckMessage(_hostId, _hostName, HostConstants.HostVersion, _hostState(), !trusted, trusted)), cancellationToken);
                    if (trusted)
                    {
                        string[] capabilities = ["heartbeat", "session_state", "launcher_actions", "audio_outputs"];
                        await FrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("session_ready",
                            new SessionReadyMessage(_hostId, _hostName, _hostState(), HostConstants.HeartbeatSeconds, capabilities)), cancellationToken);
                    }
                    return;
                }

            case "pair_request":
                {
                    PairRequestMessage? request = envelope.Payload.Deserialize<PairRequestMessage>(ProtocolJson.Options);
                    if (request is null) { await SendErrorAsync(stream, "invalid_pair_request", "Pair request payload is missing.", envelope.MessageId, cancellationToken); return; }
                    if (!_pairingCoordinator.Validate(request.ClientId, request.PairingCode, out string expectedName))
                    {
                        await FrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("pair_result", new PairResultMessage(false, null, "The pairing code is invalid or expired.")), cancellationToken);
                        _eventSink($"Pairing failed for {request.ClientName}");
                        return;
                    }
                    string token = _pairingStore.Trust(request.ClientId, string.IsNullOrWhiteSpace(expectedName) ? request.ClientName : expectedName);
                    _pairingCodeSink("------");
                    _eventSink($"Trusted device paired: {request.ClientName}");
                    await FrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("pair_result", new PairResultMessage(true, token, "Device trusted.")), cancellationToken);
                    return;
                }

            case "ping":
                {
                    if (!state.Authenticated) return;
                    PingMessage? ping = envelope.Payload.Deserialize<PingMessage>(ProtocolJson.Options);
                    if (ping is null) { await SendErrorAsync(stream, "invalid_ping", "Ping payload is missing.", envelope.MessageId, cancellationToken); return; }
                    await FrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("pong", new PongMessage(ping.Sequence, DateTimeOffset.UtcNow)), cancellationToken);
                    return;
                }

            case "audio_output_list":
                {
                    if (!state.Authenticated) return;
                    AudioOutputListResult result = _audioOutputController.List();
                    var devices = result.Devices
                        .Select(d => new AudioOutputDeviceMessage(d.Id, d.Name, d.IsDefault))
                        .ToArray();
                    await FrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("audio_output_list_result",
                        new AudioOutputListMessage(result.Success, devices, result.Error)), cancellationToken).ConfigureAwait(false);
                    return;
                }

            case "audio_output_set":
                {
                    if (!state.Authenticated) return;
                    AudioOutputSetMessage? request = envelope.Payload.Deserialize<AudioOutputSetMessage>(ProtocolJson.Options);
                    if (request is null || string.IsNullOrWhiteSpace(request.EndpointId))
                    {
                        await SendErrorAsync(stream, "invalid_audio_output", "Audio endpoint ID is missing.", envelope.MessageId, cancellationToken).ConfigureAwait(false);
                        return;
                    }
                    AudioOutputSetResult result = _audioOutputController.SetDefault(request.EndpointId);
                    _eventSink(result.Message);
                    await FrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("audio_output_result",
                        new AudioOutputResultMessage(result.Success, result.EndpointId, result.Name, result.Message)), cancellationToken).ConfigureAwait(false);
                    return;
                }

            case "launcher_action":
                {
                    if (!state.Authenticated) return;
                    LauncherActionMessage? launcher =
                        envelope.Payload.Deserialize<LauncherActionMessage>(ProtocolJson.Options);

                    if (launcher is null)
                    {
                        await SendErrorAsync(
                            stream,
                            "invalid_launcher_action",
                            "Launcher action payload is missing.",
                            envelope.MessageId,
                            cancellationToken).ConfigureAwait(false);
                        return;
                    }

                    LauncherResultMessage result =
                        _launcherController.Execute(launcher.Launcher, launcher.Action);

                    _eventSink(result.Message);

                    await FrameCodec.WriteAsync(
                        stream,
                        ProtocolEnvelope.Create("launcher_result", result),
                        cancellationToken).ConfigureAwait(false);
                    return;
                }

            default:
                await SendErrorAsync(stream, "unsupported_message", $"Message type '{envelope.Type}' is not supported by the launcher host.", envelope.MessageId, cancellationToken);
                return;
        }
    }

    private static Task SendErrorAsync(Stream stream, string code, string message, Guid relatedMessageId, CancellationToken cancellationToken) =>
        FrameCodec.WriteAsync(stream, ProtocolEnvelope.Create("error", new ProtocolError(code, message, relatedMessageId)), cancellationToken);

    public ValueTask DisposeAsync() { _listener.Stop(); return ValueTask.CompletedTask; }

    private sealed class ClientSessionState
    {
        public bool Authenticated { get; set; }
        public string ClientId { get; set; } = string.Empty;
        public string ClientName { get; set; } = string.Empty;
    }
}
