using System.ServiceProcess;
using System.Text.Json;

namespace CouchLink.SessionHost;

public sealed class BootServiceStatusReader
{
    public const string ServiceName = "CouchLinkBootService";
    public static string StatusPath => Path.Combine(
        Environment.GetFolderPath(Environment.SpecialFolder.CommonApplicationData),
        "CouchLink", "boot-service-status.json");

    public BootServiceStatus Read()
    {
        bool installed = IsServiceInstalled(out string serviceState);
        try
        {
            if (!File.Exists(StatusPath))
                return new(installed, false, serviceState, "Unknown", "Unknown", null,
                    installed ? "Service is installed but has not written a heartbeat yet." : "Boot Service is not installed.");

            BootServiceHeartbeat? heartbeat = JsonSerializer.Deserialize<BootServiceHeartbeat>(File.ReadAllText(StatusPath));
            if (heartbeat is null)
                return new(installed, false, serviceState, "Unknown", "Unknown", null, "Heartbeat file was empty.");

            bool fresh = DateTimeOffset.UtcNow - heartbeat.UpdatedAtUtc < TimeSpan.FromSeconds(12);
            return new(installed, fresh, serviceState, heartbeat.MachineState, heartbeat.Version,
                heartbeat.UpdatedAtUtc, fresh ? heartbeat.Detail : "Boot Service heartbeat is stale.");
        }
        catch (Exception ex)
        {
            return new(installed, false, serviceState, "Unknown", "Unknown", null,
                $"Could not read Boot Service status: {ex.Message}");
        }
    }

    private static bool IsServiceInstalled(out string state)
    {
        try
        {
            using ServiceController? service = ServiceController.GetServices()
                .FirstOrDefault(item => item.ServiceName.Equals(ServiceName, StringComparison.OrdinalIgnoreCase));
            if (service is null)
            {
                state = "Not installed";
                return false;
            }

            state = service.Status.ToString();
            return true;
        }
        catch
        {
            state = "Unknown";
            return false;
        }
    }

    private sealed record BootServiceHeartbeat(
        string Version,
        string MachineState,
        string Detail,
        DateTimeOffset UpdatedAtUtc);
}
