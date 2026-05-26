# ADR 003: Package by screen

## Status

Accepted

## Context

Capture screen has many UI concerns; flat `ui/` does not scale.

## Decision

`ui/<screen>/` holds Activity, adapters, and screen-local controllers.

## Consequences

- Easier to locate screen-specific code
- Shared wiring via `ui/common/AppServices` and `FinanApplication`
