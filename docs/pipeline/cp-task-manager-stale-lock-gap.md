# Known gap: cp-task-manager has no stale-lock (crash) recovery for in-flight jobs

**Status:** OPEN — upstream dependency gap. Owner: cp-task-manager maintainers.
Raised from the `cp-gov-uk-notify-gateway` Stage 2 design review (2026-07-10).
Linked from `architecture-design.md` as **OQ-4**.

## Summary

cp-task-manager claims a job by setting `worker_id` + `worker_lock_time`, and only ever clears them
via an **in-process** `releaseJob` call (on normal completion or a caught exception). There is **no
reaper** that reclaims a job whose worker died mid-execution. A hard pod crash (SIGKILL / OOM / node
loss) **while a job is locked** therefore strands that job permanently — it is never re-assigned and
its notification is left in a non-terminal state.

This is a **regression from the legacy framework jobstore** cp-task-manager was ported from
(`cp-framework-libraries`): legacy reclaimed stale locks via a **1-hour lease**
(`worker_lock_time < now − 1h`). The port kept the `worker_lock_time` column but **dropped the clause
that reads it**.

## Evidence (cp-task-manager @ `/home/santhosh/Workspace/cp-task-manager`)

- **Assignment picks only unlocked jobs:** `JobsRepository.assignJobsToWorkerBatch` —
  `WHERE worker_id IS NULL ... FOR UPDATE SKIP LOCKED` (`JobsRepository.java:63-81`).
- **Lock cleared only by explicit release:** `releaseJob` = `UPDATE jobs SET worker_id=null,
  worker_lock_time=null WHERE job_id=:jobId` (`JobsRepository.java:168`).
- **Release is in-process only:** `TaskExecutor.run()` releases on the normal path and in the `catch`
  on a graceful exception (`TaskExecutor.java` ~135-160). A hard crash runs neither.
- **`worker_lock_time` is written but never read** — no query filters or reclaims by it (grep across
  `task-manager-service/src/main/java`: none). So there is no lease-timeout / visibility-timeout.
- Poller: `JobExecutor` `@Scheduled(fixedDelayString = "${job.executor.poll-interval:5000}")`
  (`JobExecutor.java:154`).

### Legacy jobstore (the port source) HAD the reclaim — cp-task-manager dropped it
Source: `cp-framework-libraries` @ `/home/santhosh/Workspace/framework-github/cp-framework-libraries`,
`job-manager/jobstore-persistence/.../JobJdbcRepository.java`.
- The legacy lock query **reclaims stale locks**: `LOCK_JOBS_SQL` selects
  `WHERE (worker_id IS NULL OR worker_lock_time < ?) ... FOR UPDATE SKIP LOCKED`
  (`JobJdbcRepository.java:44-54`).
- The threshold is a **1-hour lease**: `lockJobsFor` binds `worker_lock_time < (now − 1 hour)`
  (`final Timestamp oneHourAgo = toSqlTimestamp(now.minusHours(1))`, `JobJdbcRepository.java:133-155`).
  A job locked by a worker that dies is re-picked once its lock is > 1h old.
- cp-task-manager's `assignJobsToWorkerBatch` kept only `worker_id IS NULL` and **omitted the
  `OR worker_lock_time < …` clause** — so `worker_lock_time` is now a **vestigial column** (written on
  lock, nulled on release, never read). A regression introduced by the port.

## Mechanisms checked and ruled out (verified across the whole repo, `main` + tests + SQL)

The stranded lock is **not** recovered by anything else:
- **Scheduled reaper:** only one `@Scheduled` exists — the poller `JobExecutor.checkAndAssignJobs()`
  (`JobExecutor.java:154`). No cleanup/reaper job.
- **Selection queries:** both `findUnassignedJobsWithLimit` (JPQL, `JobsRepository.java:59`) and the
  poller's `assignJobsToWorkerBatch` (native, `:63-81`) filter on `worker_id IS NULL` only; neither
  reads `worker_lock_time`.
- **Startup recovery:** the only startup hook is `TaskRegistry.autoRegisterTasks()`
  `@EventListener(ContextRefreshedEvent)` (`TaskRegistry.java:87`) — it registers task beans, it does
  not touch jobs. No `CommandLineRunner`/`ApplicationRunner`/`InitializingBean` reclaim.
- **Shutdown recovery:** `JobExecutor.destroy()` `@PreDestroy` (`JobExecutor.java:126`) only calls
  `executor.shutdown()`; with `waitForTasksToCompleteOnShutdown` (`TaskManagerAutoConfiguration.java:146`)
  it drains in-flight tasks on a **graceful** stop — but does **not** release locks and does not run on
  a hard crash.
- **DB level:** `worker_lock_time TIMESTAMPTZ` is nullable with **no default and no trigger**
  (`V1__create_jobs_table.sql:9`). No DB-side reclaim.
- **No stable worker identity:** `workerId = UUID.randomUUID()` is generated **fresh each poll cycle**
  (`JobExecutor.java:158`), so an id-based "release my previous locks on restart" strategy is not even
  possible — a **time-based lease is the only viable recovery**, and that is the clause the port dropped.

## Exact failure sequence (confirmed)

1. The poll sets `worker_id` + `worker_lock_time` via `assignJobsToWorkerBatch` — **committed
   immediately** in its own transaction.
2. Execution runs asynchronously on the thread pool in `TaskExecutor.run()` (a **separate**
   transaction). On success → updates + `releaseJob`; on a graceful exception → rollback + `releaseJob`
   in the `catch`.
3. A **hard crash** (SIGKILL / OOM / node loss) during step 2 runs neither the commit nor the `catch`,
   so the lock committed in step 1 **remains set** → the job is never re-selected (`worker_id` not null)
   and `worker_lock_time` is never consulted → **notification stranded non-terminal, permanently.**

Graceful shutdown within the pod's termination grace period is fine (in-flight task completes and
releases). The gap bites on hard kill during a task's (short) execution window — realistic under OOM,
node loss, or a deploy SIGKILL after the grace period, and permanent when it hits.

## Impact

- **Safe (NOT affected):** crash after the consumer commits (notification row + jobs row, with
  `worker_id = NULL`) but before the ASB ack → ASB redelivery + `notification_id` PK dedupe no-op; the
  still-unlocked job is picked up normally. *This was the scenario originally cited for the (now
  removed) reconciliation sweep — it needs no sweep.*
- **The real gap:** crash while a job is **locked and executing** → `worker_id` stays set forever →
  job never re-assigned → notification stuck non-terminal.
- The removed reconciliation sweep would **not** have caught this: it looked for `QUEUED` rows with
  **no** task, but a stranded job **has** a (locked) task row.

## Recommended fix (upstream, preferred)

**Restore the legacy reclaim clause the port dropped:** change `assignJobsToWorkerBatch` to select
`WHERE (worker_id IS NULL OR worker_lock_time < :staleThreshold)` (matching legacy `LOCK_JOBS_SQL`),
with `:staleThreshold = now − leaseTimeout`. Make the lease **configurable** rather than the legacy
hard-coded 1 hour. The lease need only exceed a task's **actual execution time** (short for send/poll),
not the inter-attempt wait — the wait is governed by `assigned_task_start_time` and the lock is held
only during execution, so a modest lease is safe even for the letter long-poll flows. (Equivalently, a
scheduled reaper that nulls `worker_id`/`worker_lock_time` for non-terminal jobs past the threshold
achieves the same result.) This fixes every cp-task-manager consumer and keeps recovery in the library
that owns the `jobs` table.

## Workaround (only if the upstream fix is unavailable in time)

A service-side scheduled job that reclaims stale-locked rows in the co-located `jobs` table by
`worker_lock_time`. Rejected as the primary fix: it reaches into the library's table (coupling) and
every consumer would have to reimplement it.

## Interaction with duplicate-send safety

Independent of this gap, a re-run of a `send-email` task after a crash must not resend the email. That
is handled by **FR-022 / AC-036** (recover-by-reference against Gov.UK Notify), not by lock recovery.

## Tracking

- [ ] Raise a cp-task-manager issue/ticket for the reaper.
- Referenced by `architecture-design.md` (OQ-4) and this design's Handoff Gap Report.
