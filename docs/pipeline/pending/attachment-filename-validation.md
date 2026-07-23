# Attachment `fileName` contract — where to validate it

**Status:** ⏳ **Decision pending** — options analysed, approach not yet chosen. Held in
`docs/pipeline/pending/`; resolve before the mi-reportdata producer flips its Service Bus toggle ON.

**Scope:** Producers (mi-reportdata and any future client) upload the attachment to Azure Blob and set
the original filename as blob metadata `fileName` (`x-ms-meta-fileName`); this service reads it off the
downloaded blob and never scrapes the `fileUri` path. The open question is **how to validate that
contract upfront** — and specifically whether a violation can be dead-lettered.

## The constraint

The DLQ decision exists **only** in `SendEmailConsumer`, and only *before* `context.complete()`:

```
SendEmailConsumer.processMessage()      ← holds ServiceBusReceivedMessageContext
  ├─ deserialise            → bad JSON        → context.deadLetter()
  ├─ validator.validate()   → bean violations → context.deadLetter()
  └─ ingest() → persist row + enqueue job → context.complete()   ← ASB message GONE here
                                   │
                                   ▼ (later, DB-driven, no ASB context)
                          SendEmailTask.execute()
                            └─ download blob → send
                               only outcomes: retry (INPROGRESS) | markFailed
```

Once `ingest()` completes the message, cp-task-manager owns the work via the DB. `SendEmailTask` has **no**
`ServiceBusReceivedMessageContext`, so by construction it can only *retry* or *markFailed* — never DLQ.
A missing `fileName` discovered at download time (in `SendEmailTask`) can therefore at best become a
`FAILED` notification row (via the `PermanentBlobException` path); it can never be dead-lettered.

**Principle:** validate where the delivery decision still lives — the consumer boundary. The tension is
that with the metadata contract the value to validate lives on the **blob**, not in the **message**, so the
boundary can't see it without touching storage.

## Options

| | Where | How | DLQ? | Cost |
|---|---|---|---|---|
| **A. Name in the message** | `SendEmailConsumer` | Add `fileName` to `SendEmailCommand`; bean `Validator` gates it (cross-field: required iff `fileUri` present) | Yes — existing `deadLetter()` path, no I/O | Revisits the metadata contract; name travels in the message, not (only) on the blob |
| **B. HEAD the blob at consume** | `SendEmailConsumer` / ingest | `getProperties()` before `complete()`; reuse the permanent/transient classifier → missing metadata = `deadLetter`, 5xx = `abandon` | Yes | Extra HEAD per message **and couples the ingest path to blob availability** |
| **C. Producer contract test** | build time | Consumer-driven / integration test asserting the producer sets `x-ms-meta-fileName`; shared upload helper | No (prevents, doesn't reject) | Cheapest; catches before runtime |
| *(current)* markFailed | `SendEmailTask` | `PermanentBlobException` → `FAILED` row + reason | No — app-level sink, not transport DLQ | Already implemented |

## Recommendation (to confirm) — layered

1. **Always do C.** A producer contract test + shared upload helper stops the violation ever reaching a
   queue. This is the real "upfront".
2. **For the runtime gate, prefer A over B.** The DLQ-timing constraint is new information that wasn't
   weighed when the metadata contract was chosen, and it argues for the name being in the message:
   - A validates with **zero I/O**, reuses the DLQ path that already exists, and keeps the ingest path
     blob-independent.
   - B's hidden cost is coupling: today ingestion is pure DB and only the *task* touches blob, so a blob
     outage just makes tasks retry. A HEAD in the consumer makes *ingestion* abandon → messages redeliver
     and pile up. It also touches the blob twice (HEAD at consume, GET at send).
   - A is **not** the brittle URI-parsing that was rejected earlier — an explicit `fileName` message field
     is a first-class contract value, not a guess. Blob metadata may be kept as well (accept dual-source
     drift risk) or dropped in favour of the message field.
   - Choose B only if the hard requirement is "the name must live solely on the blob (mirror legacy
     FileService semantics)". Then classify carefully: existing-blob-but-no-`fileName` = permanent →
     `deadLetter`; 5xx/timeout = transient → `abandon`.

## Open question to settle first — fatal vs fallback

Should a missing `fileName` even be fatal? The alternative is a **soft fallback**: send with a default name
(e.g. `attachment` + extension from content-type), log/alert, and let delivery succeed. That sidesteps the
DLQ-timing issue entirely — nothing to reject, and the breach surfaces via monitoring rather than a parked
message. Pick by how much correctness rides on the name: it drives the email's attachment filename **and
GOV.UK Notify CSV detection** (`GovNotifyClient.isCsv`). If CSV detection must be correct → hard-fail via A;
if the name is largely cosmetic → soft-fallback may be the pragmatic call.

## References

- `SendEmailConsumer` — ASB consume/validate/deadLetter/complete boundary.
- `SendEmailTask` — cp-task-manager-driven send; `PermanentBlobException` → `markFailed` (no DLQ here).
- `AttachmentDownloader` — reads bytes + `fileName` metadata from the blob.
- Producer-side contract: platform-engineering-knowledge-base
  `features/notification-notify-mbd-rewrite/design-and-planning/implementation-plan-mireportdata.md` §2.
