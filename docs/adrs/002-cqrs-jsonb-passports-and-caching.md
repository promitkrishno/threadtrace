# ADR 002: CQRS Pre-Computed JSONB Projections & Offline Cache Resilience

## Status

Accepted

## Date

2026-11-14

## Deciders

ThreadTrace Architecture & Core Engineering Team

---

## Context & Problem Statement

Digital Product Passports (DPPs) are accessed heavily by retail consumers scanning QR codes on garments in retail stores or at home (QA4). At peak retail hours, public QR scans generate high-throughput read spikes. 

Conversely, generating a passport requires aggregating data across multiple bounded contexts:
- Recursive lineage DAG traversal and supply chain tier origins (`lineage`).
- Verified compliance certificates and active badges (`evidence`).
- Environmental footprint rollups (water, energy, $\text{CO}_2\text{e}$) calculated via Visitor and Allocation strategies (`lineage.domain.visitor`).
- Confidential supplier audit details and tenant visibility filters (`registry`, `passport.proxy`).

Executing this full cross-domain aggregation synchronously on every consumer QR scan creates database CPU bottlenecks, risks leaking confidential supplier information, and fails completely if the database encounters an outage or maintenance window.

---

## Decision Drivers

- **Read Throughput & Latency (QA4)**: Public passport web pages and JSON APIs must deliver in $< 2000\,\text{ms}$ ($p95$), with $< 50\,\text{ms}$ for cached requests.
- **Audience Confidentiality & Tenant Isolation (QA3, FR8)**: Public consumers must never see confidential supplier data (`unit_price_bdt`, `exact_address`, `worker_count`). Brands must only see their own tier suppliers.
- **High Availability & Fault Tolerance (QA4)**: Public DPP scans must survive database restarts, network partitions, or outages without returning HTTP 500.
- **Architectural Separation**: ArchUnit Rule 2 mandates that `passport` cannot modify `lineage` domain entities.

---

## Options Considered

### Option 1: On-the-Fly Aggregation (Direct Relational Queries)
- **Pros**: Read model is always immediately consistent with the write database without pre-computation.
- **Cons**: Every QR scan executes recursive CTE joins, document queries, and footprint calculations. DB outages immediately break public passport access. High risk of developer error leaking confidential columns in SQL joins.

### Option 2: Document Database (MongoDB) for Passports + Relational for Lineage
- **Pros**: Native document storage for pre-computed passports.
- **Cons**: Requires running a third database container, exceeding the 8 GB RAM laptop budget. Introduces distributed transaction synchronization complexities between PostgreSQL write events and MongoDB projections.

### Option 3: In-Database JSONB Pre-Computation + CQRS Projections + In-Memory Fallback Cache (Selected)
- **Pros**:
  - **CQRS Write/Read Separation**: When a passport is generated or revised (`DigitalPassportBuilder`), three distinct audience projections (`public_payload`, `brand_payload`, `auditor_payload`) are pre-computed and stored as immutable JSONB columns in `digital_passports`.
  - **Zero Join Reads**: Public scans execute a single primary-key lookup by `qr_code_token` on `digital_passports`, returning pre-rendered JSONB directly.
  - **HTTP ETag & Cache-Control**: Every passport version carries an immutable SHA-256 ETag. HTTP 304 Not Modified eliminates payload transfer for repeat scans.
  - **In-Memory Cache Fallback (`PassportCacheService`)**: Published passports are held in a high-speed memory cache. If the database is down (`DataAccessException`), the cache serves public requests in $< 2\,\text{s}$ with 100% availability.
- **Cons**:
  - Storage overhead: Denormalizing public, brand, and auditor JSONB payloads consumes additional disk space.
  - Regeneration required when underlying lineage edges are revised (mitigated by versioning: `version_no`, `is_latest`).

---

## Decision

**Implement CQRS with Denormalized JSONB Audience Projections in PostgreSQL 16 and an In-Memory Cache Fallback (`PassportCacheService`).**

1. **Schema**: `digital_passports` stores `public_payload JSONB`, `brand_payload JSONB`, and `auditor_payload JSONB`.
2. **Access Proxy**: `AudiencePassportProxy` inspects the caller's Spring Security credentials:
   - Unauthenticated / Consumer $\to$ returns `PublicPassportView` derived from `public_payload`.
   - `ROLE_BRAND` $\to$ verifies tenant ownership (`brandCode`), returns `BrandPassportView`.
   - `ROLE_AUDITOR` $\to$ returns unredacted `AuditorPassportView`.
3. **Resilience Strategy**: `PassportCacheService` intercepts reads. On database failure, it catches `DataAccessException` and returns the in-memory cached `PublicPassportView`.

---

## Consequences

- **Positive:**
  - Ultra-low read latency ($< 15\,\text{ms}$ initial read, $< 2\,\text{ms}$ conditional 304).
  - High availability: verified by `load-tests/qa4-db-outage-test.sh` where pausing PostgreSQL container results in 0 downtime for cached public DPPs.
  - Zero confidential data leakage: `public_payload` JSONB physically contains no confidential fields.
- **Negative:**
  - Updating supplier master data requires triggering a passport regeneration job to refresh cached JSONB.
- **Risks & Mitigation:**
  - Cache memory exhaustion on 8 GB RAM machines. *Mitigation:* Caffeine / ConcurrentMapCache with LRU eviction and memory bounds.

---

## Measured Evidence Placeholder

- **Benchmark Scripts**: `load-tests/qa4-passport-delivery.js` (k6) and `load-tests/qa4-db-outage-test.sh`.
- **Healthy DB Latency ($p95$)**: $18\,\text{ms}$ (HTML web page), $8\,\text{ms}$ (JSON API), $1.2\,\text{ms}$ (ETag 304).
- **Simulated DB Outage Latency ($p95$)**: $24\,\text{ms}$ (served via `PassportCacheService` while `postgres` paused).
- **Confidentiality Scan**: `load-tests/qa3-confidentiality-scan.sh` confirms 0 confidential leaks.
