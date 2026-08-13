# [NG-S08] Provision per-context identity, ASB namespace/queues, and RBAC (infrastructure)

## User story
As the **platform engineering team**,
I want **the service's per-context managed identity provisioned with RBAC (ASB Data Receiver + Data
Sender at queue (entity) scope, Storage Blob Data Reader on the attachment container(s), Key Vault Secrets
User read-only), and the ASB namespace + command/result queues provisioned with those role assignments
in place**,
so that **cp-notification-gateway can authenticate to ASB, Blob, and Key Vault without any secret ever
being stored**.

## Background
FR-014 (identity + RBAC) and FR-015 (namespace/queue provisioning + role assignment) are bundled: RBAC
role assignment is meaningless until the namespace/queues and the identity both exist, so they are one
shippable infrastructure outcome. This is an ops/IaC story (`cpp-helm-chart`, `cpp-aks-deploy`, possibly
a Platform out-of-band request per OQ-1), distinct from the application code in NG-S07.

## Acceptance criteria
- [ ] AC-024: Given the deployed identity, when RBAC is listed, then it holds ASB Data Receiver on the
  command queue (`ng-send-email`) + Data Sender on the result queue (`mi-reportdata-notification-result`)
  at **queue (entity) scope**, Storage Blob Data Reader on the attachment container(s) — including
  cross-context read of mi-reportdata's container — and Key Vault Secrets User; no ASB SAS secret
  exists in Key Vault.
- [ ] AC-025: Given the ASB namespace, when provisioned, then the command and result queues exist and
  the identity's role assignments are present.

## NFR links
- NFR-002 (Security — auth to Azure): managed-identity RBAC only, no SAS/connection strings.
- NFR-003 (Security — secrets): Key Vault Secrets User (read-only), not KeyVaultAdministrator; no
  secret committed to the repo.

## Out of scope for this story
- The application code that consumes this identity (`DefaultAzureCredential` usage) — NG-S07.
- Postgres provisioning and the Flux CD deployment itself — NG-S09 (FR-016/017).

## Definition of done
- [ ] Infra change reviewed and approved (Helm/Terraform/CRD review, not just code review).
- [ ] AC-024/AC-025 verified by inspecting the deployed RBAC assignments and queue existence in a real
  (non-emulator) environment.
- [ ] No ASB SAS secret present in Key Vault (explicit check).
- [ ] Cross-context Storage Blob Data Reader grant on mi-reportdata's container confirmed with
  mi-reportdata's owning team (ownership/approval per requirements.md § Dependencies).
- [ ] Deployed to and verified on STE.
- [ ] Jira ticket updated with test evidence (once a ticket exists — see Notes).

## Notes / open questions
- **FR/AC traceability:** FR-014, FR-015 → AC-024, AC-025.
- **Jira story ticket:** not yet created — to be raised once ticket creation is approved.
- **Inherited open item (Platform-owned, not blocking this story's docs):** OQ-1 — whether the
  cluster's ASO version can provision ASB namespaces/queues and role-assignment CRDs directly
  (`cpp-helm-chart`) or whether this requires a Platform out-of-band request. Both provisioning paths
  yield the same queue-scoped RBAC outcome described in the ACs above; Platform decides the
  mechanism. Owner: Platform, due TBD (per requirements.md Open questions and architecture-design.md
  Deployment section).
- Cross-context blob RBAC grant on mi-reportdata's container needs sign-off from the mi-reportdata
  team — an external dependency, not a blocking clarification for this document.
