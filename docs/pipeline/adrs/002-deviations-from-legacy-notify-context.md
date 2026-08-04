# ADR 002 — Deviations from the legacy `cpp-context-notification-notify` context

**Status:** Accepted (living register — append rows as deviations are found)

## Context

`cp-notification-gateway` is the Modern-by-Default (Spring Boot) replacement for the legacy
WildFly/CQRS context service `cpp-context-notification-notify`. It is a re-platforming, not a
re-specification, so the default expectation is behavioural parity with legacy. Where the new
service **intentionally** or **structurally** diverges from legacy behaviour, we record it here
so reviewers can tell a deliberate change from an accidental regression, and so the parity gaps
introduced by the framework swap are visible in one place.

The two framework swaps that drive most deviations:

- **Messaging/eventing:** legacy CQRS command/event bus over Artemis (`uk.gov.moj.cpp.*`
  framework, RAML-defined command & query APIs) → Spring Boot + Azure Service Bus.
- **Async job scheduling:** legacy jobstore (`uk.gov.moj.cpp.jobstore`) → new
  `cp-task-manager` (`uk.gov.hmcts.cp.taskmanager`). The two have **different retry SPIs**
  (see D2).

## Deviation register

| # | Area | Legacy behaviour | New behaviour | Deliberate? | Justification / risk | References |
|---|------|------------------|---------------|-------------|----------------------|------------|
| D1 | **Send-notification ingress contract** | Synchronous REST **command API** (`notificationnotify-command-api.raml`, `POST /notifications/{notificationId}`) fronting an async JMS command handler | **Asynchronous Azure Service Bus** message consumer (`SendEmailConsumer`); no REST command endpoint. Query side stays REST **GET** (`NotificationQueryController`) in both | Yes | Fits the MbD/Service-Bus platform; decouples callers from synchronous availability. Risk: callers that expected a synchronous command ack must adopt fire-and-forget + query-for-status | new: `messaging/SendEmailConsumer.java`, `messaging/ServiceBusConfig.java`, `web/NotificationQueryController.java`; legacy: `notificationnotify-command/notificationnotify-command-api/src/raml/notificationnotify-command-api.raml` |
| D2 | **Delivery-status retry exhaustion** | `RetryHandler` pre-applies `permanentFailureHandler` as the jobstore **exhaust task**, so when configured retries run out the notification **terminalises to permanent failure** | New `cp-task-manager` SPI has **no exhaust-task hook**; `CheckEmailStatusTask.retry()` returns `INPROGRESS + shouldRetry` only. On exhaustion `TaskExecutor` re-persists the same task with `retryAttemptsRemaining=0` and **never terminalises** → notification stuck `QUEUED` and the poll **hot-loops** GOV.UK Notify | **No — regression** | Behaviour silently lost in the jobstore→task-manager migration. **Fix:** add an in-task exhaustion handler that blindly marks failed when `retryAttemptsRemaining` is exhausted, restoring legacy's terminal end-state | legacy: `.../task/handlers/RetryHandler.java`; new: `task/CheckEmailStatusTask.java:116-121`, `cp-task-manager/.../executor/TaskExecutor.java:227-252` |
| D3 | **Delivery-status classification (eager failure)** | Only `{technical-failure, permanent-failure, virus-scan-failed}` are `FAILED`; everything else (incl. `temporary-failure`, `validation-failed`, `not found`, unknown→`unexpected`) is treated as **in-progress and re-polled** | Same classification **changed** (implemented): in-progress is defined **positively** as `{accepted, created, sending, received, pending-virus-check}` (+ `delivered` = success); **all** other statuses are terminal failure. Paired with an in-task exhaustion handler (see D2) that marks failed once status-check retries are spent | **Yes — deliberate divergence** | GOV.UK Notify docs: `temporary-`/`permanent-failure` are Notify's final verdict on a message (Notify already retried up to 72h during `sending`); re-**polling** an existing id can never flip to `delivered`. Unknown→`unexpected` re-polling is meaningless. Divergence reaches legacy's eventual end-state (failure) **eagerly** and closes the D2 hot-loop for these statuses. "Resend on temporary/technical failure" is a separate feature legacy also lacked | new: `sender/NotificationStatus.java`, `task/CheckEmailStatusTask.java:68-84`; legacy: `.../client/NotificationStatus.java`, `.../task/processors/EmailStatusResponseProcessor.java`; [Notify statuses](https://docs.notifications.service.gov.uk/rest-api.html) |
| D4 | **Attachment reference: `materialUrl` + `fileId` → `fileUri`** | Attachment referenced by **`fileId`** (+ `materialUrl`) — a centralised file-service identifier the service resolves to fetch the content | New BYO-filestore model (FR-004): attachment referenced by **`fileUri`**, a direct Azure Blob URI the attachment downloader addresses via managed identity (no SAS). The attachment filename is derived from the blob name in that URI (`EmailSender.filenameFrom` via `BlobUrlParts.parse(fileUri).getBlobName()`, then last path segment) | Yes — permanent | Fits the MbD BYO-filestore platform; direct blob addressing replaces the centralised file-service. Filename derivation reuses the same `BlobUrlParts` parser already used by `BlobClientFactory` on the same URI, so it is URL-decoded and query/SAS-safe | new: `contracts/command-send-email-notification.schema.json` (MVP delta note), `contracts/command-send-email-notification-legacy.schema.json` (`fileId`), `command/SendEmailCommand.java` (`fileUri`), `sender/EmailSender.java`, `blob/BlobClientFactory.java` |
| D5 | **Blob host allow-list (SSRF / Managed-Identity-token guard)** | Endpoint + container were **JNDI-pinned** (`azure.filestore.endpoint` / `container-name`); nothing on the wire could redirect the MI token, so no host check existed | The wire-borne `fileUri` (D4) carries scheme/host/container/blob. `BlobClientFactory` now validates **`https` + host ∈ configured allow-list** (`cp.notification.blob.allowed-hosts`, set per env/stack in the Helm overlay) **before** attaching a token, rejecting anything else as `DisallowedBlobHostException` (terminal, no retry). Empty list ⇒ **fail-closed** in Workload-Identity mode; connection-string/local (Azurite) mode is exempt | **Yes — new control** | Legacy needed no equivalent because the account was config-pinned; carrying a full URI on the queue introduces an SSRF + MI-token-exfil surface, so the host must be constrained. No behaviour change for valid CP `fileUri`s | new: `blob/BlobHostValidator.java`, `blob/DisallowedBlobHostException.java`, `blob/BlobClientFactory.java`; legacy: `referencedata .../blobstore/AzureFileStoreBlobContainerClientProducer.java` (JNDI-pinned endpoint + container), `.../ReferenceDataFileInterceptor.java` (UUID blob name) |

## GOV.UK Notify status contract (basis for D3 — why the deviation is safe)

Our `CheckEmailStatusTask` **re-`GET`s the delivery status of an already-sent notification id**
(`GET /v2/notifications/{id}`). It does **not** resend. That distinction is what makes eager
failure safe. The [GOV.UK Notify REST API](https://docs.notifications.service.gov.uk/rest-api.html)
defines the email delivery statuses as:

| status | documented meaning | in-flight? | our handling |
|--------|--------------------|-----------|--------------|
| `created` | queued at Notify, ready to send to the provider | **yes** — re-poll | in-progress |
| `sending` | sent to the provider; the provider will attempt delivery **for up to 72h** | **yes** — re-poll | in-progress |
| `delivered` | delivered to the recipient | no | success → `markSent` |
| `permanent-failure` | the email address does not exist; remove it from your DB | no — **Notify's final verdict** | fail → `markFailed` |
| `temporary-failure` | not delivered (full inbox / anti-spam) after the sending window | no — **Notify's final verdict** (it already retried up to 72h during `sending`) | fail → `markFailed` |
| `technical-failure` | a problem between Notify and the provider; "you'll have to try sending your messages again" | no — needs a **new send**, not a re-poll | fail → `markFailed` |
| 404 `NoResultFound` | unknown notification id **or** past the 7-day retention window | no | fail → `markFailed` |
| _(anything else)_ | not part of the contract | n/a | `fromStatus` → `UNEXPECTED_FAILURE` → fail |

**Why re-polling the non-in-flight statuses is futile.** Notify performs its own delivery
retries (up to 72h) *while the status is `sending`*. Once it settles on
`temporary-`/`permanent-failure`, that is its final answer for **that** notification id — a
subsequent `GET` of the same id will never return `delivered`. The docs' "try sending again"
advice (for `technical-failure`, and inbox-full `temporary-failure`) means **dispatch a new
notification** — an operation this status-poll task cannot and must not perform. Therefore, for
the poll, every status that is not `delivered` and not genuinely in-flight
(`created/sending/accepted/received/pending-virus-check`) is terminal.

**Safety of the deviation (D3):** classifying these as terminal changes *when* we conclude
failure, not *whether* — legacy also ended these notifications in failure, just slowly (via retry
exhaustion, per D2) or, in the ported code, never (the D2 hot-loop). Eager failure reaches the
same terminal outcome immediately, with an accurate reason, and — crucially — is what lets D2's
in-task exhaustion handler apply only to the genuinely in-flight statuses. No notification that
legacy would have delivered is failed by this change, because Notify's own delivery attempts are
already complete before any of these statuses is returned. Defining in-progress as a **positive
allow-list** also means any future/unknown Notify status fails safe (terminal) rather than
re-polling forever.

## Consequences

- **D2 must be fixed** to restore parity — it is a regression, not an intended change.
- **D3 is an accepted, documented divergence** — reviewers should not "restore legacy parity"
  by reverting it. It is correct on the GOV.UK Notify contract and is what makes D2's fix
  effective for terminal statuses.
- **D1** is an accepted platform-level change; downstream integrators must move to the
  fire-and-forget + query-for-status model.
- New deviations discovered in later reviews should be appended as further rows rather than
  scattered across PR comments.
