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

**Why:** A Digital Product Passport (DPP) aggregates heterogeneous data from multiple bounded
contexts (lineage DAG, evidence registry, party reference data, footprint rollups). Construction
involves optional and mandatory sections, dynamic custom fields (QA5), and versioned persistence.
The Builder pattern provides a fluent, step-by-step construction API that enforces mandatory fields
at compile time, decouples DPP assembly from underlying domain entities, and handles QR code generation.
**Implementation:**
- `DigitalPassportBuilder`: Fluent builder constructing versioned `DigitalPassportEntity` instances. Configures style, shipment, brand, root carton lot, fibre composition, process step origins, verified certification badges, care/repair instructions, and environmental footprints (water, energy, CO2e).
- Dynamic Custom Fields: Supports arbitrary key-value custom attributes (`withCustomField(key, value)`) serialized to JSONB, satisfying QA5 extensibility requirements without requiring schema or domain entity modifications.
- `QrCodeGenerator`: Integrates Google ZXing (`QRCodeWriter`) to generate PNG QR code byte arrays and cryptographic verification tokens (`qrToken`) linking to the public passport web page.

## 6. Proxy Pattern & Audience Security Views — `passport.proxy`

**Why:** Digital product passports serve multiple audiences with conflicting visibility and security
requirements. Consumers require public traceability without supplier confidentiality leaks (FR8, QA3);
brands require visibility into their own supply chains but must never see competitor data (tenant isolation);
auditors require unrestricted access to raw mass-balance flags, worker counts, and unit prices.
The Proxy pattern (`AudiencePassportProxy`) intercepts retrieval requests, performs role-based authorization
and tenant ownership checks, and returns audience-tailored projections without polluting domain logic.
**Implementation:**
- `PassportAccessPort`: Target interface exposing `getPublicPassport(qrToken)`, `getBrandPassport(qrToken, brandCode)`, and `getAuditorPassport(qrToken)`.
- `AudiencePassportProxy`: Implements `PassportAccessPort`. Intercepts calls, validates caller credentials and authorities (`ROLE_BRAND`, `ROLE_AUDITOR`), verifies multi-tenant brand ownership (returning HTTP 403 Forbidden on cross-brand access), and guarantees zero confidential field leakage (`unit_price_bdt`, `exact_address`, `worker_count`) to public consumers.
- Offline Resilience: Integrates with `PassportCacheService` to serve cached views with ETag support and <2s fallback during database unavailability (QA4).

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

**Why:** The passport read model is a denormalised projection of lineage DAG +
evidence documents, optimised for ultra-fast, high-throughput consumer reads (QR code scans).
Separating the read path (`passport.projection`) from the transactional write path (`lineage.application`)
allows independent caching (ETags, Caffeine), distinct serialization rules, and guarantees that
read operations cannot mutate lineage state. Enforced by ArchUnit: `passport` cannot modify
or directly access `lineage.domain` or `lineage.infrastructure` internals.
**Implementation:**
- `PublicPassportView`: Immutable consumer-facing projection exposing only public provenance, certified badge percentages, environmental footprints, and care instructions. Strictly excludes confidential supplier pricing, worker headcounts, and precise GPS/facility addresses.
- `BrandPassportView`: Tenant-scoped projection for brand managers displaying facility tier relationships, lot numbers, and supplier identities for their own contracted supply chains.
- `AuditorPassportView`: Full audit projection providing unredacted mass-balance logs, flag reasons, worker counts, unit prices in BDT, and complete backward/forward lineage trees.

## 9. Registry Pattern — `registry`

**Why:** Centralises factory/material/supplier lookup, acting as a single source
of truth for reference data shared across bounded contexts.

## 10. Adapter Pattern — `bulkimport.adapter`

**Why:** Garment manufacturing facilities and cut-and-sew factories use disparate legacy systems,
exporting production data in heterogeneous file formats (CSV cutting table reports, Excel packing lists,
proprietary ERP exports). The Adapter pattern converts these heterogeneous third-party input formats
into uniform domain commands (`RecordEventCommand`), isolating core traceability logic from external file formats.
**Implementation:**
- `LotImportRowAdapter<T>`: Target interface defining `supports(ImportType)` and `adapt(T rawRow, ImportContext context) -> RecordEventCommand`.
- `CsvCuttingReportAdapter`: Adapts CSV cutting log rows (columns: `lot_number`, `parent_lot_number`, `cut_pieces`, `fabric_used_kg`, `waste_kg`, `cutting_table_id`) into standardized cutting transformation events.
- `ExcelPackingListAdapter`: Utilizes Apache POI to parse Microsoft Excel spreadsheets (.xlsx) containing carton packing lists into standardized packing and aggregation lot events.
- Error Isolation: Adapters perform syntactical and semantic row-level validation (`ImportRowValidationException`), enabling `BulkImportService` to report row-level rejection details (line number, failing column, rejection reason) without terminating batch processing.
