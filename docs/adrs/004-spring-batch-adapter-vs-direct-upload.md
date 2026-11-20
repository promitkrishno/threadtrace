# ADR 004: Bulk Ingestion — Spring Batch & GoF Adapter Pattern vs Direct Synchronous JPA Uploads

## Status

Accepted

## Date

2026-11-15

## Deciders

ThreadTrace Architecture & Core Engineering Team

---

## Context & Problem Statement

Garment manufacturing facilities operate diverse, legacy Enterprise Resource Planning (ERP) tools, automated cutting table software (e.g., Gerber, Lectra), and warehouse packing systems. Consequently, production lot records arrive in heterogeneous file formats:
- Automated cutting logs exported as raw CSV files.
- Finished carton packing lists exported as formatted Microsoft Excel spreadsheets (`.xlsx`).

Attempting to process these large files through standard synchronous JPA controller endpoints results in:
1. HTTP request timeouts during large multi-megabyte file uploads.
2. All-or-nothing rollback behavior: a single typographical error on row 1,450 aborts the entire 5,000-piece production batch.
3. Tight coupling between external supplier file layouts and internal domain entities.

---

## Decision Drivers

- **Fault Tolerance & Row-Level Diagnostics (FR9)**: The system must isolate bad rows, continuing to process valid rows while returning a structured error report containing line numbers, failing columns, and business rejection reasons.
- **Extensibility & Decoupling (QA5)**: Adding support for a new factory format (e.g., EDIFACT, JSON batch) must not alter the core traceability event model.
- **Memory Efficiency**: Parsing multi-sheet Excel files and large CSVs must not trigger JVM `OutOfMemoryError` on 8 GB RAM developer laptops.

---

## Options Considered

### Option 1: Synchronous Direct JPA Upload Loop
- **Pros**: Minimal boilerplate code in the controller.
- **Cons**: Monolithic transaction semantics cause entire batch failure if one row is invalid. Memory-heavy when loading entire files into JPA entities. Fails with HTTP 504 gateway timeouts on large batches.

### Option 2: Message-Broker Driven Processing (RabbitMQ / Kafka)
- **Pros**: Highly asynchronous, distributed worker scalability.
- **Cons**: Running Kafka or RabbitMQ broker containers consumes substantial RAM ($> 1\text{–}1.5\,\text{GB}$) and adds operational complexity unnecessary for typical factory batch cadences.

### Option 3: Spring Batch & GoF Adapter Pattern (`bulkimport.adapter`) (Selected)
- **Pros**:
  - **GoF Adapter Pattern**: Standardizes disparate third-party formats into uniform `RecordEventCommand` domain records via `LotImportRowAdapter<T>`.
  - **Modular Adapters**: `CsvCuttingReportAdapter` handles CSV streams; `ExcelPackingListAdapter` leverages Apache POI to parse `.xlsx` workbooks.
  - **Non-Fatal Row Validation**: Syntactic and semantic row errors throw `ImportRowValidationException`, which is caught and appended to `ImportExecutionReport(acceptedRows, rejectedRows, details)`.
  - **Low Memory Overhead**: Streaming readers process records sequentially without retaining whole files in memory.
- **Cons**:
  - Requires writing and maintaining dedicated adapters for each distinct file format.

---

## Decision

**Implement the GoF Adapter Pattern with Spring Batch / Bulk Import Services (`bulkimport.adapter` and `bulkimport.job`).**

1. **Target Interface**:
   ```java
   public interface LotImportRowAdapter<T> {
       boolean supports(ImportType type);
       RecordEventCommand adapt(T rawRow, ImportContext context) throws ImportRowValidationException;
   }
   ```
2. **Adapters**:
   - `CsvCuttingReportAdapter`: Maps cutting table CSV exports (`CUT` events, waste kg, fabric lot references).
   - `ExcelPackingListAdapter`: Maps carton packing spreadsheets (`PACK` events, garment pieces to carton aggregation).
3. **Execution Report**:
   `BulkImportService` runs batch ingestion, aggregates errors into `RejectedRowDetail(rowNumber, lotNumber, errorCode, message)`, and returns `ImportExecutionReport` with HTTP 200/207 multi-status semantics.

---

## Consequences

- **Positive:**
  - High resilience: Factory operators receive precise error feedback (e.g., "Row 42: Cutting loss 14.5% exceeds configured threshold 12.0%") without discarding the remaining 499 valid rows.
  - Testable in isolation: Adapters are unit-tested independently of file I/O and web layers.
  - Zero modifications to core `lineage` classes when introducing new import file formats.
- **Negative:**
  - Apache POI adds memory footprint for very large `.xlsx` files ($> 100\,\text{MB}$); mitigated by requiring standard batch sizes or CSV for massive volumes.

---

## Measured Evidence Placeholder

- **Integration Test**: `com.cse444.threadtrace.bulkimport.BulkImportIntegrationTest`
  - Validates CSV cutting logs with valid rows + rejected rows (asserting rejection reporting with exact row indices and reasons).
  - Validates Excel `.xlsx` packing lists with valid carton lots + rejected row diagnostics.
- **Performance**: Ingestion of a 500-row batch completes in $< 450\,\text{ms}$.
