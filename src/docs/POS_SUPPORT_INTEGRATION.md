# POS ↔ Control Tower support integration

## Required production configuration

POS Backend:

```dotenv
CT_BASE_URL=https://<control-tower-api>
CT_ENDPOINT_ID=<integration-endpoint-uuid>
CT_API_KEY=<integration-api-key>
POS_WEBHOOK_CALLBACK_URL=https://<pos-api>/support/webhooks/ct
POS_WEBHOOK_SECRET=<shared-random-secret>
```

Control Tower Backend:

```dotenv
POS_WEBHOOK_SECRET=<same-shared-random-secret>
RESEND_API_KEY=<resend-api-key>
RESEND_FROM_EMAIL=soporte@<verified-domain>
RESEND_FROM_NAME=Control Tower
```

`CT_BASE_URL` is the API origin only; both a trailing slash and a trailing
`/api/v1` are normalized by the POS client.

## Delivery flow

1. POS persists a ticket and sends `POS_SUPPORT_TICKET` with its stable UUID.
2. Control Tower creates the ticket idempotently and notifies ticket operators
   in-app, by WebSocket, mobile push, and email (according to preferences).
3. A public Control Tower reply sends email to the POS requester.
4. After the Control Tower transaction commits, it sends a signed webhook with
   `commentId`, `content`, `senderName`, and `occurredAt` to the POS.
5. POS stores the operator message idempotently, emits its own real-time
   notification, and retains polling as a recovery path for 24 hours after a
   ticket is resolved or closed.

## Smoke test after deployment

1. Create a non-production ticket in POS and confirm it appears once in Control Tower.
2. Confirm the Control Tower bell/sidebar badge and operator email arrive.
3. Add a public reply, then resolve the ticket immediately.
4. Confirm the POS receives both the reply and resolved state, without duplicates.
5. Temporarily use an invalid `CT_API_KEY`; POS must show a failed ticket that can be retried.

Never expose `CT_API_KEY`, `POS_WEBHOOK_SECRET`, or `RESEND_API_KEY` to either frontend.
