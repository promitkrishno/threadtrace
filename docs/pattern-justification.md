# ThreadTrace — Design Pattern Justification

This document details every GoF (Gang of Four) and architectural design pattern implemented in ThreadTrace, mapping each pattern to its exact requirement, participating Java classes, and rejected architectural alternatives.

---

## Pattern Summary Matrix

| Pattern | Category | Requirement Served | Participating Java Classes (100% Codebase Match) | Alternative Rejected & Rationale |
| :--- | :--- | :--- | :--- | :--- |
| **State Pattern** | GoF Behavioral | **FR2, QA2**: Lot process lifecycle & mass-balance state transitions | • `lineage.domain.state.LotState`<br/>• `lineage.domain.state.ActiveLotState`<br/>• `lineage.domain.state.ConsumedLotState`<br/>• `lineage.domain.state.QuarantinedLotState`<br/>• `lineage.domain.state.ShippedLotState`<br/>• `lineage.domain.state.LotStateMachine` | **Monolithic Switch/Case in Service**: Rejected due to high cyclomatic complexity, brittle state mutation, and violation of the Open-Closed Principle when adding process steps. |
| **Composite Pattern** | GoF Structural | **FR3, QA1**: Recursive supply-chain DAG of materials, assemblies, and cartons | • `lineage.domain.composite.LineageNode`<br/>• `lineage.domain.composite.LeafSourceLot`<br/>• `lineage.domain.composite.CompositeTransformedLot` | **Flat Tabular Graph / Adjacency Map**: Rejected because traversing and aggregating bill-of-materials hierarchies uniformly requires scattered `if-else` type-checking. |
| **Visitor Pattern** | GoF Behavioral | **FR4, FR6, QA2**: Decoupled carbon/water rollups and claim integrity audits | • `lineage.domain.visitor.LineageVisitor`<br/>• `lineage.domain.visitor.FootprintRollupVisitor`<br/>• `lineage.domain.visitor.ClaimRollupVisitor` | **Direct Methods on Entities**: Rejected because embedding emission calculations and orphan audits into `LotEntity` pollutes domain entities with reporting algorithms and violates Single Responsibility. |
| **Strategy Pattern** | GoF Behavioral | **FR6, QA5**: Dynamic environmental footprint allocation (Mass vs. Piece-Count) | • `lineage.domain.strategy.AllocationStrategy`<br/>• `lineage.domain.strategy.MassAllocationStrategy`<br/>• `lineage.domain.strategy.PieceCountAllocationStrategy`<br/>• `lineage.domain.strategy.AllocationStrategyRegistry` | **Hardcoded Math Branches**: Rejected because apparel brands require switching dynamically between mass (KG) and piece count without modifying visitor traversal logic. |
| **Builder Pattern** | GoF Creational | **FR7, QA5**: Construction of versioned DPPs with dynamic custom fields and QR codes | • `passport.domain.builder.DigitalPassportBuilder`<br/>• `passport.domain.builder.DigitalPassportEntity`<br/>• `passport.domain.builder.QrCodeGenerator` | **Telescoping Constructors**: Rejected due to high parameter count (>15 attributes), rigid construction orders, and inability to support arbitrary dynamic custom fields (QA5). |
| **Proxy Pattern** | GoF Structural | **FR8, QA3, QA4**: Audience security views, tenant isolation, and offline cache fallback | • `passport.proxy.PassportAccessPort`<br/>• `passport.proxy.AudiencePassportProxy`<br/>• `passport.web.PassportCacheService` | **Jackson `@JsonView` on Entities**: Rejected because `@JsonView` cannot enforce multi-tenant 403 access control, easily leaks fields on developer error, and cannot provide offline cache fallback. |
| **Observer Pattern** | GoF Behavioral | **FR5, QA2**: Decoupled certificate expiry alerts, claim invalidation, and quarantining | • `evidence.observer.EvidenceExpiredEvent`<br/>• `evidence.observer.EvidenceExpiringSoonEvent`<br/>• `evidence.observer.EvidenceExpiryScheduler`<br/>• `evidence.observer.EvidenceEventListener` | **Synchronous Cross-Table Updates**: Rejected because tightly coupling document persistence to lot state machines creates cyclic dependencies and blocks adding future alert channels (e.g. email/webhooks). |
| **Adapter Pattern** | GoF Structural | **FR9, QA5**: Ingestion of heterogeneous factory CSV cutting logs and Excel packing lists | • `bulkimport.adapter.LotImportRowAdapter`<br/>• `bulkimport.adapter.CsvCuttingReportAdapter`<br/>• `bulkimport.adapter.ExcelPackingListAdapter`<br/>• `bulkimport.adapter.ImportRowValidationException` | **Monolithic File Upload Controllers**: Rejected because it duplicates validation rules, tightly couples HTTP endpoints to Apache POI/CSV readers, and prevents structured row-level rejection reporting. |

---

## Detailed Pattern Analyses

### 1. State Pattern — `lineage.domain.state`

**Why:** A production lot transitions through a strict lifecycle (`ACTIVE` $\to$ `CONSUMED` $\to$ `QUARANTINED` $\to$ `SHIPPED`). The State pattern encapsulates state-specific invariants:
- A lot in `CONSUMED` status cannot record new transformation events.
- A lot in `QUARANTINED` status cannot be cut, sewn, or shipped.
- Mass-balance violations or expired compliance certificates transition lots directly to `QUARANTINED`.

**Implementation:**
- `LotState`: Interface declaring `validateCanRecordEvent()`, `validateCanSplit()`, `validateCanShip()`.
- Concrete States: `ActiveLotState`, `ConsumedLotState`, `QuarantinedLotState`, `ShippedLotState`.
- `LotStateMachine`: Manages state resolution and transitions, persisting status back to `lots.status`.

---

### 2. Composite Pattern — `lineage.domain.composite`

**Why:** A garment's bill-of-materials forms a directed acyclic tree: a carton aggregates assembled shirts; each shirt aggregates cut panels (front, back, sleeves, collar); each panel derives from dyed fabric; dyed fabric derives from raw yarn. The Composite pattern treats individual raw materials and complex assemblies uniformly.

**Implementation:**
- `LineageNode`: Component interface defining `lotId()`, `lotNumber()`, `quantity()`, `unit()`, and `accept(LineageVisitor)`.
- `LeafSourceLot`: Represents atomic, un-split source materials (spinning mill yarn or raw cotton).
- `CompositeTransformedLot`: Represents transformed or aggregated assemblies containing child `LineageNode` instances.

---

### 3. Visitor Pattern — `lineage.domain.visitor`

**Why:** Multiple cross-cutting computations must traverse the composite lineage DAG without polluting domain entities:
- Carbon, water, and energy footprint rollups.
- Verified organic and recycled claim percentage calculations.
- Orphan lot detection (lots lacking source lineage).

**Implementation:**
- `LineageVisitor`: Visitor interface with `visitLeaf(LeafSourceLot)` and `visitComposite(CompositeTransformedLot)`.
- `FootprintRollupVisitor`: Traverses the tree to compute per-piece and total environmental metrics using configured emission factors and an `AllocationStrategy`.
- `ClaimRollupVisitor`: Verifies certified percentages, ensuring claimed content does not exceed verified inputs and flagging orphan provenance.

---

### 4. Strategy Pattern — `lineage.domain.strategy`

**Why:** When lots split or merge, environmental footprints must be apportioned according to accounting rules (e.g. ISO 14044 mass allocation vs. apparel industry piece-count allocation). Strategy allows dynamic selection via configuration or API request parameter.

**Implementation:**
- `AllocationStrategy`: Common interface defining `calculateRatio(...)` and `allocate(...)`.
- `MassAllocationStrategy`: Allocates footprints normalized by mass in kilograms (`KG`).
- `PieceCountAllocationStrategy`: Allocates footprints normalized by piece counts (`PIECES`, `CARTONS`).
- `AllocationStrategyRegistry`: Resolves active strategy bean dynamically at runtime.

---

### 5. Builder Pattern — `passport.domain.builder`

**Why:** A Digital Product Passport (DPP) combines data from lineage DAGs, evidence documents, registry metadata, and footprint calculations into a multi-section document. The Builder pattern provides a fluent, step-by-step construction API that enforces mandatory fields at compile time, handles versioning, and generates QR code byte arrays.

**Implementation:**
- `DigitalPassportBuilder`: Fluent builder constructing versioned `DigitalPassportEntity` instances.
- Dynamic Custom Fields (QA5): Supports arbitrary key-value custom attributes (`withCustomField(key, value)`) serialized to JSONB without modifying lineage classes.
- `QrCodeGenerator`: Integrates Google ZXing (`QRCodeWriter`) to generate PNG QR code byte arrays.

---

### 6. Proxy Pattern & Audience Security Views — `passport.proxy`

**Why:** DPPs serve multiple audiences with conflicting visibility and security requirements:
- **Public Consumers**: Need fast provenance without seeing confidential commercial data (`unit_price_bdt`, `exact_address`, `worker_count`).
- **Brands**: Need visibility into their own contracted suppliers with strict tenant isolation (cross-brand access blocked with HTTP 403).
- **Auditors**: Need unredacted mass-balance logs, unit prices, and worker headcounts.

**Implementation:**
- `PassportAccessPort`: Target interface exposing `getPublicPassport`, `getBrandPassport`, and `getAuditorPassport`.
- `AudiencePassportProxy`: Implements `PassportAccessPort`. Intercepts calls, validates Spring Security roles and tenant ownership, and redacts sensitive data.
- Offline Resilience: Integrates with `PassportCacheService` to serve cached views with ETag support and $< 2\,\text{s}$ fallback during database downtime (QA4).

---

### 7. Observer Pattern — `evidence.observer`

**Why:** When compliance certificates or test reports expire or enter the 30-day renewal window, downstream consumers must react automatically without tight coupling to the evidence storage layer.

**Implementation:**
- `EvidenceExpiredEvent` & `EvidenceExpiringSoonEvent`: Immutable domain events published when certificates expire.
- `EvidenceExpiryScheduler`: Periodically evaluates certificate dates against the current date and publishes expiry events via Spring's `ApplicationEventPublisher`.
- `EvidenceEventListener`: Listens for `EvidenceExpiredEvent`, revoking certified claims and transitioning affected lots to `QUARANTINED`.

---

### 8. Adapter Pattern — `bulkimport.adapter`

**Why:** Garment manufacturing facilities export production data in heterogeneous file formats (CSV cutting logs, Excel packing lists). The Adapter pattern converts these heterogeneous formats into uniform domain commands (`RecordEventCommand`).

**Implementation:**
- `LotImportRowAdapter<T>`: Target interface defining `supports(ImportType)` and `adapt(T rawRow, ImportContext context)`.
- `CsvCuttingReportAdapter`: Adapts CSV cutting log rows (`CUT` events) with syntactical and semantic validation.
- `ExcelPackingListAdapter`: Utilizes Apache POI to parse Microsoft Excel spreadsheets (`.xlsx`) for carton aggregation (`PACK` events).
- Error Isolation: Adapters throw `ImportRowValidationException`, enabling `BulkImportService` to report row-level rejection details without terminating batch processing.
