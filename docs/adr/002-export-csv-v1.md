# ADR 002: CSV Export & Import

## Status

Updated: 2026-07 (v4 with import support)

## Context

Users need data portability without cloud dependency. Export provides backup/migration; import enables restore.

## Decision

- Export and Import via Storage Access Framework
- Format: `FINAN_CSV_VERSION,<N>` header followed by named sections
- Import supports versions 3+ (backward compatible)
- Single SQLite transaction for atomic import

## CSV Sections (v4)

### Version header
```
FINAN_CSV_VERSION,4
```

### WALLETS
```
id,name,currency_code,is_default,opening_balance_minor,icon
```

### CATEGORIES
```
id,name,icon,type_filter,sort_order,cash_flow_activity
```

### TAGS
```
id,name
```

### MERCHANTS
```
id,name
```

### TRANSFERS
```
id,source_wallet_id,destination_wallet_id,amount_minor,occurred_at,note
```

### TRANSACTIONS
```
id,amount_minor,type,wallet_id,category_id,occurred_at,note,merchant_id,tag_ids,transfer_id,cash_flow_activity,cash_flow_activity_overridden
```

## Version History

| Version | Changes |
|---------|---------|
| 1       | Initial: version header + transaction rows |
| 2       | Added `merchant_id` and `tag_ids` columns |
| 3       | Added `WALLETS` section with `opening_balance_minor` |
| 4       | Added `CATEGORIES`, `TAGS`, `MERCHANTS`, `TRANSFERS` sections; wallet `icon`; transaction `cash_flow_activity` |

## Consequences

- `ExportService` and `ImportService` are pure Java and unit-tested
- Settings provides both export and import buttons
- Import deduplicates entities by name (case-insensitive for categories/tags/merchants)
- Import recalculates wallet balances after completion
