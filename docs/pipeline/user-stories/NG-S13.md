# [NG-S13] Observability: structured logs, metrics, and DLQ-depth monitoring

## User story
As a **support/operations engineer**,
I want **structured JSON logs with correlation/request IDs, Micrometer metrics, and DLQ-depth
monitoring on the inbound and result queues**,
so that **I can monitor, trace, and triage the service's behaviour in production without ambiguity**.

## Background
Standalone cross-cutting story, own milestone (Milestone 7). Independent of the feature stories'
internal logic — it instruments whatever exists, so it can be developed in parallel once the basic
consumer/task/result-event code paths exist to instrument (NG-S02/NG-S03).

## Acceptance criteria
- [ ] AC-037: Given the running service, when logs are emitted, then they are valid JSON to stdout via
  logstash-logback-encoder, with `correlationId` + `requestId` MDC fields; root level INFO.
- [ ] AC-038: Given DLQ activity, when messages accumulate, then DLQ depth is exposed as a monitored
  metric (on both the inbound command queue and any result queue).

## NFR links
- NFR-007 (Observability — logging): every log line valid JSON; template logback config unmodified
  without an ADR.
- NFR-008 (Observability — metrics/tracing): Micrometer/actuator metrics (`/actuator/prometheus`) and
  OpenTelemetry tracing to Azure Monitor; DLQ depth monitored; metrics + traces present for send +
  publish paths.

## Out of scope for this story
- A dedicated alerting/paging configuration on top of the exposed metrics — not covered by any FR/AC in
  this backlog; would need a new requirement if wanted.
- The dropped-`NotificationMonitor`-failures counter is **in scope** (see below) since it is explicitly
  named in FR-023, not out of scope.

## Definition of done
- [ ] Code reviewed and approved.
- [ ] AC-037/AC-038 covered by automated tests (log-format assertion; DLQ-depth gauge present in
  `/actuator/prometheus` output).
- [ ] A metric/log counter for dropped `NotificationMonitor` failures is implemented (the legacy monitor
  events are dropped, not re-published — FR-023 requires this be observable, not silent).
- [ ] No critical or high Snyk findings introduced.
- [ ] Zero PII/secrets/tokens/connection strings/case references in any log line (NFR-004) — explicit
  review item, not just an automated check.
- [ ] Deployed to and verified on sandbox / STE.
- [ ] Jira ticket updated with test evidence (once a ticket exists — see Notes).

## Notes / open questions
- **FR/AC traceability:** FR-023 → AC-037, AC-038.
- **Reference-only Jira key:** FR-023 has **no** Jira key reserved yet in requirements.md's Jira column
  (blank). Vertical-slice ticket for NG-S13 will be created later, once the user approves ticket
  creation.
- Depends on NG-S02/NG-S03 existing to have send/publish paths worth instrumenting, but the
  instrumentation code itself (logback config, Micrometer wiring, DLQ gauge) can be built in parallel.
