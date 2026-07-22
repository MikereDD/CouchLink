using System.Text.Json;
using System.Text.Json.Serialization;

namespace CouchLink.Protocol;

public sealed record ProtocolEnvelope(
    int ProtocolVersion,
    Guid MessageId,
    string Type,
    DateTimeOffset SentAtUtc,
    JsonElement Payload)
{
    public const int CurrentProtocolVersion = 1;

    public static ProtocolEnvelope Create<TPayload>(string type, TPayload payload)
    {
        ArgumentException.ThrowIfNullOrWhiteSpace(type);

        return new ProtocolEnvelope(
            CurrentProtocolVersion,
            Guid.NewGuid(),
            type,
            DateTimeOffset.UtcNow,
            JsonSerializer.SerializeToElement(payload, ProtocolJson.Options));
    }
}

public static class ProtocolJson
{
    public static JsonSerializerOptions Options { get; } = new(JsonSerializerDefaults.Web)
    {
        PropertyNamingPolicy = JsonNamingPolicy.CamelCase,
        DefaultIgnoreCondition = JsonIgnoreCondition.WhenWritingNull,
        WriteIndented = false
    };
}
