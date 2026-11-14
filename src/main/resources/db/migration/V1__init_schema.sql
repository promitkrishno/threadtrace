-- ============================================================================
-- ThreadTrace - Milestone 1 Initial Schema
-- PostgreSQL 16 schema with optimized indexes for recursive lineage queries
-- ============================================================================

-- Enable pgcrypto extension for UUID generation if needed
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ----------------------------------------------------------------------------
-- 1. Parties (Registry - Suppliers, Mills, Factories, Brands)
-- ----------------------------------------------------------------------------
CREATE TABLE parties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    external_code VARCHAR(100) NOT NULL UNIQUE,
    country_code VARCHAR(10) NOT NULL,
    exact_address VARCHAR(500),
    worker_count INTEGER,
    unit_price_bdt NUMERIC(15, 4),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_parties_party_type ON parties (party_type);
CREATE INDEX idx_parties_country_code ON parties (country_code);

-- ----------------------------------------------------------------------------
-- 2. Facilities / Production Lines
-- ----------------------------------------------------------------------------
CREATE TABLE facilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES parties(id) ON DELETE RESTRICT,
    facility_type VARCHAR(50) NOT NULL,
    parent_id UUID REFERENCES facilities(id) ON DELETE SET NULL,
    name VARCHAR(255) NOT NULL,
    location_city VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_facilities_party_id ON facilities (party_id);
CREATE INDEX idx_facilities_parent_id ON facilities (parent_id);
CREATE INDEX idx_facilities_facility_type ON facilities (facility_type);

-- ----------------------------------------------------------------------------
-- 3. Lots (Core Traceable Entity)
-- ----------------------------------------------------------------------------
CREATE TABLE lots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_number VARCHAR(100) NOT NULL UNIQUE,
    brand_id UUID NOT NULL REFERENCES parties(id) ON DELETE RESTRICT,
    current_facility_id UUID NOT NULL REFERENCES facilities(id) ON DELETE RESTRICT,
    style_code VARCHAR(100),
    shipment_code VARCHAR(100),
    status VARCHAR(50) NOT NULL,
    current_quantity NUMERIC(15, 4) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    certified_recycled_pct NUMERIC(5, 2) DEFAULT 0.0,
    certified_organic_pct NUMERIC(5, 2) DEFAULT 0.0,
    accumulated_water_l NUMERIC(15, 4) DEFAULT 0.0,
    accumulated_energy_kwh NUMERIC(15, 4) DEFAULT 0.0,
    accumulated_co2e_kg NUMERIC(15, 4) DEFAULT 0.0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lots_lot_number ON lots (lot_number);
CREATE INDEX idx_lots_brand_id ON lots (brand_id);
CREATE INDEX idx_lots_facility_id ON lots (current_facility_id);
CREATE INDEX idx_lots_status ON lots (status);
CREATE INDEX idx_lots_style_code ON lots (style_code);
CREATE INDEX idx_lots_shipment_code ON lots (shipment_code);

-- ----------------------------------------------------------------------------
-- 4. Lot Lineage Edges (Directed Acyclic Graph)
-- ----------------------------------------------------------------------------
CREATE TABLE lot_lineage_edges (
    parent_lot_id UUID NOT NULL REFERENCES lots(id) ON DELETE RESTRICT,
    child_lot_id UUID NOT NULL REFERENCES lots(id) ON DELETE RESTRICT,
    causing_event_id UUID NOT NULL,
    input_quantity_used NUMERIC(15, 4) NOT NULL,
    input_unit VARCHAR(50) NOT NULL,
    allocation_ratio NUMERIC(10, 6) NOT NULL DEFAULT 1.000000,
    PRIMARY KEY (parent_lot_id, child_lot_id)
);

-- Bidirectional B-Tree composite indexes for <2s recursive queries on 200k+ lots
CREATE INDEX idx_lot_edges_parent ON lot_lineage_edges (parent_lot_id, child_lot_id);
CREATE INDEX idx_lot_edges_child ON lot_lineage_edges (child_lot_id, parent_lot_id);
CREATE INDEX idx_lot_edges_causing_event ON lot_lineage_edges (causing_event_id);

-- ----------------------------------------------------------------------------
-- 5. Lot Events (Immutable Event Log with SHA-256 Hash Chain)
-- ----------------------------------------------------------------------------
CREATE TABLE lot_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lot_id UUID NOT NULL REFERENCES lots(id) ON DELETE RESTRICT,
    sequence_no BIGINT NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    facility_id UUID NOT NULL REFERENCES facilities(id) ON DELETE RESTRICT,
    input_quantity NUMERIC(15, 4) NOT NULL,
    output_quantity NUMERIC(15, 4) NOT NULL,
    loss_percentage NUMERIC(5, 2) NOT NULL DEFAULT 0.0,
    unit VARCHAR(50) NOT NULL,
    step_water_liters NUMERIC(15, 4) NOT NULL DEFAULT 0.0,
    step_energy_kwh NUMERIC(15, 4) NOT NULL DEFAULT 0.0,
    step_co2e_kg NUMERIC(15, 4) NOT NULL DEFAULT 0.0,
    mass_balance_flagged BOOLEAN NOT NULL DEFAULT FALSE,
    previous_event_hash VARCHAR(64) NOT NULL,
    event_hash VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    recorded_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_lot_events_lot_seq UNIQUE (lot_id, sequence_no)
);

CREATE INDEX idx_lot_events_lot_id_seq ON lot_events (lot_id, sequence_no);
CREATE INDEX idx_lot_events_occurred ON lot_events (occurred_at);
CREATE INDEX idx_lot_events_facility_id ON lot_events (facility_id);
CREATE INDEX idx_lot_events_event_type ON lot_events (event_type);

-- ----------------------------------------------------------------------------
-- 6. Evidence Documents
-- ----------------------------------------------------------------------------
CREATE TABLE evidence_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    certificate_type VARCHAR(50) NOT NULL,
    issuer_name VARCHAR(255) NOT NULL,
    scope_description TEXT,
    claim_type VARCHAR(100),
    certified_percentage NUMERIC(5, 2),
    storage_object_key VARCHAR(500) NOT NULL,
    file_sha256 VARCHAR(64) NOT NULL,
    valid_from DATE NOT NULL,
    valid_until DATE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    party_id UUID REFERENCES parties(id) ON DELETE SET NULL,
    lot_id UUID REFERENCES lots(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evidence_lot_id ON evidence_documents (lot_id);
CREATE INDEX idx_evidence_party_id ON evidence_documents (party_id);
CREATE INDEX idx_evidence_cert_type ON evidence_documents (certificate_type);

-- ----------------------------------------------------------------------------
-- 7. Digital Passports (DPP)
-- ----------------------------------------------------------------------------
CREATE TABLE digital_passports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    style_code VARCHAR(100) NOT NULL,
    shipment_code VARCHAR(100) NOT NULL,
    brand_id UUID NOT NULL REFERENCES parties(id) ON DELETE RESTRICT,
    root_carton_lot_id UUID NOT NULL REFERENCES lots(id) ON DELETE RESTRICT,
    version_no INTEGER NOT NULL DEFAULT 1,
    is_latest BOOLEAN NOT NULL DEFAULT TRUE,
    qr_code_token VARCHAR(255) NOT NULL UNIQUE,
    etag_hash VARCHAR(64) NOT NULL,
    public_payload JSONB NOT NULL,
    brand_payload JSONB,
    auditor_payload JSONB,
    published_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_passports_style_shipment_version UNIQUE (style_code, shipment_code, version_no)
);

CREATE INDEX idx_passports_root_carton ON digital_passports (root_carton_lot_id);
CREATE INDEX idx_passports_style_shipment ON digital_passports (style_code, shipment_code);
CREATE INDEX idx_passports_brand_id ON digital_passports (brand_id);
CREATE INDEX idx_passports_qr_token ON digital_passports (qr_code_token);
