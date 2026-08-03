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

The integration endpoint must be active and the endpoint UUID and API key must
belong to the same Control Tower environment. A `404 IntegrationEndpoint not
found` response is a credential/environment mismatch, not a missing POS ticket.

The POS health check validates the actual integration credentials through:

```text
GET <CT_BASE_URL>/api/v1/integrations/<CT_ENDPOINT_ID>/verify
X-Api-Key: <CT_API_KEY>
```

It no longer treats a successful generic Control Tower actuator check as proof
that ticket delivery is working.

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

If a ticket was created without `callbackUrl`, Control Tower derives the POS
webhook URL from the endpoint health/pull URL. Polling remains the recovery path
for older tickets and for temporary webhook failures.

## Email prerequisites

- `RESEND_API_KEY` must be present in the Control Tower backend runtime.
- `RESEND_FROM_EMAIL` (or the tenant `mail.from` value) must use a domain verified
  by Resend.
- Operators must have the `POS_TICKET` email preference enabled to receive new
  incident alerts.
- POS ticket payloads must contain `submitterEmail` and, when applicable,
  `managerEmail` so public replies can be emailed back to the restaurant.

## Deployment order

1. Deploy Control Tower Backend (credential verification and reliable webhook delivery).
2. Configure the shared secret and Resend variables in the runtime.
3. Deploy POS Backend (authenticated health check, repaired polling and delivery errors).
4. Deploy POS Frontend (visible rollback/error when a reply is rejected).
5. Run the smoke test below before enabling the flow for production users.

## Smoke test after deployment

1. Create a non-production ticket in POS and confirm it appears once in Control Tower.
2. Confirm the Control Tower bell/sidebar badge and operator email arrive.
3. Add a public reply, then resolve the ticket immediately.
4. Confirm the POS receives both the reply and resolved state, without duplicates.
5. Temporarily use an invalid `CT_API_KEY`; POS must show a failed ticket that can be retried.

Never expose `CT_API_KEY`, `POS_WEBHOOK_SECRET`, or `RESEND_API_KEY` to either frontend.
