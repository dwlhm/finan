# ADR 001: minSdk 30

## Status

Accepted

## Context

Play distribution requires balancing reach vs. modern APIs. Target was ≥80% active devices.

## Decision

`minSdk = 30` (Android 11), ~83% cumulative Play coverage.

## Consequences

- No support for Android 10 and below
- `java.time` available without desugaring complexity for date ranges
