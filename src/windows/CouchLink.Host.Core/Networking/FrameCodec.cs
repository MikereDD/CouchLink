using System.Buffers.Binary;
using System.Text.Json;
using CouchLink.Protocol;

namespace CouchLink.Host.Core.Networking;

public static class FrameCodec
{
    public const int HeaderSize = sizeof(uint);
    public const int MaxPayloadBytes = 1024 * 1024;

    public static async Task WriteAsync(
        Stream stream,
        ProtocolEnvelope envelope,
        CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(stream);
        ArgumentNullException.ThrowIfNull(envelope);

        byte[] payload = JsonSerializer.SerializeToUtf8Bytes(envelope, ProtocolJson.Options);
        if (payload.Length > MaxPayloadBytes)
        {
            throw new InvalidDataException($"Frame exceeds {MaxPayloadBytes} bytes.");
        }

        byte[] header = new byte[HeaderSize];
        BinaryPrimitives.WriteUInt32BigEndian(header, checked((uint)payload.Length));

        await stream.WriteAsync(header, cancellationToken).ConfigureAwait(false);
        await stream.WriteAsync(payload, cancellationToken).ConfigureAwait(false);
        await stream.FlushAsync(cancellationToken).ConfigureAwait(false);
    }

    public static async Task<ProtocolEnvelope?> ReadAsync(
        Stream stream,
        CancellationToken cancellationToken = default)
    {
        ArgumentNullException.ThrowIfNull(stream);

        byte[] header = new byte[HeaderSize];
        if (!await ReadExactlyOrEndAsync(stream, header, cancellationToken).ConfigureAwait(false))
        {
            return null;
        }

        uint payloadLength = BinaryPrimitives.ReadUInt32BigEndian(header);
        if (payloadLength == 0 || payloadLength > MaxPayloadBytes)
        {
            throw new InvalidDataException($"Invalid frame size: {payloadLength} bytes.");
        }

        byte[] payload = new byte[payloadLength];
        await stream.ReadExactlyAsync(payload, cancellationToken).ConfigureAwait(false);

        return JsonSerializer.Deserialize<ProtocolEnvelope>(payload, ProtocolJson.Options)
            ?? throw new InvalidDataException("Frame contained an empty protocol envelope.");
    }

    private static async Task<bool> ReadExactlyOrEndAsync(
        Stream stream,
        Memory<byte> buffer,
        CancellationToken cancellationToken)
    {
        int totalRead = 0;
        while (totalRead < buffer.Length)
        {
            int read = await stream.ReadAsync(buffer[totalRead..], cancellationToken).ConfigureAwait(false);
            if (read == 0)
            {
                if (totalRead == 0)
                {
                    return false;
                }

                throw new EndOfStreamException("Connection ended inside a frame header.");
            }

            totalRead += read;
        }

        return true;
    }
}
