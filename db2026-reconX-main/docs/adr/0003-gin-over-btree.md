# ADR-0003 — Use GIN index over B-tree for JSONB metadata queries

- Status: Accepted
- Date: 2026-07-27
- Deciders: ReconX Engineering Team

## Context

With instrument metadata stored in a `metadata JSONB` column (ADR-0002), reconciliation filtering queries frequently perform containment lookups such as `WHERE metadata @> '{"sector":"Technology"}'`. Sequential table scans over unindexed JSONB columns become unacceptably slow as the dataset grows.

## Decision

Create a Generalized Inverted Index (GIN) using the `jsonb_path_ops` operator class on `instruments(metadata)`:
`CREATE INDEX idx_instruments_metadata_gin ON instruments USING GIN (metadata jsonb_path_ops);`

## Alternatives Considered

1. **Standard B-tree Index on Expression**: e.g., `CREATE INDEX ON instruments ((metadata->>'sector'))`. Rejected because it only indexes a single specific key rather than arbitrary JSON containment paths.
2. **Default GIN (`jsonb_ops`)**: Rejected because `jsonb_ops` indexes both keys and values separately, resulting in larger index size and slower containment lookups compared to `jsonb_path_ops`.

## Consequences

**Positive:**
- Extremely fast containment searches (`@>`) using index scans instead of full table sequential scans.
- `jsonb_path_ops` produces smaller index footprints than default `jsonb_ops`.

**Negative:**
- Higher write overhead during `INSERT`/`UPDATE` operations on `instruments`.
- `jsonb_path_ops` does not support key-existence operators (`?`, `?|`, `?&`), only containment queries (`@>`).
