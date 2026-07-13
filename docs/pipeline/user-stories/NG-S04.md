# [NG-S04] Secured read (query) API for notification status

## User story
As a **support/operations engineer (or an authenticated machine consumer)**,
I want **to query a notification's full record via an authenticated read-only REST API**,
so that **I can triage incidents and check delivery status without direct database access, and without
any PII ever being exposed unauthenticated**.

## Background
Independently valuable and independently testable (can be built/tested against seeded rows without the
full send pipeline being complete), but depends on the NG-S01 schema. Per the Q3 carry-forward, FR-024
(IDAM auth) is pulled into the MVP and delivered alongside FR-009 in this same story — **no story in
this backlog ships an unauthenticated PII-returning endpoint**.

## Acceptance criteria
- [ ] AC-017: Given a persisted notification, when the read API is queried by id
  (`GET /notifications/{notificationId}`), then the response contains all columns of the `notification`
  table.
- [ ] AC-018: Given the query API, when any request is made, then it is read-only (no
  command/write side effects) — includes the paged search endpoint
  (`GET /notifications?status=&createdFrom=&createdTo=&page=&size=`).
- [ ] AC-039: Given an unauthenticated request to the query API, when received, then it is rejected
  (401/403); an authenticated request (valid IDAM JWT) is served.

## NFR links
- NFR-004 (Security — logging/PII): responses may contain PII (`send_to_address`); the API itself must
  never log PII, secrets, tokens, or full request/response bodies.
- NFR-005 (Data classification): notification data is OFFICIAL-SENSITIVE; safe to return because the
  endpoint is authenticated from day one.
- NFR-006 (OWASP): assessed against the OWASP Top 10 — no unresolved Critical/High.

## Out of scope for this story
- Write/command endpoints (the "no REST" rule applies only to the command path, not this API).
- `sendToAddress`-based search filtering — optional per the Q5 default; may be added as a follow-on if
  a real consumer needs it.
- The companion `api-notification-gateway` OpenAPI repo's CI/publishing setup (tracked as a Stage 2
  follow-up, not an FR/AC in this backlog).

## Definition of done
- [ ] Code reviewed and approved.
- [ ] All ACs covered by automated tests (unit + integration), including an explicit
  unauthenticated-request-rejected test.
- [ ] Accessibility audit — not applicable (machine/API consumer, no UI, NFR-015).
- [ ] No critical or high Snyk findings introduced.
- [ ] OWASP Top 10 review passed (NFR-006).
- [ ] Deployed to and verified on sandbox.
- [ ] Jira ticket updated with test evidence (once a ticket exists — see Notes).

## Notes / open questions
- **FR/AC traceability:** FR-009 → AC-017, AC-018; FR-024 → AC-039 (pulled forward per Q3, delivered
  alongside FR-009 in this same story rather than deferred to Milestone 7).
- **Reference-only Jira key:** FR-009 → PEG-3360. FR-024 has **no** Jira key reserved yet in
  requirements.md's Jira column (blank — it originally sat in Milestone 7 before being pulled forward).
  Vertical-slice tickets for NG-S04 will be created later, once the user approves ticket creation.
- OpenAPI spec lives in the companion `api-notification-gateway` repo (API-first, per Q6); this story's
  DoD assumes that repo/spec exists or is created alongside it.
- Depends on NG-S01 (schema) only; does not require NG-S02/NG-S03 to be complete (can be tested with
  seeded rows), though it has no real data to show until NG-S02 lands.
