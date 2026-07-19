# CouchLink Protocol v0.1

## Envelope

Every logical message is represented by a JSON envelope during early development:

```json
{
  "protocolVersion": 1,
  "messageId": "uuid",
  "type": "hello",
  "sentAtUtc": "2026-07-18T00:00:00Z",
  "payload": {}
}
```

## Initial message types

- `hello`
- `hello_ack`
- `ping`
- `pong`
- `pair_request`
- `pair_challenge`
- `pair_confirm`
- `pair_result`
- `input_pointer_move`
- `input_pointer_button`
- `input_scroll`
- `input_key`
- `command`
- `command_result`
- `error`

## Framing

For the first TCP prototype:

1. Four-byte unsigned big-endian payload length
2. UTF-8 JSON payload

The maximum accepted frame size must be bounded.

## Rules

- Unknown message types return an error rather than terminating the host.
- Every command capable of changing host state carries a unique message ID.
- Duplicate message IDs must not execute twice.
- Input messages are rejected until pairing and authentication complete.
- Protocol evolution is versioned independently from application versions.
