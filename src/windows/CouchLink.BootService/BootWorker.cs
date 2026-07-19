using System.Runtime.InteropServices;
using System.Security.Cryptography;
using System.Text;
using System.Text.Json;

namespace CouchLink.BootService;

public sealed class BootWorker : BackgroundService
{
    public const string ServiceName = "CouchLinkBootService";
    public const string Version = "0.1-dev.12.1";

    private const uint NoConsoleSession = 0xFFFFFFFF;
    private const int WtsSessionStateLock = 0;
    private const int WtsSessionStateUnlock = 1;

    private readonly ILogger<BootWorker> _logger;

    private readonly string _statusDirectory = Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData),
        "CouchLink");

    private PreLoginBroker? _broker;
    private Task? _brokerTask;
    private VirtualHidBridge? _hid;

    public BootWorker(ILogger<BootWorker> logger)
    {
        _logger = logger;
    }

    protected override async Task ExecuteAsync(CancellationToken stoppingToken)
    {
        Directory.CreateDirectory(_statusDirectory);

        _logger.LogInformation(
            "CouchLink Boot Service {Version} started.",
            Version);

        _hid = new VirtualHidBridge(_logger);

        _broker = new PreLoginBroker(
            _logger,
            _statusDirectory,
            CreateStableHostId(Environment.MachineName),
            DetectMachineState,
            _hid);

        _brokerTask = _broker.RunAsync(stoppingToken);

        while (!stoppingToken.IsCancellationRequested)
        {
            WriteHeartbeat();

            if (_brokerTask.IsFaulted)
            {
                Exception? failure = _brokerTask.Exception?.GetBaseException();

                _logger.LogError(
                    failure,
                    "Persistent pre-login broker task faulted.");
            }

            await Task.Delay(
                TimeSpan.FromSeconds(3),
                stoppingToken).ConfigureAwait(false);
        }
    }

    public override async Task StopAsync(CancellationToken cancellationToken)
    {
        WriteHeartbeat(
            "Stopping",
            "Boot Service is stopping.");

        if (_broker is not null)
        {
            await _broker.DisposeAsync().ConfigureAwait(false);
        }

        _hid?.ReleaseAll();
        _hid?.Dispose();

        try
        {
            if (_brokerTask is not null)
            {
                await _brokerTask
                    .WaitAsync(cancellationToken)
                    .ConfigureAwait(false);
            }
        }
        catch (OperationCanceledException)
        {
            // Normal during service shutdown.
        }
        catch (Exception ex)
        {
            _logger.LogWarning(
                ex,
                "Pre-login broker stopped with an error.");
        }

        await base.StopAsync(cancellationToken).ConfigureAwait(false);
    }

    private void WriteHeartbeat(
        string? forcedState = null,
        string? forcedDetail = null)
    {
        string machineState = forcedState ?? DetectMachineState();

        string detail = forcedDetail ?? machineState switch
        {
            "DesktopAvailable" =>
                "A Windows desktop session is available.",

            "SignInRequired" =>
                "Windows is awaiting sign-in; the CouchLink status broker is available.",

            _ =>
                "Boot Service is active."
        };

        var heartbeat = new
        {
            Version,
            MachineState = machineState,
            Detail = detail,
            PreLoginBroker = _broker?.State ?? "Unavailable",
            BrokerEndpoint = _broker?.BoundEndpoint,
            BrokerClients = _broker?.ConnectedClients ?? 0,
            BrokerLastError = _broker?.LastError,
            PreLoginControlEnabled = ReadPreLoginPermission(),
            UpdatedAtUtc = DateTimeOffset.UtcNow
        };

        string path = Path.Combine(
            _statusDirectory,
            "boot-service-status.json");

        string temporaryPath = path + ".tmp";

        string json = JsonSerializer.Serialize(
            heartbeat,
            new JsonSerializerOptions
            {
                WriteIndented = true
            });

        File.WriteAllText(temporaryPath, json);
        File.Move(temporaryPath, path, true);
    }

    private bool ReadPreLoginPermission()
    {
        string path = Path.Combine(
            _statusDirectory,
            "machine-permissions.json");

        try
        {
            if (!File.Exists(path))
            {
                return false;
            }

            using JsonDocument document =
                JsonDocument.Parse(File.ReadAllText(path));

            JsonElement root = document.RootElement;

            bool remoteInputEnabled =
                root.TryGetProperty(
                    "RemoteInputEnabled",
                    out JsonElement remote) &&
                remote.GetBoolean();

            bool preLoginControlEnabled =
                root.TryGetProperty(
                    "PreLoginControlEnabled",
                    out JsonElement preLogin) &&
                preLogin.GetBoolean();

            return remoteInputEnabled && preLoginControlEnabled;
        }
        catch (Exception ex)
        {
            _logger.LogDebug(
                ex,
                "Could not read pre-login permission state.");

            return false;
        }
    }

    private static string DetectMachineState()
    {
        uint sessionId = WTSGetActiveConsoleSessionId();

        if (sessionId == NoConsoleSession)
        {
            return "Unknown";
        }

        IntPtr buffer = IntPtr.Zero;

        try
        {
            bool success = WTSQuerySessionInformation(
                IntPtr.Zero,
                sessionId,
                WtsInfoClass.SessionInfoEx,
                out buffer,
                out int bytesReturned);

            if (!success ||
                buffer == IntPtr.Zero ||
                bytesReturned < Marshal.SizeOf<WtsInfoEx>())
            {
                return "Unknown";
            }

            WtsInfoEx info = Marshal.PtrToStructure<WtsInfoEx>(buffer);

            if (info.Level != 1)
            {
                return "Unknown";
            }

            return info.Data.SessionFlags switch
            {
                WtsSessionStateLock => "SignInRequired",
                WtsSessionStateUnlock => "DesktopAvailable",
                _ => "Unknown"
            };
        }
        finally
        {
            if (buffer != IntPtr.Zero)
            {
                WTSFreeMemory(buffer);
            }
        }
    }

    private static string CreateStableHostId(string hostName)
    {
        byte[] digest = SHA256.HashData(
            Encoding.UTF8.GetBytes($"CouchLink/dev/{hostName}"));

        return Convert
            .ToHexString(digest[..8])
            .ToLowerInvariant();
    }

    [StructLayout(LayoutKind.Sequential)]
    private struct WtsInfoEx
    {
        public int Level;
        public WtsInfoExLevel1 Data;
    }

    [StructLayout(LayoutKind.Sequential, CharSet = CharSet.Unicode)]
    private struct WtsInfoExLevel1
    {
        public uint SessionId;
        public int SessionState;
        public int SessionFlags;

        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 33)]
        public string WinStationName;

        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 21)]
        public string UserName;

        [MarshalAs(UnmanagedType.ByValTStr, SizeConst = 18)]
        public string DomainName;

        public long LogonTime;
        public long ConnectTime;
        public long DisconnectTime;
        public long LastInputTime;
        public long CurrentTime;

        public uint IncomingBytes;
        public uint OutgoingBytes;
        public uint IncomingFrames;
        public uint OutgoingFrames;
        public uint IncomingCompressedBytes;
        public uint OutgoingCompressedBytes;
    }

    private enum WtsInfoClass
    {
        SessionInfoEx = 25
    }

    [DllImport("kernel32.dll")]
    private static extern uint WTSGetActiveConsoleSessionId();

    [DllImport(
        "wtsapi32.dll",
        SetLastError = true,
        CharSet = CharSet.Unicode)]
    [return: MarshalAs(UnmanagedType.Bool)]
    private static extern bool WTSQuerySessionInformation(
        IntPtr serverHandle,
        uint sessionId,
        WtsInfoClass infoClass,
        out IntPtr buffer,
        out int bytesReturned);

    [DllImport("wtsapi32.dll")]
    private static extern void WTSFreeMemory(IntPtr memory);
}