using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace CouchLink.Host.Core.Networking;

internal static class NetworkAddressHelper
{
    public static IReadOnlyList<(IPAddress Address, IPAddress Broadcast)> GetActiveIpv4Addresses()
    {
        var results = new List<(IPAddress, IPAddress)>();

        foreach (NetworkInterface adapter in GetCandidateAdapters())
        {
            IPInterfaceProperties properties;
            try
            {
                properties = adapter.GetIPProperties();
            }
            catch (NetworkInformationException)
            {
                continue;
            }

            foreach (UnicastIPAddressInformation unicast in properties.UnicastAddresses)
            {
                if (unicast.Address.AddressFamily != AddressFamily.InterNetwork ||
                    IPAddress.IsLoopback(unicast.Address) ||
                    unicast.IPv4Mask is null)
                {
                    continue;
                }

                results.Add((unicast.Address, CalculateBroadcast(unicast.Address, unicast.IPv4Mask)));
            }
        }

        return results;
    }

    public static (IPAddress Address, IPAddress Broadcast, string MacAddress)? GetPrimaryNetworkIdentity()
    {
        foreach (NetworkInterface adapter in GetCandidateAdapters())
        {
            byte[] physicalAddress = adapter.GetPhysicalAddress().GetAddressBytes();
            if (physicalAddress.Length != 6 || physicalAddress.All(static value => value == 0))
            {
                continue;
            }

            IPInterfaceProperties properties;
            try
            {
                properties = adapter.GetIPProperties();
            }
            catch (NetworkInformationException)
            {
                continue;
            }

            foreach (UnicastIPAddressInformation unicast in properties.UnicastAddresses)
            {
                if (unicast.Address.AddressFamily != AddressFamily.InterNetwork ||
                    IPAddress.IsLoopback(unicast.Address) ||
                    unicast.IPv4Mask is null)
                {
                    continue;
                }

                string mac = string.Concat(physicalAddress.Select(static value => value.ToString("X2")));
                return (unicast.Address, CalculateBroadcast(unicast.Address, unicast.IPv4Mask), mac);
            }
        }

        return null;
    }

    private static IEnumerable<NetworkInterface> GetCandidateAdapters() =>
        NetworkInterface.GetAllNetworkInterfaces()
            .Where(static adapter =>
                adapter.OperationalStatus == OperationalStatus.Up &&
                adapter.NetworkInterfaceType != NetworkInterfaceType.Loopback &&
                adapter.NetworkInterfaceType != NetworkInterfaceType.Tunnel)
            .OrderBy(static adapter => AdapterPriority(adapter.NetworkInterfaceType))
            .ThenBy(static adapter => adapter.Name, StringComparer.OrdinalIgnoreCase);

    private static int AdapterPriority(NetworkInterfaceType type) => type switch
    {
        NetworkInterfaceType.Ethernet => 0,
        NetworkInterfaceType.Wireless80211 => 1,
        _ => 2,
    };

    private static IPAddress CalculateBroadcast(IPAddress address, IPAddress mask)
    {
        byte[] addressBytes = address.GetAddressBytes();
        byte[] maskBytes = mask.GetAddressBytes();
        byte[] broadcast = new byte[addressBytes.Length];

        for (int i = 0; i < broadcast.Length; i++)
        {
            broadcast[i] = (byte)(addressBytes[i] | ~maskBytes[i]);
        }

        return new IPAddress(broadcast);
    }
}
