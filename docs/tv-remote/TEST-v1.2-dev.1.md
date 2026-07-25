# CouchLink TV Remote v1.2-dev.1 — Discovery Test

This first branch build proves that CouchLink can locate and reach the Android TV Remote services on the local network before certificate pairing and command support are added.

## Test setup

1. Connect the phone and Hisense A6H Google TV to the same home network.
2. Turn the TV on and leave it awake.
3. Install the debug APK and open CouchLink.
4. Open the new **TV Remote** tab.
5. Tap **Scan for TVs**.
6. Select the Hisense TV if it appears.
7. Tap **Test connection**.

Expected result:

- the TV appears under **Discovered TVs**, or its IP can be entered manually;
- pairing service TCP port `6467` reports **REACHABLE**;
- remote-control service TCP port `6466` reports **REACHABLE**;
- CouchLink reports that the TV is ready for pairing.

## Report back

Record:

- the discovered TV name;
- the IP address shown;
- whether each port is reachable;
- whether discovery worked without entering the IP manually.

No certificate is generated and no remote command is sent in this build.
