# [NG-S09] Provision STE Postgres database and deploy via Flux CD GitOps

## User story
As the **platform engineering team**,
I want **the service's Postgres database/schema provisioned across environments and the service
deployed to STE via a Flux CD `HelmRelease` of the shared `springboot-app` chart**,
so that **cp-notification-gateway runs in STE without any legacy ADO/Helmsman deployment path**.

## Background
FR-016 (Postgres provisioning) and FR-017 (Flux CD deployment) are bundled: the service cannot be
deployed without its database existing first, so together they are one shippable "service is running in
STE" outcome. FR-016 carries no dedicated AC of its own (requirements.md notes it is "exercised by the
Milestone 5 deployment").

## Acceptance criteria
- [ ] AC-026: Given the STE environment, when the service is deployed, then it is via a Flux
  `HelmRelease` of the shared `springboot-app` chart with the per-context identity and CSI secret
  access, and no Helmsman/`cpp-aks-deploy` ADO path is used.
- [ ] Postgres provisioning check (FR-016, no dedicated AC): given the STE environment, when the
  database/schema is provisioned via ops/IaC, then the service's Flyway migrations (NG-S01) apply
  cleanly against it on first deploy.

## NFR links
- NFR-011 (Tech-stack conformance): Postgres 16 + Flyway, matching `tech-stack.md`.
- NFR-013 (Coding in the open): deployment config lives in `cpp-flux-config`, no direct commits bypass
  review.

## Out of scope for this story
- Identity/RBAC provisioning — NG-S08 (FR-014/015), a precondition this story's `HelmRelease` values
  reference (identity/secretProvider) but does not itself create.
- Application-level ASB auth code — NG-S07.

## Definition of done
- [ ] Infra change reviewed and approved.
- [ ] AC-026 verified by confirming the deployed `HelmRelease` in `cpp-flux-config` and absence of any
  Helmsman/ADO deployment artifact for this service.
- [ ] Postgres provisioning check verified by a clean Flyway apply against the provisioned STE database.
- [ ] No critical or high Snyk findings introduced.
- [ ] Deployed to and verified on STE.
- [ ] Jira ticket updated with test evidence (once a ticket exists — see Notes).

## Notes / open questions
- **FR/AC traceability:** FR-016 (no dedicated AC), FR-017 → AC-026.
- **Reference-only Jira keys:** FR-016 → PEG-3367; FR-017 → PEG-3368. Vertical-slice ticket for NG-S09
  will be created later, once the user approves ticket creation.
- Depends on NG-S08 (identity/RBAC/CSI secret) being provisioned first; both are ops/infra stories and
  may be sequenced together at sprint planning.
