# ADR-0001 — Partition the `trades` table by `trade_date`

- Status: Accepted
- Date: 2026-07-27
- Deciders: ReconX Engineering Team

## Context

`trades` is our highest-volume table in ReconX — expecting ~50,000 trade inserts per day, accumulating ~91 million rows over a 5-year retention window. Almost all daily reconciliation jobs, analyst queries, and EOD report refreshes filter by specific date ranges (e.g. single day or single month). 

An unpartitioned table forces full-table scans for range-based reporting, increases index maintenance overhead, and complicates historical data archiving.

## Decision

Partition the `trades` table by `RANGE` on `trade_date`, with one child partition per calendar month (e.g. `trades_y2026m05`). The composite primary key includes `(id, trade_date)` to satisfy PostgreSQL's partitioning requirement. A `trades_default` partition is maintained to catch out-of-range writes safely.

## Alternatives Considered

1. **Unpartitioned Single Table with B-tree index on `trade_date`**: Rejected due to index size bloat and slow EOD query performance over 91M rows.
2. **Hash Partitioning on `trade_ref`**: Rejected because recon queries filter by date ranges, not hash values; hash partitioning does not support partition pruning for date ranges.

## Consequences

**Positive:**
- Partition pruning eliminates non-target partitions (scanning 1 month instead of 91M rows).
- Archival and purging of old data becomes an efficient DDL `DETACH PARTITION` operation instead of costly `DELETE` statements.
- Indexes per partition remain compact and fit inside memory.

**Negative:**
- Primary key must include `trade_date`, creating a composite key `(id, trade_date)`.
- Global unique constraints across partitions require extra careful schema handling.
