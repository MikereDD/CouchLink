namespace CouchLink.SessionHost;

public sealed record BootServiceStatus(
    bool IsInstalled,
    bool IsReachable,
    string ServiceState,
    string MachineState,
    string Version,
    DateTimeOffset? UpdatedAtUtc,
    string Detail)
{
    public static BootServiceStatus Unavailable(string detail = "Boot Service status is unavailable.") =>
        new(false, false, "Unavailable", "Unknown", "Unknown", null, detail);
}
