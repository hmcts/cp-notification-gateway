# [NG-S10] Office 365 routing and immediate-delivery send path

## User story
As a **producer sending to a `cjsm.net` recipient domain or with an attachment larger than 2MB**,
I want **my email routed via Microsoft Office 365 (through Azure APIM) and marked delivered immediately
on a successful response**,
so that **large attachments and legally-routed recipients are not blocked by Gov.UK Notify's size/domain
constraints**.

## Background
FR-018 (routing rule) and FR-019 (O365 send behaviour) are bundled: a routing decision that selects a
sender with no working implementation behind it delivers no value, and both requirements sit under the
single "Milestone 6 — Office 365 send path" heading in requirements.md. The `SenderFactory` seam itself
is recommended to be scaffolded early (in NG-S02, stubbed) per architecture-design.md Risk #2; this
story replaces the stub with the real APIM-backed sender and completes the routing behaviour.
**Dependency/risk carried from requirements.md:** the mi-reportdata NCES CSV may exceed 2MB — until
this story lands, only ≤2MB Gov.Notify sends work; confirmed by Stage 2 not to be a go-live blocker
(mi-reportdata does not consume until the MVP is fully implemented).

## Acceptance criteria
- [ ] AC-027: Given a recipient domain ending in `cjsm.net` (or configured domain), when routing, then
  Office 365 (APIM) is selected.
- [ ] AC-028: Given an attachment > 2MB, when routing, then Office 365 (APIM) is selected.
- [ ] AC-029: Given a ≤2MB attachment and a non-`cjsm.net` domain, when routing, then Gov.UK Notify is
  selected.
- [ ] AC-030: Given a successful O365 APIM response, when the `send-email` task completes, then
  `markSent()` is called directly and no `check-email-status` task is scheduled.

## NFR links
- NFR-001 (Behavioural parity): sender routing rules unchanged from legacy (domain/size thresholds).

## Out of scope for this story
- O365 error-code → transient/permanent mapping — architecture-design.md's error taxonomy notes this
  "lands with Milestone 6" as a design detail; if a dedicated FR/AC is minted for it later it will need
  its own story, not invented here.
- Any change to the Gov.UK Notify send/poll path (NG-S02) — routing only selects between the two, it
  does not alter either sender's internal behaviour.

## Definition of done
- [ ] Code reviewed and approved.
- [ ] All ACs covered by automated tests (unit + integration).
- [ ] **FR-010/AC-019 harness scenario covered by this story**: the WireMock-stubbed O365 endpoint
  (per FR-010's "WireMock stubbing Gov.Notify/O365") drives a routed send through this story's sender
  in the Testcontainers/ASB-emulator harness.
- [ ] No critical or high Snyk findings introduced.
- [ ] Deployed to and verified on sandbox / STE (via NG-S06's O365 mock connector).
- [ ] Jira ticket updated with test evidence (once a ticket exists — see Notes).

## Notes / open questions
- **FR/AC traceability:** FR-018 → AC-027, AC-028, AC-029; FR-019 → AC-030. **FR-010 → AC-019** partially
  covered here (O365-stub sub-scenario) — see `_index.md` for the full breakdown.
- **Reference-only Jira keys:** FR-018 and FR-019 have **no** Jira key reserved yet in requirements.md's
  Jira column (both blank). Vertical-slice ticket for NG-S10 will be created later, once the user
  approves ticket creation.
- Depends on NG-S02's `SenderFactory` seam existing; does not require NG-S02's Gov.Notify path to
  change.
