# CouchLink Protocol

## Transport

CouchLink currently uses JSON messages in a framed TCP stream. UDP `45820` is reserved for local host discovery; TCP `45821` serves the signed-in desktop and TCP `45822` serves Boot Service/pre-login sessions.

## Envelope

```json
{
  "protocolVersion": 1,
  "messageType": "hello",
  "messageId": "unique-id",
  "payload": {}
}
```

Every message carries a protocol version, type, correlation identifier, and payload. Unsupported protocol versions receive an explicit error instead of being interpreted optimistically.

## Important message families

| Family | Purpose |
|---|---|
| Discovery | Host identity, address, port, machine state, and pairing requirement |
| Pairing | Approval-code exchange and trusted-device enrollment |
| Session | `hello`, `hello_ack`, heartbeat, reconnect, and state transition |
| Input | Pointer, button, scroll, text, key, and command actions |
| Launcher | Launch-or-focus request and structured `launcher_result` response |
| Error | Machine-readable code, human-readable detail, and correlation ID |

## Compatibility rules

- Protocol version `1` is shared by Android, desktop host, and Boot Service.
- Product version numbers may advance without changing the protocol version.
- New optional fields should remain backward-compatible inside protocol `1`.
- Any incompatible envelope or message-contract change requires a protocol-version increment.
- Unknown or malformed commands must fail closed.

## Security requirements

A trusted session is required before input or launcher commands are accepted. Pre-login sessions never offer pairing and authorize only identities previously mirrored from the signed-in host.
