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
verified claims audit, orphan lot detection) must traverse the composite DAG
without modifying domain entities. Visitor separates these algorithms from the
data structure.
**Implementation:**
- `LineageVisitor`: Interface defining `visitLeaf(LeafSourceLot)` and `visitComposite(CompositeTransformedLot)`.
- `FootprintRollupVisitor`: Traverses the lineage DAG to aggregate water (L), energy (kWh), and CO2e (kg) per lot and per piece using an `AllocationStrategy`.
- `ClaimRollupVisitor`: Traverses the lineage DAG to verify certified recycled and organic percentages, checking that claims never exceed certified inputs and flagging orphan lots lacking verified source provenance. Returns immutable `ClaimRollupResult`.

## 4. Strategy Pattern — `lineage.domain.strategy`

**Why:** Environmental footprints (water, energy, CO2e) must be apportioned
differently across split and merged lots depending on accounting methodology
(mass-based vs piece-count-based). Strategy permits switching allocation
mechanisms dynamically via configuration or request without modifying rollup visitors.
**Implementation:**
- `AllocationStrategy`: Common interface defining `calculateRatio(...)` and `allocate(...)`.
- `MassAllocationStrategy`: Allocates footprints normalized by mass in kilograms (KG).
- `PieceCountAllocationStrategy`: Allocates footprints normalized by piece counts (PIECES / CARTONS).
- `AllocationStrategyRegistry`: Resolves active strategy bean dynamically by name.

## 5. Builder Pattern — `passport.domain.builder`

**Why:** A Digital Product Passport (DPP) aggregates data from multiple bounded
contexts (lineage, evidence, registry). The Builder pattern provides a fluent,
step-by-step construction API that enforces mandatory fields at compile time.

## 6. Proxy Pattern — `passport.proxy`

**Why:** DPP generation is expensive (aggregation, QR-code rendering). A caching
proxy defers and caches the full passport, returning a lightweight summary for
list views.

## 7. Observer Pattern — `evidence.observer`

**Why:** When compliance certificates or test reports expire or approach
expiry, downstream consumers (claim invalidation, lot quarantine, audit alerts)
must react automatically without tight coupling to the evidence persistence layer.
Observer decouples the evidence lifecycle from downstream business actions.
**Implementation:**
- `EvidenceExpiredEvent` & `EvidenceExpiringSoonEvent`: Immutable domain events published when certificates expire or enter the 30-day renewal window.
- `EvidenceExpiryScheduler`: Periodically evaluates certificate dates against the current date and publishes expiry events via Spring's `ApplicationEventPublisher`.
- `EvidenceEventListener`: Listens for `EvidenceExpiredEvent` and automatically revokes certified claims and transitions affected lots to `QUARANTINED` status.

## 8. CQRS (Command-Query Responsibility Segregation) — `passport.projection`

**Why:** The passport read model is a denormalised projection of lineage +
evidence data, optimised for fast reads. Separating the read path from the write
path allows independent scaling and simpler query logic. Enforced by ArchUnit:
`passport` cannot modify `lineage` entities.

## 9. Registry Pattern — `registry`

**Why:** Centralises factory/material/supplier lookup, acting as a single source
of truth for reference data shared across bounded contexts.
