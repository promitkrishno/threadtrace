# ThreadTrace — Mid-Project Checkpoint Change Log

This document serves as the formal change tracking log for mid-project instructor change requests and architectural evolutions in ThreadTrace.

---

## 1. Change Request Overview Template

```markdown
### Change Request ID: CR-[YYYYMMDD-###]
- **Date Received:** [YYYY-MM-DD]
- **Requested By:** [Course Instructor / Teaching Assistant]
- **Summary of Change Request:**
  [Brief 1-2 sentence description of the requested modification or new feature requirement]
- **Rationale / Business Need:**
  [Why this change was requested and what supply-chain scenario it models]
```

---

## 2. Impact Analysis & Scope Boundary

| Evaluation Category | Impact Assessment | Architectural Guardrail Checked |
| :--- | :--- | :--- |
| **Domain Layer Impact** | `[None / Low / Medium / High]` | Are core entities mutated, or can patterns absorb the change? |
| **ArchUnit Rules** | `[No Violations / Rules Updated]` | `ArchitectureTest` rules 1 & 2 must remain 100% compliant. |
| **Database Schema** | `[None / Flyway Migration Vx__...]` | Backward compatibility preserved for existing lot histories? |
| **Performance (QA1, QA4)** | `[Negligible / Measured Impact]` | Trace latency remains $< 2000\,\text{ms}$; cache fallback intact? |
| **Security (QA3)** | `[Zero Leakage / RBAC Updated]` | Zero confidential fields leaked; tenant isolation preserved? |

---

## 3. File Modification Matrix

This section tracks core existing files modified versus new components created to enforce high cohesion and low coupling.

### A. Core Existing Files Modified
*Core domain classes should be modified minimally; preference is given to extension over modification.*

| File Path | Nature of Modification | Rationale & Pattern Applied |
| :--- | :--- | :--- |
| *Example:* `lineage/domain/model/LotEntity.java` | *None (Untouched)* | Preserved by Strategy & Visitor patterns |
| *Example:* `common/config/TraceabilityProperties.java` | Added new configuration binding | Enabled external configuration without code changes |

### B. New Components Added
*New classes, adapters, visitors, or migrations introduced to satisfy the change request.*

| New File Path | Architectural Role | Pattern / Mechanism |
| :--- | :--- | :--- |
| *Example:* `lineage/domain/strategy/EconomicAllocationStrategy.java` | Economic value allocation | Strategy Pattern (`AllocationStrategy`) |
| *Example:* `bulkimport/adapter/EdifactOrderAdapter.java` | Ingests EDIFACT order files | Adapter Pattern (`LotImportRowAdapter`) |

---

## 4. Regression & Quality Attribute Verification

Before submitting or signing off on any mid-project checkpoint change, execute and record the following verification steps:

1. **Full Automated Test Suite**:
   ```bash
   ./mvnw clean test
   ```
   - Target: 100% pass rate across all unit, slice, and integration tests.
   - Total Tests Executed: `[X]`, Failures: `0`, Errors: `0`.

2. **ArchUnit Compliance**:
   ```bash
   ./mvnw test -Dtest=ArchitectureTest
   ```
   - Rule 1: Domain independent of infrastructure & web $\to$ **PASSED**
   - Rule 2: Passport decoupled from lineage internals $\to$ **PASSED**

3. **Planted Defect & Security Regression**:
   ```bash
   ./mvnw test -Dtest=QaSeededDatasetTest,QaAudienceSecurityTest,Qa5AdaptabilityTest
   ```
   - Over-claimed content detected $\to$ **PASSED**
   - Expired cert quarantine $\to$ **PASSED**
   - Cutting loss $> 12\%$ flagged $\to$ **PASSED**
   - Tenant isolation & 0 confidential leaks $\to$ **PASSED**

---

## 5. Historical Change Log Entries

### Entry CR-20261114-001 (Baseline Milestone Evolution)
- **Change:** Transitioned from single-lot cutting to multi-lot aggregation in bulk import.
- **Components Added:** `ExcelPackingListAdapter.java`, `CsvCuttingReportAdapter.java`.
- **Core Files Touched:** `SecurityConfig.java` (added test users for multi-tenant tests).
- **Verification:** All 26 tests passed; ArchUnit boundaries verified.
