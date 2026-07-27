# ADR-0002 — Use JSONB for flexible instrument metadata

- Status: Accepted
- Date: 2026-07-27
- Deciders: ReconX Engineering Team

## Context

Financial instruments span diverse asset classes (Equities, Fixed Income, FX, Derivatives, Commodities). Each asset class carries distinct attributes (e.g. `sector` and `exchange` for Equities; `tenor` and `coupon` for Bonds; `underlying` and `contractSize` for Futures). 

Adding sparse SQL columns for every asset class attribute leads to wide, mostly-null tables and frequent schema migration DDL whenever new instrument attributes are introduced.

## Decision

Add a binary JSON column (`metadata JSONB NOT NULL DEFAULT '{}'::jsonb`) to the `instruments` table. All asset-class specific, secondary, or evolving metadata attributes are stored within this JSONB document.

## Alternatives Considered

1. **Entity-Attribute-Value (EAV) Table**: Rejected due to complex JOIN queries and poor query readability when fetching instrument details.
2. **Class-per-Table Inheritance / Separate Attribute Tables**: Rejected due to high schema complexity and multiple table joins needed per query.
3. **Plain Text `JSON` Column**: Rejected because text `JSON` requires re-parsing on every query and cannot be efficiently indexed.

## Consequences

**Positive:**
- Schema flexibility allows adding new instrument attributes without executing database migrations.
- Binary storage (`JSONB`) avoids redundant parsing and supports indexing.
- Simplifies the `instruments` table definition to core universal fields.

**Negative:**
- Application layer must enforce schema validation for JSON payloads.
- Typing is dynamic inside JSON fields, requiring careful query construction.
