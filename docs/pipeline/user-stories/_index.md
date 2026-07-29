# User stories index: cp-notification-gateway (Stage 3)

> Pipeline **Stage 3 (User Story)**. Inputs: `docs/pipeline/architecture-design.md`,
> `docs/pipeline/requirements.md`, `docs/pipeline/legacy-result-event-contract.md`,
> `docs/pipeline/cp-task-manager-stale-lock-gap.md`,
> `docs/pipeline/artifacts/001-notification-gateway-blueprint.html`. **Human gate — not approved until
> the user confirms.** Story IDs (`NG-S01`…`NG-S13`) are local placeholders. **Jira tickets created so
> far (epic [PEG-3350](https://tools.hmcts.net/jira/browse/PEG-3350)): NG-S01 →
> [PEG-3372](https://tools.hmcts.net/jira/browse/PEG-3372), NG-S02 → [PEG-3373](https://tools.hmcts.net/jira/browse/PEG-3373),
> NG-S03 → [PEG-3382](https://tools.hmcts.net/jira/browse/PEG-3382), NG-S04 →
> [PEG-3383](https://tools.hmcts.net/jira/browse/PEG-3383)** (labels `claude-generated`/`needs-review`);
> the remaining stories' tickets are created in later, separately-approved steps.
>
> FRs are numbered in milestone/build order in `requirements.md`, not shippable-slice order. Per the
> Stage 3 re-slice rule, layered FRs that are not independently valuable/testable on their own (e.g.
> ingest → attachment download → send → poll, or O365 routing → O365 send) are bundled into one
> vertical story; FRs that carry separable value (e.g. result-event publish vs. the core send) are
> split into their own story. See each story's "Notes / open questions" for the INVEST rationale.

## FR → Story → AC mapping

| FR | Requirement (short) | Story | AC IDs covered | Notes |
|----|---|---|---|---|
| FR-001 | Scaffold service from HMCTS template | [NG-S01](./NG-S01.md) | (no dedicated AC) | Bundled with FR-002 — enabler |
| FR-002 | Flyway `notification` + co-located `jobs` table | [NG-S01](./NG-S01.md) | AC-001, AC-001a | |
| FR-003 | Listen & orchestrate command ingestion (dedupe, atomic INSERT+enqueue, ack after commit) | [NG-S02](./NG-S02.md) | AC-002, AC-003, AC-004, AC-005, AC-006, AC-007, AC-008 | Bundled with FR-004/005/006 — inseparable layers of "one complete send" |
| FR-004 | Attachment download from Azure Blob | [NG-S02](./NG-S02.md) | AC-009, AC-010 | |
| FR-005 | Gov.UK Notify send + status poll | [NG-S02](./NG-S02.md) | AC-011, AC-012 | |
| FR-006 | cp-task-manager retry/poll timings preserved | [NG-S02](./NG-S02.md) | AC-013 | |
| FR-007 | ReplyTo-gated result publish | [NG-S03](./NG-S03.md) | AC-014, AC-015 | Split from core send — separable value, independently testable via synthetic `ReplyTo` message |
| FR-008 | Result-event contract parity (golden master) | [NG-S03](./NG-S03.md) | AC-016 | |
| FR-009 | Query API returns all columns | [NG-S04](./NG-S04.md) | AC-017, AC-018 | Bundled with FR-024 per Q3 (auth ships in MVP, no unauthenticated PII endpoint) |
| FR-010 | Integration test harness (Testcontainers/ASB emulator/Azurite/WireMock/Awaitility) | *(no dedicated story — folded into DoD, per coordinator decision)* | AC-019 | **Covered via DoD across [NG-S02](./NG-S02.md) (happy path, atomicity, blob 403/404), [NG-S03](./NG-S03.md) (result-queue message), [NG-S10](./NG-S10.md) (O365 stub), [NG-S11](./NG-S11.md) (retry), [NG-S12](./NG-S12.md) (redelivery/idempotency)** — not orphaned |
| FR-011 | CI validation pipeline (GitHub Actions) | [NG-S05](./NG-S05.md) | AC-020 | |
| FR-012 | STE simulator/stub routing (Gov.Notify + O365) | [NG-S06](./NG-S06.md) | AC-021 | |
| FR-013 | Managed-identity RBAC for ASB (application code) | [NG-S07](./NG-S07.md) | AC-022, AC-023 | Split from FR-014/015 — `svc` repo vs. infra repos |
| FR-014 | Per-context identity + RBAC (Blob, Key Vault, ASB) | [NG-S08](./NG-S08.md) | AC-024 | Bundled with FR-015 — RBAC needs the namespace to exist |
| FR-015 | ASB namespace/queue provisioning + role assignments | [NG-S08](./NG-S08.md) | AC-025 | |
| FR-016 | Postgres database/schema provisioning | [NG-S09](./NG-S09.md) | (no dedicated AC) | Bundled with FR-017 — deploy needs the DB first |
| FR-017 | Flux CD GitOps deployment | [NG-S09](./NG-S09.md) | AC-026 | |
| FR-018 | SenderFactory routing (cjsm.net / >2MB → O365) | [NG-S10](./NG-S10.md) | AC-027, AC-028, AC-029 | Bundled with FR-019 — routing with no working sender behind it has no value |
| FR-019 | Office 365 immediate-delivery send | [NG-S10](./NG-S10.md) | AC-030 | |
| FR-020 | Failure handling: retry, permanent failure, DLQ | [NG-S11](./NG-S11.md) | AC-031, AC-032, AC-033 | Distinct from FR-022 — bounds *how long* retries continue |
| FR-021 | *(removed by Stage 2 — dual-write atomicity resolved by co-located transaction; no reconciliation sweep needed)* | — | — | Not carried into Stage 3 |
| FR-022 | At-least-once safety: state guard + Gov.Notify recover-by-reference | [NG-S12](./NG-S12.md) | AC-035, AC-036 | Distinct from FR-020 — prevents *double-acting* on retries |
| FR-023 | Observability: logs, metrics, DLQ-depth | [NG-S13](./NG-S13.md) | AC-037, AC-038 | |
| FR-024 | Secure query API with IDAM auth | [NG-S04](./NG-S04.md) | AC-039 | Pulled forward per Q3 — delivered alongside FR-009, not deferred |

**Coverage check:** every FR-001…FR-024 (FR-021 removed by Stage 2) lands in exactly one story.
Every AC-001…AC-039 in `requirements.md` (AC-034 does not exist — no FR/AC gap, just an unused number)
lands in exactly one story or is explicitly covered via DoD (FR-010/AC-019). No orphaned FR or AC.

## Story list

| Story | Title | FRs | Jira ticket |
|---|---|---|---|
| [NG-S01](./NG-S01.md) | Service scaffold and notification/jobs schema | FR-001, FR-002 | **[PEG-3372](https://tools.hmcts.net/jira/browse/PEG-3372)** (created) |
| [NG-S02](./NG-S02.md) | End-to-end email send (ingest, attachment, Gov.Notify, poll, retry) | FR-003, FR-004, FR-005, FR-006 | **[PEG-3373](https://tools.hmcts.net/jira/browse/PEG-3373)** (created) |
| [NG-S03](./NG-S03.md) | ReplyTo-gated result event with legacy parity | FR-007, FR-008 | **[PEG-3382](https://tools.hmcts.net/jira/browse/PEG-3382)** (created) |
| [NG-S04](./NG-S04.md) | Secured read (query) API | FR-009, FR-024 | **[PEG-3383](https://tools.hmcts.net/jira/browse/PEG-3383)** (created) |
| [NG-S05](./NG-S05.md) | CI validation pipeline (GitHub Actions) | FR-011 | not yet created |
| [NG-S06](./NG-S06.md) | STE provider simulators/stubs | FR-012 | not yet created |
| [NG-S07](./NG-S07.md) | Managed-identity RBAC for ASB (application code) | FR-013 | not yet created |
| [NG-S08](./NG-S08.md) | Provision identity + ASB namespace/queues + RBAC (infra) | FR-014, FR-015 | not yet created |
| [NG-S09](./NG-S09.md) | Provision STE Postgres + Flux CD deploy | FR-016, FR-017 | not yet created |
| [NG-S10](./NG-S10.md) | Office 365 routing and immediate-delivery send | FR-018, FR-019 | not yet created |
| [NG-S11](./NG-S11.md) | Failure handling: retry, permanent failure, DLQ | FR-020 | not yet created |
| [NG-S12](./NG-S12.md) | At-least-once safety: state guard + recover-by-reference | FR-022 | not yet created |
| [NG-S13](./NG-S13.md) | Observability: logs, metrics, DLQ-depth | FR-023 | not yet created |

Note on FR-010: the integration-test-harness FR has no dedicated story in this backlog — per the
coordinator's Q1 decision its scenarios are folded into the DoD of NG-S02, NG-S03, NG-S10, NG-S11,
NG-S12 (see mapping table above).

## ADR flags raised in this pass
- **NG-S01**: *"Direct-to-DB + cp-task-manager replaces CQRS/Event-Sourcing for notification-notify"*
  (Stage 2 recommendation #1, pattern departure from the legacy context).
- **NG-S12**: *"Idempotent send via Gov.UK Notify recover-by-reference"* (Stage 2 recommendation #2,
  FR-022 duplicate-send prevention without a sweep/outbox).

## Open / blocked items

- **Data retention period (NFR-014, UK-GDPR / Data Protection Act 2018) — stage-gating, must be
  resolved before go-live.** `requirements.md` NFR-014 requires the `notification` table's retention
  period to be "defined and enforced" but no value is given, and no FR in this backlog enforces a
  purge/retention mechanism. Stage 2's Handoff Gap Report (Gap #4) explicitly defers enforcement and
  routes this to product/DPO rather than inventing a period. **No story or ticket is created for this
  in Stage 3** — per the coordinator's Q3 decision, this is recorded here as a single open-item line
  only. Once product/DPO set the retention period, amend `requirements.md` NFR-014 with the agreed
  value, and a new story (with a real AC, e.g. a scheduled purge job) should be raised at that point —
  the current schema does not preclude this (indexed `created_at`).
- **cp-task-manager stale-lock recovery gap (OQ-4)** — upstream library gap (no reaper for jobs whose
  worker crashed mid-execution); documented in full in `cp-task-manager-stale-lock-gap.md` and
  referenced as a risk note inside [NG-S12](./NG-S12.md). **No story or ticket is created for this in
  this backlog** — it belongs to the cp-task-manager library/repo, per the explicit carry-forward
  instruction from Stage 2.
- **OQ-1 (ASO capability for ASB CRDs)** — Platform-owned, referenced inside [NG-S08](./NG-S08.md);
  decides the provisioning *mechanism* only, not the RBAC outcome described in that story's ACs.
- **OQ-2 (ACR pull / App Insights identity) and OQ-3 (`sadevcommonscsl` shared storage account)** —
  Platform-owned, carried as assumptions per `requirements.md`; not attached to any specific story.

## Handoff to Test Specs (Stage 4)

Inputs the test-engineer stage must read (explicit paths — Stage 4 reads only what is listed here):
- `docs/pipeline/user-stories/_index.md` (this file — FR→Story→AC mapping, open/blocked items, ADR flags)
- `docs/pipeline/user-stories/NG-S01.md`
- `docs/pipeline/user-stories/NG-S02.md`
- `docs/pipeline/user-stories/NG-S03.md`
- `docs/pipeline/user-stories/NG-S04.md`
- `docs/pipeline/user-stories/NG-S05.md`
- `docs/pipeline/user-stories/NG-S06.md`
- `docs/pipeline/user-stories/NG-S07.md`
- `docs/pipeline/user-stories/NG-S08.md`
- `docs/pipeline/user-stories/NG-S09.md`
- `docs/pipeline/user-stories/NG-S10.md`
- `docs/pipeline/user-stories/NG-S11.md`
- `docs/pipeline/user-stories/NG-S12.md`
- `docs/pipeline/user-stories/NG-S13.md`
- `docs/pipeline/requirements.md` (governing FRs/ACs/NFRs, referenced by ID from the stories above)
- `docs/pipeline/legacy-result-event-contract.md` (golden-master detail underlying NG-S03's ACs)

**Carry-forwards for Stage 4 to honour:**
- FR-010/AC-019 has no dedicated story — its five sub-scenarios are Definition-of-Done items inside
  NG-S02/NG-S03/NG-S10/NG-S11/NG-S12; Stage 4 should write test specs against those DoD items directly,
  not look for a standalone "harness" story.
- No Jira tickets exist yet for any of these 13 stories — test-spec authoring should reference story
  IDs (`NG-S01`…`NG-S13`), not Jira keys, until ticket creation is separately approved.
- OQ-4 (cp-task-manager stale-lock gap) is a risk note inside NG-S12 only — do not write a test spec
  attempting to verify a fix for it; it is out of this service's control.
- The data-retention open item has no AC — do not invent one; it stays untested until product/DPO
  resolve it and a real story/AC is raised.

**This is the Stage 3 human gate. Halting here — do not proceed to test-engineer (Stage 4) until the
user confirms these 13 stories are approved.**
