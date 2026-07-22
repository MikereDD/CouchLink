using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace CouchLink.Host.Core.Networking;

internal static class NetworkAddressHelper
{
    public static IReadOnlyList<(IPAddress Address, IPAddress Broadcast)> GetActiveIpv4Addresses()
    {
        var results = new List<(IPAddress, IPAddress)>();

        foreach (NetworkInterface adapter in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (adapter.OperationalStatus != OperationalStatus.Up ||
                adapter.NetworkInterfaceType is NetworkInterfaceType.Loopback or NetworkInterfaceType.Tunnel)
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

                results.Add((unicast.Address, CalculateBroadcast(unicast.Address, unicast.IPv4Mask)));
            }
        }

        return results;
    }


    public static (IPAddress Address, IPAddress Broadcast, string MacAddress)? GetPrimaryNetworkIdentity()
    {
        foreach (NetworkInterface adapter in NetworkInterface.GetAllNetworkInterfaces())
        {
            if (adapter.OperationalStatus != OperationalStatus.Up ||
                adapter.NetworkInterfaceType is NetworkInterfaceType.Loopback or NetworkInterfaceType.Tunnel)
                continue;

            IPInterfaceProperties properties;
            try { properties = adapter.GetIPProperties(); }
            catch (NetworkInformationException) { continue; }

            foreach (UnicastIPAddressInformation unicast in properties.UnicastAddresses)
            {
                if (unicast.Address.AddressFamily != AddressFamily.InterNetwork ||
                    IPAddress.IsLoopback(unicast.Address) || unicast.IPv4Mask is null)
                    continue;

                string mac = string.Concat(adapter.GetPhysicalAddress().GetAddressBytes().Select(b => b.ToString("X2")));
                if (mac.Length == 12)
                    return (unicast.Address, CalculateBroadcast(unicast.Address, unicast.IPv4Mask), mac);
            }
        }
        return null;
    }

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
