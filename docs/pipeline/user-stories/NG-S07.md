# [NG-S07] Managed-identity RBAC for Azure Service Bus (application code)

## User story
As **the service (cp-gov-uk-notify-gateway)**,
I want **to authenticate all Azure Service Bus access — consume and send — exclusively via
managed-identity RBAC using `DefaultAzureCredential`**,
so that **no SAS token or connection string ever exists in production configuration**.

## Background
Application-code story, distinct from the infra-provisioning story (NG-S08) per requirements.md's own
"Repo references" column (this FR is `svc`; FR-014/015 are `cpp-helm-chart`/`cpp-aks-deploy`). The ASB
client is built on `DefaultAzureCredential` from the outset (NG-S02 already uses the emulator connection
string locally); this story is about proving the non-emulator, production auth path.

## Acceptance criteria
- [ ] AC-022: Given the service running in a non-emulator environment, when it connects to ASB, then it
  uses `DefaultAzureCredential` and no SAS token or connection string is present in config, env vars, or
  Helm values.
- [ ] AC-023: Given the managed identity, when RBAC is inspected, then it holds Azure Service Bus Data
  Receiver on the command queue (`ng-send-email`) and Azure Service Bus Data Sender on the result queue
  (`mi-reportdata-notification-result`), both at **queue (entity) scope** — least-privilege, not
  namespace scope. The inbound `ReplyTo` resolves to the single provisioned result queue, so a
  queue-scoped Sender is sufficient. Constraint: because the code sends to whatever `ReplyTo` the message
  carries, the set of `ReplyTo` values MUST stay within the queues granted here — if additional result
  queues are introduced, extend the Sender grant per queue (or widen to namespace scope).

## NFR links
- NFR-002 (Security — auth to Azure): zero SAS tokens/connection strings in code, config, env vars,
  Helm values (emulator-only exception for local/test).

## Out of scope for this story
- Provisioning the identity itself and its RBAC role assignments in the target environment — NG-S08
  (FR-014/015); this story is the application code that *uses* whatever identity NG-S08 provisions.
- Blob and Key Vault auth code (also managed-identity based) — covered incidentally by NG-S02's
  `AttachmentDownloader` (FR-004) and is not a separate AC here.

## Definition of done
- [ ] Code reviewed and approved.
- [ ] AC-022/AC-023 covered by automated tests (config assertion that no SAS/connection-string property
  is read outside the emulator-local profile) plus a manual RBAC verification against a real
  non-emulator ASB namespace in STE.
- [ ] No critical or high Snyk findings introduced.
- [ ] Deployed to and verified on sandbox / STE.
- [ ] Jira ticket updated with test evidence (once a ticket exists — see Notes).

## Notes / open questions
- **FR/AC traceability:** FR-013 → AC-022, AC-023.
- **Jira story ticket:** not yet created — to be raised once ticket creation is approved.
- Verification of AC-023 (queue-scope RBAC) is only fully provable once NG-S08 has provisioned the
  real identity in STE — this story's code can be reviewed/merged independently, but its DoD's
  "deployed to and verified" step is naturally sequenced after NG-S08. *(Verified in STE01 on 2026-08-13:
  the `govuknotifygateway` MI holds queue-scoped `Data Receiver@ng-send-email` +
  `Data Sender@mi-reportdata-notification-result` — matches this AC.)*
