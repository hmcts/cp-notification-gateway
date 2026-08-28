# [NG-S12] At-least-once safety: state-transition guard and Gov.UK Notify recover-by-reference

## User story
As **the service (cp-gov-uk-notify-gateway)**,
I want **cp-task-manager task re-runs and ASB message redeliveries to never cause a duplicate email
send or a duplicate result-event publish**,
so that **at-least-once delivery/execution semantics produce an exactly-once *observable* effect**.

## Background
Distinct mechanism from NG-S11: this story guards against *double-acting* on retries/redeliveries,
rather than bounding *how long* retries continue. Two concrete mechanisms, both required together:
(1) a state-transition guard on every terminal transition and result-event publish (only fires on a
real QUEUED/ATTEMPTED → terminal transition), and (2) at the start of the `send-email` task, a
recover-by-reference check against Gov.UK Notify (client `reference` = `notificationId`) before sending,
to close the duplicate-send window a crash-after-send / task re-run would otherwise open (the legacy
service has no such guard — it re-sends).

## Acceptance criteria
- [ ] AC-035: Given a task re-run against an already-terminal row, when it reaches
  `markSent()`/`markFailed()`, then the state-transition guard blocks the update and suppresses a
  duplicate result event.
- [ ] AC-036: Given a crash after the Gov.Notify send but before recording, when the task re-runs, then
  the service queries Gov.Notify by reference and does not re-send.

## NFR links
- NFR-009 (Reliability — idempotency): exactly-once observable effect under at-least-once delivery.
- NFR-010 (Reliability — atomicity): state-update + result-event publish is at-least-once, made safe by
  this guard.

## Out of scope for this story
- Retry-count bounding and DLQ routing on genuine failures — NG-S11 (FR-020).
- **cp-task-manager's stale-lock (crash-recovery) gap — explicitly no story and no ticket for this in
  this backlog** (see Notes below); it is an upstream library gap tracked separately.

## Definition of done
- [ ] Code reviewed and approved.
- [ ] All ACs covered by automated tests (unit + integration), including a simulated crash-after-send
  scenario asserting no duplicate email via the WireMock Gov.Notify stub.
- [ ] **FR-010/AC-019 harness scenario covered by this story**: the redelivery/idempotency sub-scenario
  of AC-019 (duplicate command / task re-run produces a single row and no duplicate send/result event).
- [ ] No critical or high Snyk findings introduced.
- [ ] Deployed to and verified on sandbox.
- [ ] Jira ticket updated with test evidence (once a ticket exists — see Notes).

## Notes / open questions
- **FR/AC traceability:** FR-022 → AC-035, AC-036. **FR-010 → AC-019** partially covered here
  (redelivery/idempotency sub-scenario) — see `_index.md` for the full breakdown.
- **Reference-only Jira key:** FR-022 has **no** Jira key reserved yet in requirements.md's Jira column
  (blank). Vertical-slice ticket for NG-S12 will be created later, once the user approves ticket
  creation.
- **ADR flag:** Stage 2 recommends drafting ADR *"Idempotent send via Gov.UK Notify recover-by-reference"*
  (architecture-design.md Follow-ups) — how duplicate sends are prevented without a transactional
  outbox/reconciliation sweep. Draft alongside this story (skill: `skills/adr-template.md`).
- **Risk note (external dependency, no story/ticket in this backlog):** cp-task-manager has no
  stale-lock/crash recovery for jobs whose worker died mid-execution (`worker_id`/`worker_lock_time` set
  but never reclaimed on a hard crash) — a regression from the legacy jobstore's 1-hour lease. Documented
  fully in `cp-task-manager-stale-lock-gap.md` (design OQ-4). This is **independent** of AC-035/AC-036
  above (which cover crash-after-send-but-before-record and re-run-against-terminal-row; the stale-lock
  gap covers a crash *while a job is locked and executing*, which strands the notification non-terminal
  until the upstream library adds a reaper/reclaim clause). Per explicit instruction, **no story and no
  Jira ticket is created for this gap here** — it is tracked upstream in the cp-task-manager repo/library,
  referenced here only as a risk to be aware of when reviewing this story's crash-safety tests.
- Depends on NG-S02 (core send path); independent of NG-S03/NG-S10/NG-S11.
