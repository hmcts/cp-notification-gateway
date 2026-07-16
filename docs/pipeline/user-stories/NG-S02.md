# [NG-S02] End-to-end email send: ingest, attachment download, Gov.UK Notify send, status poll, retry

## User story
As **the mi-reportdata producer context (and, transitively, its NCES report recipients)**,
I want **a `send-email-notification` command I publish to be durably received exactly once, its
attachment fetched from Azure Blob, and the email actually delivered via Gov.UK Notify with delivery
status tracked through to `SENT`**,
so that **report recipients reliably receive their NCES extract even under ASB redelivery, transient
provider failures, or duplicate commands**.

## Background
This is the core MVP vertical slice. Per the re-slice rule, FR-003 (ingest/dedupe/atomicity) →
FR-004 (blob download) → FR-005 (Gov.Notify send + poll) → FR-006 (preserved retry/poll timings) are
inseparable layers of one behaviour — none of them is independently valuable or demoable on its own
(an ingested-but-never-sent row, or a send path with no reliable ingest, delivers nothing to
mi-reportdata) — so they are bundled into one story, matching Milestone 1's own framing ("deliver one
complete send"). Runs against the local emulator/Testcontainers stack (FR-010); production RBAC lands
in NG-S07/NG-S08.

## Acceptance criteria
- [ ] AC-002: Given a `send-email-notification` message on the shared `nn-send-email` command queue,
  when the service is running, then the `ServiceBusProcessorClient` consumes it and no REST command
  endpoint exists on the service.
- [ ] AC-003: Given two clients sending to the same command-type queue, when both messages are
  consumed, then both are processed through the same shared queue and handler — no per-client queue,
  no per-originator routing (`clientContext` is a passthrough only).
- [ ] AC-004: Given a command with a `notificationId` not present in the table, when consumed, then a
  `notification` row is inserted with status `QUEUED` — and, when the inbound ASB message carries a
  `ReplyTo` property, its value is persisted into `result_queue` on that same insert (consumed later by
  NG-S03/FR-007; `null` when absent, the mi-reportdata MVP case).
- [ ] AC-005: Given a command whose `notificationId` already exists, when consumed (redelivery or
  duplicate), then it is a silent no-op — no second row, no second task.
- [ ] AC-006: Given a new command, when it is processed, then the `notification` row insert and the
  `send-email` task enqueue commit together in one Postgres transaction; if either fails, neither
  persists.
- [ ] AC-007: Given a message being processed with `disableAutoComplete`, when the local transaction
  commits successfully, then the ASB message is completed (acked).
- [ ] AC-008: Given a processing failure before commit, when the transaction rolls back, then the
  message is not completed — it is abandoned and eligible for redelivery/DLQ.
- [ ] AC-009: Given a command carrying a `fileUri`, when the `send-email` task runs, then the
  attachment is downloaded from Azure Blob via `azure-storage-blob` + Workload Identity and passed to
  `SenderFactory`.
- [ ] AC-010: Given a blob returning 403 or 404, when download is attempted, then the notification is
  marked FAILED and not retried indefinitely.
- [ ] AC-011: Given a successful Gov.Notify send, when the task completes, then a `check-email-status`
  task is scheduled and the Gov.Notify external reference id is carried in that task's payload
  (`job_data`) — not persisted on the `notification` row.
- [ ] AC-012: Given `check-email-status` polling returns `DELIVERED`, when the task runs, then
  `markSent()` is called and status becomes `SENT`.
- [ ] AC-013: Given the email flow, when retry/poll intervals are inspected, then they match the legacy
  email retry-duration list `60,300,1800,3600,7200,14400` byte-for-byte — sourced from the
  `cp.notification.retry.email-durations-secs` property (default = that legacy list, environment-overridable
  like the legacy `@GlobalValue`), not a hard-coded constant.

## NFR links
- NFR-001 (Behavioural parity): retry-duration list byte-equal to legacy; sender routing unchanged.
- NFR-002 (Managed-identity RBAC): Blob download via Workload Identity, no SAS/connection strings.
- NFR-005 (Data classification): attachment content and `send_to_address` are OFFICIAL-SENSITIVE.
- NFR-009 (Idempotency): duplicate command / redelivery produces a single row, no duplicate send.
- NFR-010 (Atomicity): INSERT + task enqueue commit in one local transaction; no orphaned `QUEUED` row.

## Out of scope for this story
- Office 365 routing/send — only the `SenderFactory` seam is scaffolded here (stub branch); the live
  behaviour is NG-S10 (FR-018/019).
- Result-event building/publishing — NG-S03 (FR-007/008); this story only reaches the terminal state
  (`markSent()`/`markFailed()`), it does not publish anything.
- Failure/DLQ hardening beyond "abandon on pre-commit failure" (AC-008) — deeper retry/DLQ semantics on
  the task side are NG-S11 (FR-020).
- Crash-recovery dedupe against Gov.Notify (recover-by-reference) — NG-S12 (FR-022).

## Definition of done
- [ ] Code reviewed and approved.
- [ ] All ACs above covered by automated tests (unit + integration).
- [ ] **FR-010/AC-019 harness scenarios covered by this story** (folded in per the Q1 decision — this
  story owns the core-send slice of the Testcontainers/ASB-emulator/Azurite/WireMock harness, no external
  calls). **End-to-end behaviour is verified only through BDD acceptance scenarios** (Cucumber over the
  full emulator stack); boundary-specific outcomes stay in the relevant boundary integration test:
  - [ ] Happy path (BDD acceptance): a command on the queue drives blob download → Gov.Notify (WireMock
    stub) → row `SENT`.
  - [ ] Unretrievable attachment (BDD acceptance, negative): a missing/403/404 blob fails the
    notification (`FAILED`) and no email is sent.
  - [ ] Atomicity (messaging boundary test, `SendEmailConsumerIntegrationTest.Atomicity`): a forced
    failure between INSERT and task-enqueue leaves neither row nor task persisted, and the ASB message is
    not completed.
  - Note: the non-retry guarantee behind the blob-failure path is a boundary/task concern —
    `AttachmentDownloaderIntegrationTest` proves 404 → non-retryable `PermanentBlobException` — so the BDD
    scenario asserts only the business outcome (`FAILED`, no email), not the retry mechanics.
- [ ] Accessibility audit — not applicable (no UI, NFR-015).
- [ ] No critical or high Snyk findings introduced.
- [ ] Deployed to and verified on sandbox (Testcontainers/emulator stack; real STE RBAC is NG-S07/S08).
- [ ] Jira ticket updated with test evidence (once a ticket exists — see Notes).

## Notes / open questions
- **FR/AC traceability:** FR-003 → AC-002–AC-008; FR-004 → AC-009, AC-010; FR-005 → AC-011, AC-012;
  FR-006 → AC-013. **FR-010 → AC-019 covered via this story's DoD** (happy path, atomicity, blob
  403/404 sub-scenarios); the redelivery/idempotency and retry sub-scenarios of AC-019 are covered via
  NG-S12 and NG-S11 respectively (see `_index.md` for the full breakdown).
- **Jira story ticket: [PEG-3373](https://tools.hmcts.net/jira/browse/PEG-3373)** — created and linked to
  epic PEG-3350 (labels `claude-generated`, `needs-review`). The per-FR keys reserved in
  `requirements.md`'s Jira column remain **reference-only** for traceability: FR-003 → PEG-3353; FR-004 →
  PEG-3355 (PEG-3354 skipped/unused, not an error); FR-005 → PEG-3356; FR-006 → PEG-3357; FR-010 →
  PEG-3361 (harness, folded into this story's DoD).
- Recommend scaffolding the `SenderFactory` routing seam (with a stubbed Office 365 branch) as part of
  this story's implementation, per architecture-design.md Risk #2 — even though the live O365 behaviour
  (FR-018/019) ships in NG-S10 — so the interface exists before NG-S10 starts.
- Depends on NG-S01 (schema/scaffold) being complete; no dependency on any other feature story.
