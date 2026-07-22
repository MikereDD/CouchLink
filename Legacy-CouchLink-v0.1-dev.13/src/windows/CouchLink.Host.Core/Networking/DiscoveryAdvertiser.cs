using System.Net;
using System.Net.Sockets;
using System.Text.Json;
using CouchLink.Protocol;

namespace CouchLink.Host.Core.Networking;

internal sealed class DiscoveryAdvertiser : IAsyncDisposable
{
    private readonly Func<DiscoveryAdvertisement> _advertisementFactory;
    private readonly UdpClient _udpClient;

    public DiscoveryAdvertiser(Func<DiscoveryAdvertisement> advertisementFactory)
    {
        _advertisementFactory = advertisementFactory;
        _udpClient = new UdpClient(AddressFamily.InterNetwork)
        {
            EnableBroadcast = true
        };
    }

    public async Task RunAsync(CancellationToken cancellationToken)
    {
        while (!cancellationToken.IsCancellationRequested)
        {
            DiscoveryAdvertisement advertisement = _advertisementFactory();
            byte[] payload = JsonSerializer.SerializeToUtf8Bytes(advertisement, ProtocolJson.Options);

            var targets = new HashSet<IPAddress> { IPAddress.Broadcast };
            foreach ((_, IPAddress broadcast) in NetworkAddressHelper.GetActiveIpv4Addresses())
            {
                targets.Add(broadcast);
            }

            foreach (IPAddress target in targets)
            {
                try
                {
                    await _udpClient.SendAsync(
                        payload,
                        new IPEndPoint(target, HostConstants.DiscoveryPort),
                        cancellationToken).ConfigureAwait(false);
                }
                catch (SocketException)
                {
                    // An adapter may disappear between enumeration and send.
                }
            }

            await Task.Delay(
                HostConstants.AdvertisementIntervalMilliseconds,
                cancellationToken).ConfigureAwait(false);
        }
    }

    public ValueTask DisposeAsync()
    {
        _udpClient.Dispose();
        return ValueTask.CompletedTask;
    }
}
