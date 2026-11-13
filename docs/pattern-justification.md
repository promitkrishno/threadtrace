# ThreadTrace — Design Pattern Justification

This document explains each GoF / architectural pattern used in the project,
why it was chosen, and where it is implemented.

---

## 1. State Pattern — `lineage.domain.state`

**Why:** Process steps (RECEIVE → CUT → SEW → … → SHIP) form a finite state
machine. The State pattern encapsulates step-specific validation and transition
logic, preventing a monolithic switch/case in the domain service.

## 2. Composite Pattern — `lineage.domain.composite`

**Why:** A garment's bill-of-materials is a tree: a shirt comprises a body
panel, sleeves, collar, each of which may comprise sub-materials. Composite lets
us treat individual materials and assemblies uniformly for quantity and cost
aggregation.

## 3. Visitor Pattern — `lineage.domain.visitor`

**Why:** Multiple cross-cutting operations (carbon footprint calculation,
compliance audit, lineage export) must traverse the composite tree without
modifying domain entities. Visitor separates these algorithms from the data
structure.

## 4. Strategy Pattern — `lineage.domain.strategy`

**Why:** Lineage queries may use different algorithms (recursive CTE,
materialised path, or future graph DB). Strategy allows swapping implementations
at configuration time without touching business logic.

## 5. Builder Pattern — `passport.domain.builder`

**Why:** A Digital Product Passport (DPP) aggregates data from multiple bounded
contexts (lineage, evidence, registry). The Builder pattern provides a fluent,
step-by-step construction API that enforces mandatory fields at compile time.

## 6. Proxy Pattern — `passport.proxy`

**Why:** DPP generation is expensive (aggregation, QR-code rendering). A caching
proxy defers and caches the full passport, returning a lightweight summary for
list views.

## 7. Observer Pattern — `evidence.observer`

**Why:** When evidence (certificates, lab reports, photos) is uploaded or
expires, multiple subsystems (passport invalidation, notification, audit log)
must react. Observer decouples the evidence lifecycle from downstream consumers.

## 8. CQRS (Command-Query Responsibility Segregation) — `passport.projection`

**Why:** The passport read model is a denormalised projection of lineage +
evidence data, optimised for fast reads. Separating the read path from the write
path allows independent scaling and simpler query logic. Enforced by ArchUnit:
`passport` cannot modify `lineage` entities.

## 9. Registry Pattern — `registry`

**Why:** Centralises factory/material/supplier lookup, acting as a single source
of truth for reference data shared across bounded contexts.
