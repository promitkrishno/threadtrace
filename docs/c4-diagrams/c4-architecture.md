# ThreadTrace — C4 Architecture Documentation

This document describes the architecture of the **ThreadTrace** Garment Supply-Chain Traceability and Digital Product Passport (DPP) platform using the C4 model (Context, Container, and Component levels).

---

## 1. C4 Level 1: System Context Diagram

The Context diagram illustrates how ThreadTrace interacts with external actors (consumers, brands, compliance auditors, factory operators) and third-party systems.

```mermaid
flowchart TD
    subgraph Users["Users & Stakeholders"]
        Consumer["End Consumer<br/>[Person]<br/>Scans garment QR code in retail/home"]
        BrandUser["Brand Sustainability Manager<br/>[Person]<br/>Audits supply chain and monitors emissions"]
        Auditor["Compliance Auditor / Inspector<br/>[Person]<br/>Validates mass-balance and certificates"]
        FactoryUser["Factory Production Manager<br/>[Person]<br/>Uploads cutting and packing reports"]
    end

    subgraph ThreadTraceSystem["ThreadTrace System Boundary"]
        ThreadTrace["ThreadTrace Platform<br/>[Software System]<br/>Garment supply-chain traceability, hash-chained event ledger, mass-balance validation, and Digital Product Passports"]
    end

    subgraph ExternalSystems["External Systems"]
        MinioStore["MinIO / S3 Object Storage<br/>[External Storage]<br/>Stores raw PDF certificates and test reports"]
        FactoryERP["Factory ERP & Cutting Tables<br/>[External System]<br/>Exports daily CSV cutting logs and Excel packing lists"]
    end

    Consumer -->|"Scans QR code / Views Public DPP<br/>[HTTPS / GET]"| ThreadTrace
    BrandUser -->|"Manages styles, traces brand lots, reviews tier provenance<br/>[HTTPS / REST / Basic Auth]"| ThreadTrace
    Auditor -->|"Audits mass-balance flags, inspects confidential costs & workers<br/>[HTTPS / REST / ROLE_AUDITOR]"| ThreadTrace
    FactoryUser -->|"Submits batch CSV/Excel production files<br/>[HTTPS / POST Multipart]"| ThreadTrace
    FactoryERP -.->|"Supplies production exports"| FactoryUser
    ThreadTrace -->|"Stores and fetches PDF certificates<br/>[S3 API / SHA-256]"| MinioStore
```

---

## 2. C4 Level 2: Container Diagram

The Container diagram zooms into ThreadTrace, showing its high-level technical runtime blocks, communication protocols, and data persistence boundaries.

```mermaid
flowchart TD
    subgraph Clients["Clients & Entry Points"]
        MobileBrowser["Mobile Web Browser<br/>[Client]<br/>Public consumers viewing passport web page"]
        BrandApp["Brand / Auditor Client<br/>[Client]<br/>REST API callers & inspection dashboards"]
        ImportClient["Factory ERP File Uploader<br/>[Client]<br/>Batch upload tool / curl"]
    end

    subgraph ThreadTraceContainer["ThreadTrace Server Boundary"]
        SpringBootApp["Spring Boot Application<br/>[Container: Java 21 / Spring Boot 3.4.1]<br/>Exposes Web MVC, Thymeleaf UI, Spring Security, Spring Batch, Domain Logic"]
        InMemoryCache["In-Memory Cache<br/>[Container: Caffeine / ConcurrentMapCache]<br/>Caches public DPP views; provides sub-2s offline fallback during DB outage"]
    end

    subgraph DataStorage["Data & Storage Infrastructure"]
        PostgresDB[("PostgreSQL 16 Database<br/>[Container: Relational & JSONB]<br/>Stores parties, facilities, lots, DAG edges, 2M+ hash-chained events, and passports")]
        MinIOContainer[("MinIO Object Storage<br/>[Container: Blob Storage]<br/>Encrypted object storage for certificate PDFs and lab reports")]
    end

    MobileBrowser -->|"GET /passports/{qrToken}<br/>[HTTP / HTML5 / CSS]"| SpringBootApp
    BrandApp -->|"GET/POST /api/v1/*<br/>[HTTP / JSON / Basic Auth]"| SpringBootApp
    ImportClient -->|"POST /api/v1/imports/*<br/>[HTTP / Multipart]"| SpringBootApp

    SpringBootApp <-->|"Reads/Writes Cached Passports<br/>[In-Memory / Sub-ms]"| InMemoryCache
    SpringBootApp -->|"Recursive CTE queries, transactional lot event logs, JSONB DPPs<br/>[JDBC / HikariCP / Port 5432]"| PostgresDB
    SpringBootApp -->|"Uploads/downloads PDF evidence<br/>[HTTP S3 API / Port 9000]"| MinIOContainer
```

---

## 3. C4 Level 3: Component Diagram

The Component diagram reveals the internal modular decomposition of the Spring Boot application, showing how GoF patterns and domain boundaries collaborate while respecting strict ArchUnit rules.

```mermaid
flowchart TD
    subgraph WebLayer["Presentation & API Layer"]
        PassportWeb["PassportWebController<br/>[Spring MVC]<br/>Renders Thymeleaf DPP page"]
        PassportApi["PassportApiController<br/>[REST Controller]<br/>Public & audience JSON DPP endpoints with ETags"]
        TraceApi["TraceController<br/>[REST Controller]<br/>Recursive backward/forward trace endpoints"]
        ImportApi["BulkImportController<br/>[REST Controller]<br/>CSV & Excel batch import endpoints"]
        EvidenceApi["EvidenceController<br/>[REST Controller]<br/>Certificate upload & verification"]
    end

    subgraph SecurityProxy["Security & Access Proxy Layer (Proxy Pattern)"]
        AudienceProxy["AudiencePassportProxy<br/>[Component / Proxy Pattern]<br/>Enforces multi-tenant brand isolation (403) and sanitises public views"]
    end

    subgraph PassportModule["Digital Product Passport Module (Builder & CQRS)"]
        PassportService["DigitalPassportService<br/>[Application Service]<br/>Orchestrates DPP generation & caching"]
        PassportBuilder["DigitalPassportBuilder<br/>[Builder Pattern]<br/>Constructs versioned DPPs with dynamic custom fields"]
        PassportCache["PassportCacheService<br/>[Cache Service]<br/>Pre-warms cache & provides DB-down fallback"]
        PassportViews["Projections (CQRS)<br/>PublicPassportView<br/>BrandPassportView<br/>AuditorPassportView"]
    end

    subgraph BulkImportModule["Bulk Import Module (Adapter Pattern)"]
        ImportService["BulkImportService<br/>[Application Service]<br/>Coordinates batch jobs & generates error reports"]
        CsvAdapter["CsvCuttingReportAdapter<br/>[Adapter Pattern]<br/>Converts CSV rows to RecordEventCommand"]
        ExcelAdapter["ExcelPackingListAdapter<br/>[Adapter Pattern]<br/>Parses Excel .xlsx rows using Apache POI"]
    end

    subgraph LineageModule["Lineage & Traceability Core (State, Composite, Visitor, Strategy)"]
        LineageService["LineageTraceService<br/>[Application Service]<br/>Executes recursive CTE backward/forward queries"]
        MassBalanceValidator["MassBalanceValidator<br/>[Domain Service]<br/>Enforces process loss limits (e.g. CUT <= 12%)"]
        StateMachine["LotStateMachine<br/>[State Pattern]<br/>Enforces ACTIVE -> CONSUMED -> QUARANTINED -> SHIPPED"]
        CompositeDag["LineageNode (Composite Pattern)<br/>LeafSourceLot & CompositeTransformedLot"]
        RollupVisitors["Visitors (Visitor Pattern)<br/>FootprintRollupVisitor<br/>ClaimRollupVisitor"]
        AllocStrategies["Strategies (Strategy Pattern)<br/>MassAllocationStrategy<br/>PieceCountAllocationStrategy"]
    end

    subgraph EvidenceModule["Evidence & Compliance Module (Observer Pattern)"]
        EvidenceService["EvidenceService<br/>[Application Service]<br/>Manages document lifecycle and SHA-256 hashes"]
        ExpiryScheduler["EvidenceExpiryScheduler<br/>[Observer Pattern - Subject]<br/>Detects expired certs and publishes events"]
        EvidenceListener["EvidenceEventListener<br/>[Observer Pattern - Observer]<br/>Revokes certified claims & quarantines lots"]
        StoragePort["StoragePort / LocalDisk / MinIO<br/>[Storage Adapter]"]
    end

    subgraph RegistryModule["Registry Module"]
        RegistryService["RegistryService<br/>[Application Service]<br/>Party & Facility lookup and validations"]
    end

    %% Wiring
    PassportWeb --> AudienceProxy
    PassportApi --> AudienceProxy
    AudienceProxy --> PassportService
    PassportService --> PassportBuilder
    PassportService --> PassportCache
    PassportService -.->|"Reads through Port only<br/>(ArchUnit Rule 2)"| LineageService

    TraceApi --> LineageService
    LineageService --> CompositeDag
    LineageService --> RollupVisitors
    RollupVisitors --> AllocStrategies

    ImportApi --> ImportService
    ImportService --> CsvAdapter
    ImportService --> ExcelAdapter
    ImportService --> MassBalanceValidator
    ImportService --> StateMachine

    EvidenceApi --> EvidenceService
    EvidenceService --> StoragePort
    ExpiryScheduler -->|"Spring ApplicationEventPublisher"| EvidenceListener
    EvidenceListener --> StateMachine
```
