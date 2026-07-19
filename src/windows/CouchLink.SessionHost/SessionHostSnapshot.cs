using CouchLink.Host.Core;

namespace CouchLink.SessionHost;

public sealed record SessionHostSnapshot(
    HostSnapshot Host,
    BootServiceStatus BootService,
    string SessionState,
    DateTimeOffset UpdatedAtUtc);
