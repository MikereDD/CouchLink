using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;

namespace CouchLink.Host.Core.Networking;

internal sealed record NetworkInterfaceUnicastAddress(IPAddress Address, IPAddress? IPv4Mask);

internal sealed record NetworkInterfaceInventoryEntry(
    string AdapterId,
    string DisplayName,
    NetworkInterfaceType InterfaceType,
    OperationalStatus OperationalStatus,
    byte[] PhysicalAddress,
    IReadOnlyList<NetworkInterfaceUnicastAddress> UnicastAddresses,
    bool PropertiesReadable = true);

internal sealed record NetworkIdentity(
    string AdapterId,
    string DisplayName,
    NetworkInterfaceType InterfaceType,
    IPAddress Address,
    IPAddress Broadcast,
    string MacAddress);

internal static class NetworkAddressHelper
{
    private static readonly string[] ExcludedNameMarkers =
    ["docker", "vethernet", "hyper-v", "virtual", "vpn", "wireguard", "tap", "tun", "loopback", "tunnel"];

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

    public static IReadOnlyList<NetworkIdentity> GetEligibleNetworkIdentities() =>
        GetEligibleNetworkIdentities(GetInventory());

    internal static IReadOnlyList<NetworkIdentity> GetEligibleNetworkIdentities(
        IEnumerable<NetworkInterfaceInventoryEntry> inventory) =>
        inventory
            .Where(IsEligibleAdapter)
            .Select(CreateIdentity)
            .Where(static identity => identity is not null)
            .Select(static identity => identity!)
            .OrderBy(static identity => AdapterPriority(identity.InterfaceType))
            .ThenBy(static identity => identity.DisplayName, StringComparer.Ordinal)
            .ThenBy(static identity => identity.AdapterId, StringComparer.Ordinal)
            .ToArray();

    internal static NetworkIdentity? SelectDefaultIdentity(IEnumerable<NetworkInterfaceInventoryEntry> inventory) =>
        GetEligibleNetworkIdentities(inventory).FirstOrDefault();

    private static IReadOnlyList<NetworkInterfaceInventoryEntry> GetInventory()
    {
        var inventory = new List<NetworkInterfaceInventoryEntry>();
        foreach (NetworkInterface adapter in NetworkInterface.GetAllNetworkInterfaces())
        {
            try
            {
                IPInterfaceProperties properties = adapter.GetIPProperties();
                inventory.Add(new NetworkInterfaceInventoryEntry(
                    adapter.Id,
                    $"{adapter.Name} {adapter.Description}",
                    adapter.NetworkInterfaceType,
                    adapter.OperationalStatus,
                    adapter.GetPhysicalAddress().GetAddressBytes(),
                    properties.UnicastAddresses
                        .Select(static address => new NetworkInterfaceUnicastAddress(address.Address, address.IPv4Mask))
                        .ToArray()));
            }
            catch (NetworkInformationException)
            {
                inventory.Add(new NetworkInterfaceInventoryEntry(
                    adapter.Id, adapter.Name, adapter.NetworkInterfaceType, adapter.OperationalStatus,
                    [], [], PropertiesReadable: false));
            }
        }

        return inventory;
    }

    private static bool IsEligibleAdapter(NetworkInterfaceInventoryEntry adapter) =>
        adapter.PropertiesReadable &&
        adapter.OperationalStatus == OperationalStatus.Up &&
        (adapter.InterfaceType == NetworkInterfaceType.Ethernet || adapter.InterfaceType == NetworkInterfaceType.Wireless80211) &&
        adapter.PhysicalAddress.Length == 6 &&
        adapter.PhysicalAddress.Any(static value => value != 0) &&
        !ExcludedNameMarkers.Any(marker => adapter.DisplayName.Contains(marker, StringComparison.OrdinalIgnoreCase));

    private static NetworkIdentity? CreateIdentity(NetworkInterfaceInventoryEntry adapter)
    {
        NetworkInterfaceUnicastAddress? address = adapter.UnicastAddresses.FirstOrDefault(IsUsableIpv4);
        if (address?.IPv4Mask is null)
        {
            return null;
        }

        return new NetworkIdentity(
            adapter.AdapterId,
            adapter.DisplayName,
            adapter.InterfaceType,
            address.Address,
            CalculateBroadcast(address.Address, address.IPv4Mask),
            Convert.ToHexString(adapter.PhysicalAddress));
    }

    private static bool IsUsableIpv4(NetworkInterfaceUnicastAddress address)
    {
        if (address.Address.AddressFamily != AddressFamily.InterNetwork ||
            address.IPv4Mask is null ||
            address.IPv4Mask.GetAddressBytes().All(static value => value == 0) ||
            IPAddress.IsLoopback(address.Address) ||
            address.Address.Equals(IPAddress.Any) ||
            address.Address.Equals(IPAddress.Broadcast))
        {
            return false;
        }

        byte firstOctet = address.Address.GetAddressBytes()[0];
        return firstOctet is not (>= 224 and <= 239) &&
               !(firstOctet == 169 && address.Address.GetAddressBytes()[1] == 254);
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
