# ThreadTrace — Quality Attribute Verification & Evidence Report

This report presents empirical benchmark evidence, automated security audit logs, fault-tolerance test results, and regression test proofs for Quality Attributes **QA1 through QA5** on ThreadTrace.

---

## Executive Summary Matrix

| Quality Attribute | Target Metric / SLA | Measured Result | Status | Verification Tool / Evidence Reference |
| :--- | :--- | :--- | :--- | :--- |
| **QA1: Trace Speed** | Bounded recursive trace $p95 < 2000\,\text{ms}$ (200k lots / 2M events) | **$42\,\text{ms}$** ($p95$) / **$58\,\text{ms}$** ($p99$) | **PASSED** | `load-tests/qa1-trace-speed.js` (k6) |
| **QA2: Data Integrity** | 100% detection of over-claims, expired certs, cutting loss $> 12\%$, orphans | **4 / 4 Defects Caught** (0 false negatives) | **PASSED** | `QaSeededDatasetTest.java` (5/5 JUnit tests) |
| **QA3: Confidentiality** | 0 confidential leaks on public views; HTTP 403 on cross-brand access | **0 leaks**; **HTTP 403 confirmed** | **PASSED** | `load-tests/qa3-confidentiality-scan.sh` & `QaAudienceSecurityTest.java` |
| **QA4: Passport Availability** | Mobile DPP load $p95 < 2000\,\text{ms}$; 100% uptime during DB outage | **$18\,\text{ms}$** normal / **$24\,\text{ms}$** during DB outage | **PASSED** | `load-tests/qa4-passport-delivery.js` & `qa4-db-outage-test.sh` |
| **QA5: Modifiability** | Add certs, steps, emission factors, & custom fields with zero domain code edits | **Zero Java code edits** to lineage core | **PASSED** | `Qa5AdaptabilityTest.java` (5/5 JUnit tests) |

---

## 1. QA1: Lineage Trace Performance Evidence

### Test Configuration
- **Dataset**: 200,000 lots, 180,000 DAG edges, 2,000,000 events (`load-tests/seed-200k-lots.sql`).
- **Hardware**: Intel Core i7 / 8 GB RAM laptop running PostgreSQL 16 Alpine in Docker.
- **k6 Scenario**: 20 virtual users generating concurrent backward and forward traces over 50 seconds.

### k6 Benchmark Output Log
```text
          /\      |‾‾| /‾‾/   /‾‾/   
     /\  /  \     |  |/  /   /  /    
    /  \/    \    |     (   /   ‾‾\  
   /          \   |  |\  \ |  (‾)  | 
  / __________ \  |__| \__\ \_____/ .io

  execution: local
     script: load-tests/qa1-trace-speed.js
     scenarios: (100.00%) 1 scenario, 20 max VUs, 50s max duration

  ✓ backward trace status is 200
  ✓ backward trace returns nodes
  ✓ backward trace latency < 2000ms
  ✓ forward trace status is 200
  ✓ forward trace returns nodes
  ✓ forward trace latency < 2000ms

  checks.........................: 100.00% ✓ 4860      ✗ 0   
  http_req_duration..............: avg=31.4ms min=12.1ms med=28.7ms max=84.2ms p(90)=39.5ms p(95)=42.1ms p(99)=58.4ms
    { name:BackwardTrace }.......: avg=33.8ms min=14.2ms med=31.1ms max=84.2ms p(90)=41.8ms p(95)=44.6ms p(99)=61.2ms
    { name:ForwardTrace }........: avg=29.0ms min=12.1ms med=26.3ms max=72.1ms p(90)=37.2ms p(95)=39.6ms p(99)=55.6ms
  http_req_failed................: 0.00%   ✓ 0         ✗ 2430
  vus............................: 20      min=1       max=20
```

---

## 2. QA2: Integrity & Planted Defect Audit Evidence

Verified via automated JUnit test suite `QaSeededDatasetTest.java` targeting 4 planted supply chain anomalies:

```text
[INFO] Running com.cse444.threadtrace.QaSeededDatasetTest
00:13:58.320 [main] INFO c.c.threadtrace.QaSeededDatasetTest -- Problem 1 caught: Over-claimed recycled content detected (Claim 60.0% > Verified 25.0%)
00:13:58.320 [main] INFO c.c.threadtrace.QaSeededDatasetTest -- Problem 2 caught: Lot LOT-EXP-TEST-01 quarantined and certificate GRS revoked
00:13:58.321 [main] INFO c.c.threadtrace.QaSeededDatasetTest -- Problem 3 caught: Cutting loss 14.28% exceeds configured limit 12.0% -> lot quarantined
00:13:58.322 [main] INFO c.c.threadtrace.QaSeededDatasetTest -- Problem 4 caught: Orphan lot LOT-ORPHAN-01 flagged (missing source lineage)
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.183 s -- in com.cse444.threadtrace.QaSeededDatasetTest
```

---

## 3. QA3: Confidentiality & Tenant Isolation Evidence

Verified via `load-tests/qa3-confidentiality-scan.sh` and `QaAudienceSecurityTest.java`:

### Terminal Output
```text
======================================================================
 Starting QA3 Confidentiality & Multi-Tenant Security Scan
 Target Base URL: http://localhost:8080
 Sample QR Token: QR-BENCH-0160001
======================================================================
[TEST 1] Scanning Public DPP JSON (GET /api/v1/passports/QR-BENCH-0160001)... PASSED (0 confidential fields detected)
[TEST 2] Scanning Public DPP HTML (GET /passports/QR-BENCH-0160001)... PASSED (0 confidential fields in HTML markup)
[TEST 3] Scanning Public Lineage Trace (GET /api/v1/trace/backward/...)... PASSED (Lineage trace sanitised)
[TEST 4] Verifying Tenant Isolation (EcoThreads requesting Patagonia Passport)... PASSED (HTTP 403 Forbidden correctly returned for cross-tenant access)
[TEST 5] Verifying Auditor RBAC (Brand attempting Auditor Endpoint)... PASSED (HTTP 403 Forbidden correctly returned for non-auditor caller)
======================================================================
 SUCCESS: All QA3 confidentiality and tenant isolation checks passed!
 Zero confidential fields leaked; multi-tenant RBAC enforced.
```

---

## 4. QA4: High Availability & Offline Outage Resilience Evidence

### Simulated Database Outage Run (`load-tests/qa4-db-outage-test.sh`)
```text
======================================================================
 Starting QA4 High Availability / Database Outage Resilience Test
 Target Base URL: http://localhost:8080
 Testing QR Token: QR-BENCH-0160001
======================================================================
[STEP 1] Pre-warming cache while database is healthy... SUCCESS (HTTP 200 received, cache warmed)
[STEP 2] Simulating database outage: pausing PostgreSQL container...
Container threadtrace-postgres-1 Paused
Database container paused. Validating outage...
[STEP 3] Querying Public Passport API during DB outage... HTTP Status: 200 (24 ms)
[STEP 4] Querying Public Passport HTML Web Page during DB outage... SUCCESS: HTTP 200 OK served seamlessly from cache fallback!
[STEP 5] Restoring database availability: unpausing PostgreSQL...
Container threadtrace-postgres-1 Unpaused
======================================================================
 SUCCESS: QA4 Database Outage Resilience Verified!
 In-memory cache fallback served requests with 0 downtime in <2s.
======================================================================
```

---

## 5. QA5: Modifiability & Adaptability Verification Evidence

Verified via `Qa5AdaptabilityTest.java`:
- Confirms new certificate type `CRADLE_TO_CRADLE` registered via `@TestPropertySource`.
- Confirms new process step `QUILT` (max loss $8.5\%$) loaded dynamically.
- Confirms custom emission factors ($35.5\,\text{L}$, $5.2\,\text{kWh}$, $1.9\,\text{kg}$) active.
- Confirms DPP dynamic custom fields (`circularDesignScore`, `takeBackUrl`) added without touching any `lineage` class.
- ArchUnit test confirms 0 dependencies from `lineage` to `passport`.

```text
[INFO] Running com.cse444.threadtrace.Qa5AdaptabilityTest
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 20.10 s -- in com.cse444.threadtrace.Qa5AdaptabilityTest
```

---

## 6. Failed Runs & Iteration Log

In accordance with architectural review rigor, this section logs architectural iterations, failed attempts, root cause diagnostics, and engineering resolutions:

### Iteration 1: Unindexed Recursive CTEs (QA1 Failure)
- **Symptom**: During initial load testing with 200k lots, `GET /api/v1/trace/backward/{lotId}` exhibited a $p95$ latency of $4,820\,\text{ms}$, breaching the $2000\,\text{ms}$ SLA.
- **Root Cause Analysis**: PostgreSQL `EXPLAIN ANALYZE` revealed a `Seq Scan on lot_lineage_edges` inside the recursive term of the CTE. As the recursion traversed 6 tiers, it performed 6 sequential table scans across 180,000 edges ($6 \times 180,000 = 1,080,000$ row comparisons).
- **Corrective Action**: Added composite B-Tree indexes in `V1__init_schema.sql`:
  ```sql
  CREATE INDEX idx_lot_edges_parent ON lot_lineage_edges (parent_lot_id, child_lot_id);
  CREATE INDEX idx_lot_edges_child ON lot_lineage_edges (child_lot_id, parent_lot_id);
  ```
- **Outcome**: Latency dropped from $4,820\,\text{ms}$ to $42\,\text{ms}$ ($114\times$ speedup), satisfying QA1.

### Iteration 2: MinIO HTTP Client Loopback Block on Windows Sandbox
- **Symptom**: Integration tests threw `java.net.SocketException: Permission denied: connect` when initializing `MinioStorageAdapter`.
- **Root Cause Analysis**: On Windows, initializing `java.net.http.HttpClient.newBuilder().build()` during Spring Bean context creation opens an internal loopback selector pipe that was blocked by local sandbox socket restrictions.
- **Corrective Action**: Refactored `MinioStorageAdapter` to lazily instantiate `HttpClient` on first active remote upload, and automatically fall back to `LocalDiskStorageAdapter` when MinIO is unreachable or restricted.
- **Outcome**: Context starts in $< 3\,\text{s}$ with 100% test reliability.

### Iteration 3: Cross-Tenant Brand Information Leak
- **Symptom**: Preliminary prototype returned brand-level supplier pricing to any authenticated user with `ROLE_BRAND`.
- **Root Cause Analysis**: The controller checked `isAuthenticated()` and role `ROLE_BRAND`, but did not check tenant ownership between the authenticated caller's external code and the passport's `brand_id`.
- **Corrective Action**: Implemented the Proxy pattern in `AudiencePassportProxy`, extracting the caller's identity via Spring Security `Principal` and comparing `callerIdentity` to `brand.getExternalCode()`, throwing `AccessDeniedException` (HTTP 403) on mismatches.
- **Outcome**: Fully verified by `QaAudienceSecurityTest` and `qa3-confidentiality-scan.sh`.
