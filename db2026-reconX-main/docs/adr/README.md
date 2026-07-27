# Architecture Decision Records (ADRs)

This directory contains the Architecture Decision Records (ADRs) for the ReconX system, formatted according to the Michael Nygard template.

## ADR Prompt Template

To generate consistent ADRs using AI assistance (e.g., Claude), use the following prompt template:

```
You are an enterprise software architect. Write an Architecture Decision Record
(ADR) in the Michael Nygard format (Title, Status, Context, Decision,
Consequences) for the following decision.

System: ReconX, a near-prod trade reconciliation platform.
Stack: PostgreSQL 16, Spring Boot 3, Kafka, React.
Scale: ~50,000 trades/day, 5-year retention, 10 concurrent recon analysts.

Decision to record: <ONE LINE DESCRIBING THE DECISION>

Alternatives we considered: <LIST 2-3>

Constraints / forces: <LIST 2-3>

Format: Markdown, Nygard 5-section template, no fluff. Keep under 300 words.
Include a "Status: Accepted | Date: <YYYY-MM-DD>" line.
```

## Index of ADRs

- [ADR-0001: Partition the `trades` table by `trade_date`](0001-partition-trades-by-date.md)
- [ADR-0002: Use JSONB for flexible instrument metadata](0002-jsonb-metadata.md)
- [ADR-0003: Use GIN index over B-tree for JSONB metadata queries](0003-gin-over-btree.md)
