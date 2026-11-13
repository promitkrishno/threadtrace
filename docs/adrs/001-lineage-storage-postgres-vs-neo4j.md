# ADR 001: Lineage Storage — PostgreSQL vs Neo4j

## Status

Accepted

## Date

2026-11-13

## Context

ThreadTrace requires storing garment supply-chain lineage data, which forms a
directed acyclic graph (DAG) of process steps, material transformations, and
custody transfers. We evaluated two storage options:

1. **PostgreSQL** with recursive CTEs and adjacency list / LTREE for
   hierarchical queries
2. **Neo4j** as a native graph database optimised for traversal-heavy workloads

### Decision Drivers

- Team familiarity and operational burden
- Query complexity for typical lineage depths (≤ 20 hops)
- Hosting cost on an 8 GB RAM development laptop
- Integration with Spring Data JPA and existing relational models (evidence,
  certificates, passports)

## Decision

**Use PostgreSQL 16** with an adjacency-list model augmented by materialised
paths for lineage queries.

## Rationale

- Lineage depth in garment supply chains is bounded (typically 5–15 steps),
  making recursive CTEs performant.
- PostgreSQL eliminates the operational overhead of a second database engine.
- JSONB columns can store flexible step metadata without schema migrations.
- Spring Data JPA and Flyway provide mature tooling for PostgreSQL.
- A future migration to Neo4j remains possible via the Strategy pattern in
  `lineage.domain.strategy`.

## Consequences

- **Positive:** Single database, simpler deployment, lower memory footprint.
- **Negative:** Deep graph traversals (>50 hops) may require denormalisation or
  caching.
- **Risks:** If supply-chain complexity grows significantly, revisit with Neo4j
  or Apache AGE extension.
