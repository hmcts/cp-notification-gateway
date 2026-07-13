# [NG-S06] STE provider simulators/stubs (Gov.UK Notify + Office 365)

## User story
As an **engineer running end-to-end deployment tests in STE**,
I want **Gov.UK Notify and Microsoft Office 365 sends routed to a sandbox/test-mode or mocked endpoint
in the STE environment**,
so that **deployment verification exercises the real send paths without dispatching real emails**.

## Background
Standalone testing-infrastructure story, own milestone (Milestone 4). The per-provider mechanism
(hosted sandbox/test-mode API key for Gov.Notify vs. a deployed mock connector for O365/APIM) is decided
during implementation of this story, per FR-012.

## Acceptance criteria
- [ ] AC-021: Given the STE environment, when the service sends via Gov.Notify or O365, then it targets
  the configured sandbox/test-mode or mock endpoint (not production), and no real email is dispatched.

## NFR links
- NFR-012 (Test coverage): enables the STE E2E test coverage required by the test pyramid.

## Out of scope for this story
- Production Gov.Notify/O365 credentials and routing — those are configured per-environment via the
  same `SenderFactory` seam (NG-S02/NG-S10), just pointed at different endpoints in STE vs. prod.

## Definition of done
- [ ] Code reviewed and approved.
- [ ] AC-021 covered by an STE E2E smoke test confirming no real email is dispatched.
- [ ] No critical or high Snyk findings introduced.
- [ ] Deployed to and verified on the STE environment (this story's purpose is precisely to make that
  verification safe).
- [ ] Jira ticket updated with test evidence (once a ticket exists — see Notes).

## Notes / open questions
- **FR/AC traceability:** FR-012 → AC-021.
- **Reference-only Jira key:** FR-012 → PEG-3363. Vertical-slice ticket for NG-S06 will be created
  later, once the user approves ticket creation.
- Depends on NG-S02 (Gov.Notify send path) to have something to stub; the O365 half of this story
  depends on NG-S10 (Office 365 send path) existing, so may need to land in two increments if NG-S10
  is not yet complete — flagged for sequencing at sprint planning, not a blocking clarification.
