# [NG-S05] CI validation pipeline (GitHub Actions)

## User story
As a **developer on cp-gov-uk-notify-gateway**,
I want **every pull request to run build, unit/integration test, lint, secrets-scan, and CodeQL/SBOM
gates via GitHub Actions**,
so that **no change merges to `main` without passing the full house-style validation suite**.

## Background
Standalone enabler, own milestone (Milestone 3) and own AC (AC-020). Mirrors `cpp-mbd-idam-integration`;
explicitly **not** Azure DevOps (FR-011). Runs the FR-010-scope test suites that later stories add to,
but the pipeline itself is buildable/testable as soon as the scaffold (NG-S01) exists.

## Acceptance criteria
- [ ] AC-020: Given a pull request, when the GitHub Actions workflows run (`ci-draft.yml` →
  `ci-build-publish.yml` build+test, `code-analysis.yml` PMD lint, `secrets-scanner.yml`,
  `codeql.yml`), then the build, test, and lint gates execute (running the unit + integration suites)
  and must all pass before merge. Release builds (`ci-released.yml`) validate the apiSpec version then
  build/publish.

## NFR links
- NFR-012 (Test coverage): CI is the enforcement point for unit ≥80% on new code and the integration
  test pyramid.
- NFR-013 (Coding in the open): PR with ≥1 human approval, no direct commits to `main`, enforced by
  branch protection tied to these gates.

## Out of scope for this story
- The content of the test suites themselves (owned by each feature story's DoD).
- STE deployment pipelines (Flux CD) — NG-S09.

## Definition of done
- [ ] Code reviewed and approved.
- [ ] AC-020 covered by a demonstration PR exercising all five workflows.
- [ ] No critical or high Snyk findings introduced (secrets-scanner + CodeQL gates enforce this going
  forward).
- [ ] Branch protection on `main` requires all workflows green + ≥1 human approval.
- [ ] Jira ticket updated with test evidence (once a ticket exists — see Notes).

## Notes / open questions
- **FR/AC traceability:** FR-011 → AC-020.
- **Jira story ticket:** not yet created — to be raised once ticket creation is approved.
- Workflows are template-shipped (`.github/workflows/*`) — this story is primarily about enabling and
  verifying them, not hand-rolling CI logic.
- Depends on NG-S01 (a buildable repo) but not on any other feature story.
