using System.Net;
using System.Net.Sockets;
using System.Text.Json;
using CouchLink.Protocol;

namespace CouchLink.Host.Core.Networking;

internal sealed record DiscoveryAdvertisementTarget(
    DiscoveryAdvertisement Advertisement,
    IPAddress SourceAddress,
    IPAddress BroadcastAddress);

internal sealed class DiscoveryAdvertiser : IAsyncDisposable
{
    private readonly Func<DiscoveryAdvertisementTarget?> _advertisementFactory;

    public DiscoveryAdvertiser(Func<DiscoveryAdvertisementTarget?> advertisementFactory) =>
        _advertisementFactory = advertisementFactory;

    public async Task RunAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            DiscoveryAdvertisementTarget? advertisement = _advertisementFactory();
            if (advertisement is not null)
            {
                try
                {
                    using var udpClient = new UdpClient(new IPEndPoint(advertisement.SourceAddress, 0))
                    {
                        EnableBroadcast = true
                    };
                    await udpClient.SendAsync(
                        SerializeAdvertisement(advertisement.Advertisement)!,
                        new IPEndPoint(advertisement.BroadcastAddress, HostConstants.DiscoveryPort),
                        cancellationToken).ConfigureAwait(false);
                }
                catch (SocketException)
                {
                    // An adapter may disappear between selection and send.
                }
            }

            await Task.Delay(
                HostConstants.AdvertisementIntervalMilliseconds,
                cancellationToken).ConfigureAwait(false);
        }
    }

    internal static byte[]? SerializeAdvertisement(DiscoveryAdvertisement? advertisement) =>
        advertisement is null ? null : JsonSerializer.SerializeToUtf8Bytes(advertisement, ProtocolJson.Options);

    public ValueTask DisposeAsync() => ValueTask.CompletedTask;
}
