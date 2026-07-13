# [NG-S03] ReplyTo-gated result event with legacy golden-master parity

## User story
As a **future originator context that sets `ReplyTo` on its command**,
I want **a terminal result event (`notification-sent` or `notification-failed`) published to my reply
queue, with a payload matching the legacy golden-master contract exactly**,
so that **I can consume delivery outcomes with zero observable behaviour change from the legacy
`cpp-context-notification-notify` service**.

## Background
Separable from the core send (NG-S02): the mi-reportdata MVP sets no `ReplyTo`, so this path is a
no-op for the MVP's only real client, but it is required by Milestone 1/FR-007/FR-008 for parity and
for any future originator that opts in. It is independently testable via a synthetic `ReplyTo`-bearing
message (per `legacy-result-event-contract.md`), which is why it is split out rather than bundled into
NG-S02.

## Acceptance criteria
- [ ] AC-014: Given an inbound message with `ReplyTo` set, when the notification reaches a terminal
  state, then the corresponding result event is sent to the queue named by `ReplyTo`.
- [ ] AC-015: Given an inbound message with no `ReplyTo` (the mi-reportdata MVP), when the notification
  reaches a terminal state, then no result event is published and the send completes silently.
- [ ] AC-016: Given a `markSent()`/`markFailed()` outcome, when the result event is built, then its
  name and payload match the legacy golden-master payload exactly (only routing changes from topic to
  `ReplyTo` queue):
  - `notification-sent`: verbatim copy of the internal event — `notificationId` (required), `sentTime`
    (required), plus `completedAt`/`sendToAddress`/`replyToAddress`/`emailSubject`/`emailBody`/
    `clientContext` when present; `emailSubject`/`emailBody`/`replyToAddress` sourced from the Gov.Notify
    send response.
  - `notification-failed`: hand-built — always `notificationId`/`failedTime`/`errorMessage`; adds
    `statusCode`/`clientContext` only if present; drops `failedTask`; no nulls emitted.
  - Both: `additionalProperties: false` — no extra fields.

## NFR links
- NFR-001 (Behavioural parity): golden-master payload diff = zero differences (per
  `legacy-result-event-contract.md`).
- NFR-009 (Idempotency): at most one result event per notification, guarded by the same
  state-transition guard as NG-S12.

## Out of scope for this story
- `email-notification-bounced` and `poca-email-notification-received` events (out-of-scope bounce/POCA
  flows, per requirements.md § Out of scope).
- The state-transition guard mechanism itself that prevents a duplicate publish on task re-run —
  implemented in NG-S12 (FR-022); this story assumes the guard exists and only builds/routes the
  payload.

## Definition of done
- [ ] Code reviewed and approved.
- [ ] All ACs covered by automated tests (unit + integration), including a golden-master diff test
  against the two JSON schemas captured in `legacy-result-event-contract.md`.
- [ ] **FR-010/AC-019 harness scenario covered by this story**: a synthetic `ReplyTo`-bearing message
  drives a result-queue message end to end in the Testcontainers/ASB-emulator harness.
- [ ] No critical or high Snyk findings introduced.
- [ ] Deployed to and verified on sandbox.
- [ ] Jira ticket updated with test evidence (once a ticket exists — see Notes).

## Notes / open questions
- **FR/AC traceability:** FR-007 → AC-014, AC-015; FR-008 → AC-016. **FR-010 → AC-019** partially
  covered here (result-queue-message sub-scenario) — see `_index.md` for the full breakdown across
  NG-S02/NG-S03/NG-S10/NG-S11/NG-S12.
- **Reference-only Jira keys:** FR-007 → PEG-3358; FR-008 → PEG-3359. Vertical-slice tickets for NG-S03
  will be created later, once the user approves ticket creation.
- **Schema-owning location (Q6, resolved by Stage 2):** the two JSON schemas live in-repo under
  `contracts/` in `cp-notification-gateway`, additive-only evolution; promote to a shared registry only
  when a real originator opts in.
- Depends on NG-S02 (needs the terminal-state hooks from `markSent()`/`markFailed()`); does not require
  any change to NG-S02's core send logic.
