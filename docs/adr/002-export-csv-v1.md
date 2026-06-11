# ADR 002: Export CSV v1 (export-only)

## Status

Accepted

## Context

Users need data portability without cloud dependency. Import deferred to reduce risk.

## Decision

- Export via Storage Access Framework
- Format: `FINAN_CSV_VERSION,1` header + transaction rows (v2 adds `merchant_id` and `tag_ids` columns)
- No import in v1

## Consequences

- `ExportService` is pure Java and unit-tested
- Settings does not copy raw `.db` files
