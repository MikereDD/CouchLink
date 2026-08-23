using CouchLink.Host.Core;
using CouchLink.Host.Core.Networking;
using System.Net;
using System.Net.NetworkInformation;

namespace CouchLink.Versioning.Tests;

internal static class Program
{
    private sealed record VersionCase(string Left, string Right, int ExpectedSign);

    private static int Main(string[] args)
    {
        string? group = args.SkipWhile(static argument => argument != "--group").Skip(1).FirstOrDefault();
        return group switch
        {
            "default-selection" => RunDefaultSelectionTests(),
            "advertisement-lifecycle" => RunAdvertisementLifecycleTests(),
            "manual-selection" => RunManualSelectionTests(),
            "compatibility" => RunCompatibilityTests(),
            null => RunAllTests(),
            _ => Fail($"Unknown test group: {group}"),
        };
    }

    private static int RunAllTests()
    {
        int failures = RunVersionTests();
        failures += RunDefaultSelectionTests();
        failures += RunAdvertisementLifecycleTests();
        failures += RunManualSelectionTests();
        failures += RunCompatibilityTests();
        return failures == 0 ? 0 : 1;
    }

    private static int RunVersionTests()
    {
        VersionCase[] cases =
        [
            new("1.3.1-dev.8.1", "1.3.1-dev.8", 1),
            new("1.3.1-dev.8", "1.3.1-dev.8.1", -1),
            new("1.3.1-dev.8", "1.3.1-dev.7.1", 1),
            new("1.3.1-dev.7.1", "1.3.1-dev.8", -1),
            new("1.3.1-dev.7.1", "1.3.1-dev.7", 1),
            new("1.3.1-dev.7", "1.3.1-dev.7.1", -1),
            new("1.3.1-dev.7", "1.3.1-dev.6.4", 1),
            new("1.3.1-dev.6.4", "1.3.1-dev.7", -1),
            new("1.3.1-dev.6.4", "1.3.1-dev.6.3", 1),
            new("1.3.1-dev.6.3", "1.3.1-dev.6.4", -1),
            new("1.3.1-dev.6.3", "1.3.1-dev.6.2", 1),
            new("1.3.1-dev.6.2", "1.3.1-dev.6.3", -1),
            new("1.3.1-dev.6.2", "1.3.1-dev.6.1", 1),
            new("1.3.1-dev.6.1", "1.3.1-dev.6.2", -1),
            new("1.3.1-dev.6.1", "1.3.1-dev.6", 1),
            new("1.3.1-dev.6", "1.3.1-dev.6.1", -1),
            new("1.3.1-dev.6", "1.3.1-dev.5", 1),
            new("1.3.1-dev.5", "1.3.1-dev.6", -1),
            new("1.3.1-dev.5", "1.3.1-dev.4", 1),
            new("1.3.1-dev.4", "1.3.1-dev.5", -1),
            new("1.3.1-dev.4", "1.3.1-dev.4", 0),
            new("1.3.1", "1.3.1-dev.4", 1),
            new("1.3.1-rc.1", "1.3.1-beta.9", 1),
            new("1.3.1-beta.1", "1.3.1-alpha.9", 1),
            new("1.3.1-alpha.2", "1.3.1-dev.9", 1),
            new("1.3.1-dev.9", "1.3.1-alpha.2", -1),
            new("1.3.3", "1.3.2", 1),
            new("1.3.2", "1.3.3", -1),
            new("1.3.2", "1.3.1", 1),
            new("1.3.1", "1.3.2", -1),
            new("1.3.2-dev.1", "1.3.1", 1),
            new("v1.3.1-dev.5", "1.3.1-dev.4", 1),
            new("1.3.1-dev.5-debug", "1.3.1-dev.5", 0),
            new("1.3.1-dev.5-release", "1.3.1-dev.5", 0),
            new("1.3.1-preview.2", "1.3.1-rc.9", 1),
            new("1.3.1", "1.3.1.0", 0),
        ];

        int failures = 0;
        foreach (VersionCase test in cases)
        {
            int actual = Math.Sign(CouchLinkVersion.Compare(test.Left, test.Right));
            if (actual == test.ExpectedSign)
            {
                Console.WriteLine($"PASS  {test.Left} ? {test.Right} => {actual}");
                continue;
            }

            failures++;
            Console.Error.WriteLine(
                $"FAIL  {test.Left} ? {test.Right}: expected {test.ExpectedSign}, got {actual}");
        }

        Console.WriteLine();
        Console.WriteLine(failures == 0
            ? $"All {cases.Length} CouchLink version-comparison tests passed."
            : $"{failures} of {cases.Length} CouchLink version-comparison tests failed.");

        return failures;
    }

    private static int RunDefaultSelectionTests()
    {
        NetworkInterfaceInventoryEntry[] inventory =
        [
            Adapter("wifi", "Wi-Fi", NetworkInterfaceType.Wireless80211, "10.0.0.2", "001122334455"),
            Adapter("ethernet", "Ethernet", NetworkInterfaceType.Ethernet, "192.168.1.4", "AABBCCDDEEFF"),
            Adapter("docker", "Docker Desktop", NetworkInterfaceType.Ethernet, "172.17.0.1", "111111111111"),
            Adapter("invalid-mac", "Office", NetworkInterfaceType.Ethernet, "192.168.1.5", "000000000000"),
            Adapter("property-failure", "Office", NetworkInterfaceType.Ethernet, "192.168.1.6", "112233445566", propertiesReadable: false),
            Adapter("unusable-address", "Office", NetworkInterfaceType.Ethernet, "169.254.1.5", "112233445566"),
        ];

        IReadOnlyList<NetworkIdentity> eligible = NetworkAddressHelper.GetEligibleNetworkIdentities(inventory);
        NetworkIdentity? selected = NetworkAddressHelper.SelectDefaultIdentity(inventory);
        int failures = 0;
        failures += Assert(eligible.Select(static identity => identity.AdapterId).SequenceEqual(["ethernet", "wifi"]), "Only eligible physical LAN identities are retained in deterministic order.");
        failures += Assert(selected is { AdapterId: "ethernet", Address: var address, MacAddress: "AABBCCDDEEFF" } && address.Equals(IPAddress.Parse("192.168.1.4")), "Ethernet identity is selected atomically with its address and MAC.");

        NetworkInterfaceInventoryEntry[] ties =
        [
            Adapter("b", "Same", NetworkInterfaceType.Ethernet, "192.168.1.8", "010203040506"),
            Adapter("a", "Same", NetworkInterfaceType.Ethernet, "192.168.1.9", "060504030201"),
            Adapter("z", "alpha", NetworkInterfaceType.Ethernet, "192.168.1.10", "0A0B0C0D0E0F"),
        ];
        failures += Assert(NetworkAddressHelper.GetEligibleNetworkIdentities(ties).Select(static identity => identity.AdapterId).SequenceEqual(["a", "b", "z"]), "Ordinal display-name then opaque-ID ordering is applied.");

        NetworkIdentity? reportedOrder = NetworkAddressHelper.SelectDefaultIdentity(
        [
            new NetworkInterfaceInventoryEntry("ordered", "Ordered", NetworkInterfaceType.Ethernet, OperationalStatus.Up, Convert.FromHexString("001122334455"),
            [
                new NetworkInterfaceUnicastAddress(IPAddress.Parse("127.0.0.1"), IPAddress.Parse("255.0.0.0")),
                new NetworkInterfaceUnicastAddress(IPAddress.Parse("192.168.1.11"), IPAddress.Parse("255.255.255.0")),
                new NetworkInterfaceUnicastAddress(IPAddress.Parse("10.0.0.11"), IPAddress.Parse("255.0.0.0")),
            ])
        ]);
        failures += Assert(reportedOrder?.Address.Equals(IPAddress.Parse("192.168.1.11")) == true, "The first usable Windows-reported IPv4 address is preserved.");
        return failures;
    }

    private static int RunAdvertisementLifecycleTests()
    {
        var selection = new SessionInterfaceSelection();
        NetworkInterfaceInventoryEntry selected = Adapter("selected", "Ethernet", NetworkInterfaceType.Ethernet, "192.168.1.2", "001122334455");
        NetworkInterfaceInventoryEntry alternative = Adapter("alternative", "Wi-Fi", NetworkInterfaceType.Wireless80211, "10.0.0.2", "AABBCCDDEEFF");
        selection.Start([selected, alternative]);
        int failures = Assert(selection.CurrentIdentity?.AdapterId == "selected", "A new session selects one automatic identity.");
        failures += Assert(
            selection.Refresh([alternative])?.AdapterId == "alternative" &&
            !selection.RequiresManualReselection &&
            selection.Snapshot.SelectionMode == HostNetworkSelectionMode.Automatic,
            "Selection loss automatically falls back to the first remaining eligible identity.");
        failures += Assert(DiscoveryAdvertiser.SerializeAdvertisement(null) is null, "No advertisement is serialized when selection is unavailable.");

        var delayed = new SessionInterfaceSelection();
        delayed.Start(Array.Empty<NetworkInterfaceInventoryEntry>());
        failures += Assert(delayed.Snapshot.Availability == HostNetworkSelectionAvailability.NotStarted,
            "A session with no interface starts in a waiting state.");
        failures += Assert(delayed.Refresh([selected])?.AdapterId == "selected" &&
            delayed.Snapshot.SelectionMode == HostNetworkSelectionMode.Automatic,
            "A delayed interface becomes the automatic selection when it first appears.");

        IReadOnlyList<NetworkIdentity> current = [
            NetworkAddressHelper.SelectDefaultIdentity([selected])!
        ];
        var runtime = new CouchLinkHostRuntime(() => current);
        var snapshots = new List<HostSnapshot>();
        runtime.SnapshotChanged += (_, snapshot) => snapshots.Add(snapshot);
        runtime.StartAsync().GetAwaiter().GetResult();
        snapshots.Clear();
        current = [];
        failures += Assert(runtime.CreateAdvertisement() is null && snapshots.Count == 1 &&
            snapshots[0].NetworkSelection.Availability == HostNetworkSelectionAvailability.NotStarted &&
            snapshots[0].NetworkSelection.SelectedAddress is null,
            "Interface loss with no eligible fallback stops advertising and publishes a waiting snapshot.");
        runtime.StopAsync().GetAwaiter().GetResult();
        current = [NetworkAddressHelper.SelectDefaultIdentity([selected])!];
        failures += Assert(runtime.CreateAdvertisement() is null &&
            runtime.Snapshot.NetworkSelection.Availability == HostNetworkSelectionAvailability.NotStarted &&
            runtime.Snapshot.NetworkSelection.Candidates.Count == 0,
            "A stopped runtime does not restore network selection during an advertising refresh.");

        IReadOnlyList<NetworkIdentity> candidates =
        [
            NetworkAddressHelper.SelectDefaultIdentity([selected])!,
            NetworkAddressHelper.SelectDefaultIdentity([alternative])!,
        ];
        var candidateRuntime = new CouchLinkHostRuntime(() => candidates);
        var candidateSnapshots = new List<HostSnapshot>();
        candidateRuntime.SnapshotChanged += (_, snapshot) => candidateSnapshots.Add(snapshot);
        candidateRuntime.StartAsync().GetAwaiter().GetResult();
        candidateSnapshots.Clear();
        candidates = [NetworkAddressHelper.SelectDefaultIdentity([selected])!];
        DiscoveryAdvertisementTarget? continuedAdvertisement = candidateRuntime.CreateAdvertisement();
        failures += Assert(candidateSnapshots.Count == 1 &&
            candidateSnapshots[0].NetworkSelection.Availability == HostNetworkSelectionAvailability.Available &&
            candidateSnapshots[0].NetworkSelection.SelectedCandidateId == "selected" &&
            candidateSnapshots[0].NetworkSelection.Candidates.Select(static candidate => candidate.Id).SequenceEqual(["selected"]) &&
            continuedAdvertisement is not null,
            "Removing an unselected candidate publishes its removal while the selected interface keeps advertising.");
        candidates =
        [
            NetworkAddressHelper.SelectDefaultIdentity([selected])!,
            NetworkAddressHelper.SelectDefaultIdentity([alternative])!,
        ];
        candidateSnapshots.Clear();
        DiscoveryAdvertisementTarget? addedAdvertisement = candidateRuntime.CreateAdvertisement();
        failures += Assert(candidateSnapshots.Count == 1 &&
            candidateSnapshots[0].NetworkSelection.SelectedCandidateId == "selected" &&
            candidateSnapshots[0].NetworkSelection.Candidates.Select(static candidate => candidate.Id).SequenceEqual(["selected", "alternative"]) &&
            addedAdvertisement is not null,
            "Adding an eligible candidate leaves the selected identity unchanged and publishes the new candidate.");
        candidateRuntime.StopAsync().GetAwaiter().GetResult();

        IReadOnlyList<NetworkIdentity> unavailableCandidates = [NetworkAddressHelper.SelectDefaultIdentity([selected])!];
        var unavailableRuntime = new CouchLinkHostRuntime(() => unavailableCandidates);
        var unavailableSnapshots = new List<HostSnapshot>();
        unavailableRuntime.SnapshotChanged += (_, snapshot) => unavailableSnapshots.Add(snapshot);
        unavailableRuntime.StartAsync().GetAwaiter().GetResult();
        unavailableSnapshots.Clear();
        unavailableCandidates = [];
        unavailableRuntime.CreateAdvertisement();
        unavailableSnapshots.Clear();
        unavailableCandidates =
        [
            NetworkAddressHelper.SelectDefaultIdentity([selected])!,
            NetworkAddressHelper.SelectDefaultIdentity([alternative])!,
        ];
        DiscoveryAdvertisementTarget? unavailableAdvertisement = unavailableRuntime.CreateAdvertisement();
        failures += Assert(unavailableSnapshots.Count == 1 &&
            unavailableSnapshots[0].NetworkSelection.Availability == HostNetworkSelectionAvailability.Available &&
            unavailableSnapshots[0].NetworkSelection.SelectionMode == HostNetworkSelectionMode.Automatic &&
            unavailableSnapshots[0].NetworkSelection.SelectedCandidateId == "selected" &&
            unavailableSnapshots[0].NetworkSelection.Candidates.Select(static candidate => candidate.Id).SequenceEqual(["selected", "alternative"]) &&
            unavailableAdvertisement is not null,
            "Returning candidates automatically restore selection and advertising.");
        unavailableRuntime.StopAsync().GetAwaiter().GetResult();
        return failures;
    }

    private static int RunManualSelectionTests()
    {
        var selection = new SessionInterfaceSelection();
        NetworkInterfaceInventoryEntry ethernet = Adapter("ethernet", "Ethernet", NetworkInterfaceType.Ethernet, "192.168.1.2", "001122334455");
        NetworkInterfaceInventoryEntry wifi = Adapter("wifi", "Wi-Fi", NetworkInterfaceType.Wireless80211, "10.0.0.2", "AABBCCDDEEFF");

        selection.Start([ethernet, wifi]);
        HostNetworkSelectionSnapshot automatic = selection.Snapshot;
        int failures = 0;
        failures += Assert(
            automatic.Availability == HostNetworkSelectionAvailability.Available &&
            automatic.SelectionMode == HostNetworkSelectionMode.Automatic &&
            automatic.SelectedCandidateId == "ethernet" &&
            automatic.SelectedAddress == "192.168.1.2" &&
            automatic.Candidates.Select(static candidate => candidate.Id).SequenceEqual(["ethernet", "wifi"]),
            "A new session exposes the automatic eligible identity and candidates.");
        failures += Assert(selection.Select("wifi", [ethernet, wifi]), "Selecting an eligible opaque ID succeeds.");
        HostNetworkSelectionSnapshot manual = selection.Snapshot;
        failures += Assert(
            manual.SelectionMode == HostNetworkSelectionMode.Manual &&
            manual.SelectedCandidateId == "wifi" &&
            manual.SelectedAddress == "10.0.0.2" &&
            selection.CurrentIdentity?.MacAddress == "AABBCCDDEEFF",
            "Manual selection atomically changes the address and MAC identity.");
        failures += Assert(!selection.Select("missing", [ethernet, wifi]) &&
            selection.Snapshot.SelectedCandidateId == manual.SelectedCandidateId &&
            selection.Snapshot.SelectionMode == manual.SelectionMode &&
            selection.CurrentIdentity?.MacAddress == "AABBCCDDEEFF",
            "An absent opaque ID is rejected without changing the selection.");
        selection.Refresh([ethernet]);
        HostNetworkSelectionSnapshot fallback = selection.Snapshot;
        failures += Assert(
            fallback.Availability == HostNetworkSelectionAvailability.Available &&
            fallback.SelectedCandidateId == "ethernet" && fallback.SelectedAddress == "192.168.1.2" &&
            fallback.SelectionMode == HostNetworkSelectionMode.Automatic &&
            fallback.Candidates.Select(static candidate => candidate.Id).SequenceEqual(["ethernet"]),
            "Loss of a manually selected interface automatically falls back to an eligible alternative.");
        failures += Assert(selection.Select("ethernet", [ethernet]) && selection.Snapshot.SelectionMode == HostNetworkSelectionMode.Manual,
            "Explicit selection can return an automatically selected fallback to manual mode.");
        selection.Stop();
        failures += Assert(selection.Snapshot.Availability == HostNetworkSelectionAvailability.NotStarted &&
            selection.Snapshot.SelectedAddress is null && selection.Snapshot.Candidates.Count == 0,
            "Stopping clears the current-session selection.");
        return failures;
    }

    private static int RunCompatibilityTests()
    {
        int failures = 0;
        failures += Assert(HostConstants.DiscoveryPort == 45820 && HostConstants.SessionPort == 45821, "Discovery and session ports remain protocol compatible.");
        NetworkIdentity identity = NetworkAddressHelper.SelectDefaultIdentity([Adapter("selected", "Ethernet", NetworkInterfaceType.Ethernet, "192.168.1.2", "001122334455")])!;
        failures += Assert(identity.Broadcast.Equals(IPAddress.Parse("192.168.1.255")), "Selected identity retains its interface broadcast address.");
        return failures;
    }

    private static NetworkInterfaceInventoryEntry Adapter(string id, string name, NetworkInterfaceType type, string address, string mac, bool propertiesReadable = true) =>
        new(id, name, type, OperationalStatus.Up, Convert.FromHexString(mac),
            [new NetworkInterfaceUnicastAddress(IPAddress.Parse(address), IPAddress.Parse("255.255.255.0"))], propertiesReadable);

    private static int Assert(bool condition, string description)
    {
        Console.WriteLine($"{(condition ? "PASS" : "FAIL")}  {description}");
        return condition ? 0 : 1;
    }

    private static int Fail(string message)
    {
        Console.Error.WriteLine(message);
        return 1;
    }
}
