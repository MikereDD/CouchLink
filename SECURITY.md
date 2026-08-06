# Security Policy

## Supported version

Security fixes are applied to the latest stable CouchLink release and, when practical, the current development branch.

| Version | Supported |
|---|---|
| 1.1 | Yes |
| 1.0 | Critical fixes only |
| Older development builds | No |

## Reporting a vulnerability

Do not publish an unpatched vulnerability in a public issue. Report it privately to the project maintainer through the private contact method provided by the official repository host.

Include:

- the affected CouchLink component and version;
- clear reproduction steps;
- expected and observed behavior;
- the security impact;
- relevant logs or screenshots with secrets removed; and
- any proposed mitigation.

## Security boundaries

- CouchLink does not provide internet-facing or cloud-relay functionality.
- The Windows host is intended for trusted local networks.
- Wake-on-LAN magic packets are unauthenticated LAN broadcasts by design; CouchLink exposes Wake PC only for a Host with an existing trusted pairing token and stores its wake identity only after a trusted session succeeds.
- Pairing tokens and signing keys must never be committed to source control.
- Android release keystores and passwords must remain private.
- The Windows host does not inject keyboard or mouse input and does not bypass Windows authentication.
