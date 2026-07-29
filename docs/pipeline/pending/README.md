# Pending decisions

Design decisions that are **analysed but not yet decided**. Each doc states the options, the constraint,
and a recommendation to confirm. Resolve, then fold the outcome into the relevant plan/design doc (or an
ADR if one is adopted) and remove it from here.

| Decision | Status | Gated by |
|---|---|---|
| [Query-API authentication](query-api-authentication.md) (NG-S04 / FR-024) | ⏳ Decision pending | Exposure & day-one consumer (Pattern 1 vs 2) |
| [Attachment `fileName` validation](attachment-filename-validation.md) | ⏳ Decision pending | Where to validate (message vs blob HEAD) + fatal vs fallback |
