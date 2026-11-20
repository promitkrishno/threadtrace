-- ============================================================================
-- ThreadTrace - Fast Benchmark Data Seeder (QA1)
-- Populates 200,000 lots, 2,000,000 hash-chained lot events, lineage DAG edges,
-- parties, facilities, and digital passports in <60 seconds on an 8 GB RAM laptop.
-- ============================================================================

\timing on
SET client_min_messages = warning;
SET synchronous_commit = off;

BEGIN;

-- ----------------------------------------------------------------------------
-- 1. Parties & Facilities across 6 Process Levels
-- ----------------------------------------------------------------------------
-- Fixed UUIDs for deterministic reference
CREATE TEMPORARY TABLE seed_constants (
    brand_id UUID,
    brand_other_id UUID,
    party_spin UUID,
    party_fab UUID,
    party_dye UUID,
    party_cut UUID,
    party_sew UUID,
    party_pack UUID,
    fac_spin UUID,
    fac_fab UUID,
    fac_dye UUID,
    fac_cut UUID,
    fac_sew UUID,
    fac_pack UUID
);

INSERT INTO seed_constants VALUES (
    '10000000-0000-0000-0000-000000000001'::uuid, -- Patagonia Global (BRAND-PAT-001)
    '10000000-0000-0000-0000-000000000002'::uuid, -- EcoThreads Brand (BRAND-ECO-001)
    '20000000-0000-0000-0000-000000000001'::uuid, -- Apex Spinning Mills
    '20000000-0000-0000-0000-000000000002'::uuid, -- Beximco Fabric Mills
    '20000000-0000-0000-0000-000000000003'::uuid, -- Robintex Dye & Wash
    '20000000-0000-0000-0000-000000000004'::uuid, -- Square Fashions Cutting
    '20000000-0000-0000-0000-000000000005'::uuid, -- Square Fashions Sewing
    '20000000-0000-0000-0000-000000000006'::uuid, -- DBL Logistics & Packing
    '30000000-0000-0000-0000-000000000001'::uuid, -- Fac Spinning
    '30000000-0000-0000-0000-000000000002'::uuid, -- Fac Fabric
    '30000000-0000-0000-0000-000000000003'::uuid, -- Fac Dye
    '30000000-0000-0000-0000-000000000004'::uuid, -- Fac Cut
    '30000000-0000-0000-0000-000000000005'::uuid, -- Fac Sew
    '30000000-0000-0000-0000-000000000006'::uuid  -- Fac Pack
);

-- Insert Parties (ON CONFLICT DO NOTHING for idempotent runs)
INSERT INTO parties (id, party_type, name, external_code, country_code, exact_address, worker_count, unit_price_bdt)
SELECT brand_id, 'BRAND', 'Patagonia Global', 'BRAND-PAT-001', 'USA', '259 W Santa Clara St, Ventura, CA', 1200, 4500.00 FROM seed_constants
ON CONFLICT (external_code) DO NOTHING;

INSERT INTO parties (id, party_type, name, external_code, country_code, exact_address, worker_count, unit_price_bdt)
SELECT brand_other_id, 'BRAND', 'EcoThreads Europe', 'BRAND-ECO-001', 'DEU', 'Friedrichstr 42, Berlin', 350, 3800.00 FROM seed_constants
ON CONFLICT (external_code) DO NOTHING;

INSERT INTO parties (id, party_type, name, external_code, country_code, exact_address, worker_count, unit_price_bdt)
SELECT party_spin, 'SPINNING_MILL', 'Apex Spinning Mills Ltd', 'MILL-SPIN-001', 'BGD', 'Plot 12, Gazipur Industrial Area, Gazipur', 850, 420.50 FROM seed_constants
ON CONFLICT (external_code) DO NOTHING;

INSERT INTO parties (id, party_type, name, external_code, country_code, exact_address, worker_count, unit_price_bdt)
SELECT party_fab, 'FABRIC_MILL', 'Beximco Fabric Mills Ltd', 'MILL-FAB-001', 'BGD', 'Sarabo, Kashimpur, Gazipur', 1400, 680.00 FROM seed_constants
ON CONFLICT (external_code) DO NOTHING;

INSERT INTO parties (id, party_type, name, external_code, country_code, exact_address, worker_count, unit_price_bdt)
SELECT party_dye, 'DYE_HOUSE', 'Robintex Dye & Wash Plant', 'PLANT-DYE-001', 'BGD', 'Baura, Bhulta, Narayanganj', 620, 310.00 FROM seed_constants
ON CONFLICT (external_code) DO NOTHING;

INSERT INTO parties (id, party_type, name, external_code, country_code, exact_address, worker_count, unit_price_bdt)
SELECT party_cut, 'FACTORY', 'Square Fashions Cutting Unit', 'FAC-CUT-001', 'BGD', 'Valuka, Mymensingh', 950, 150.00 FROM seed_constants
ON CONFLICT (external_code) DO NOTHING;

INSERT INTO parties (id, party_type, name, external_code, country_code, exact_address, worker_count, unit_price_bdt)
SELECT party_sew, 'FACTORY', 'Square Fashions Assembly Plant', 'FAC-SEW-001', 'BGD', 'Valuka, Mymensingh', 2800, 290.00 FROM seed_constants
ON CONFLICT (external_code) DO NOTHING;

INSERT INTO parties (id, party_type, name, external_code, country_code, exact_address, worker_count, unit_price_bdt)
SELECT party_pack, 'FACTORY', 'DBL Packing & Distribution Hub', 'FAC-PACK-001', 'BGD', 'Kashimpur, Gazipur', 450, 95.00 FROM seed_constants
ON CONFLICT (external_code) DO NOTHING;

-- Insert Facilities
INSERT INTO facilities (id, party_id, facility_type, name, location_city)
SELECT fac_spin, party_spin, 'MILL', 'Apex Spinning Unit 1', 'Gazipur' FROM seed_constants
ON CONFLICT (id) DO NOTHING;

INSERT INTO facilities (id, party_id, facility_type, name, location_city)
SELECT fac_fab, party_fab, 'MILL', 'Beximco Weaving Hall B', 'Gazipur' FROM seed_constants
ON CONFLICT (id) DO NOTHING;

INSERT INTO facilities (id, party_id, facility_type, name, location_city)
SELECT fac_dye, party_dye, 'WASH_PLANT', 'Robintex Eco-Dye Facility', 'Narayanganj' FROM seed_constants
ON CONFLICT (id) DO NOTHING;

INSERT INTO facilities (id, party_id, facility_type, name, location_city)
SELECT fac_cut, party_cut, 'PRODUCTION_LINE', 'Square Automated Cutting Floor', 'Mymensingh' FROM seed_constants
ON CONFLICT (id) DO NOTHING;

INSERT INTO facilities (id, party_id, facility_type, name, location_city)
SELECT fac_sew, party_sew, 'PRODUCTION_LINE', 'Square Assembly Line A-D', 'Mymensingh' FROM seed_constants
ON CONFLICT (id) DO NOTHING;

INSERT INTO facilities (id, party_id, facility_type, name, location_city)
SELECT fac_pack, party_pack, 'FACTORY', 'DBL Export Logistics Terminal', 'Gazipur' FROM seed_constants
ON CONFLICT (id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 2. Populate 200,000 Lots across 6 Supply Chain Process Levels
-- Level 1: 1..20,000       - Spinning (Yarn lots)
-- Level 2: 20,001..50,000  - Fabric Mill (Knitted/Woven fabric)
-- Level 3: 50,001..80,000  - Dye/Wash (Dyed fabric)
-- Level 4: 80,001..120,000 - Cut (Cut garment panels)
-- Level 5: 120,001..160,000- Sew (Assembled garments)
-- Level 6: 160,001..200,000- Pack/Ship (Cartons / Shipped DPP units)
-- ----------------------------------------------------------------------------
INSERT INTO lots (
    id,
    lot_number,
    brand_id,
    current_facility_id,
    style_code,
    shipment_code,
    status,
    current_quantity,
    unit,
    certified_recycled_pct,
    certified_organic_pct,
    accumulated_water_l,
    accumulated_energy_kwh,
    accumulated_co2e_kg,
    created_at
)
SELECT
    ('a0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid AS id,
    'LOT-BENCH-' || lpad(i::text, 7, '0') AS lot_number,
    (SELECT brand_id FROM seed_constants) AS brand_id,
    CASE
        WHEN i <= 20000  THEN (SELECT fac_spin FROM seed_constants)
        WHEN i <= 50000  THEN (SELECT fac_fab FROM seed_constants)
        WHEN i <= 80000  THEN (SELECT fac_dye FROM seed_constants)
        WHEN i <= 120000 THEN (SELECT fac_cut FROM seed_constants)
        WHEN i <= 160000 THEN (SELECT fac_sew FROM seed_constants)
        ELSE                  (SELECT fac_pack FROM seed_constants)
    END AS current_facility_id,
    'STYLE-TSHIRT-' || lpad(((i % 500) + 1)::text, 4, '0') AS style_code,
    'SHIP-2026-' || lpad(((i % 200) + 1)::text, 4, '0') AS shipment_code,
    CASE
        WHEN i > 160000 THEN 'SHIPPED'
        ELSE 'CONSUMED'
    END AS status,
    CASE
        WHEN i <= 80000  THEN 1000.0000
        WHEN i <= 160000 THEN 2500.0000
        ELSE                  50.0000
    END AS current_quantity,
    CASE
        WHEN i <= 80000  THEN 'KG'
        WHEN i <= 160000 THEN 'PIECES'
        ELSE                  'CARTONS'
    END AS unit,
    CASE WHEN (i % 3) = 0 THEN 45.00 ELSE 0.00 END AS certified_recycled_pct,
    CASE WHEN (i % 2) = 0 THEN 80.00 ELSE 0.00 END AS certified_organic_pct,
    (250.0 + (i % 100))::numeric(15,4) AS accumulated_water_l,
    (45.0 + (i % 20))::numeric(15,4) AS accumulated_energy_kwh,
    (8.5 + (i % 5))::numeric(15,4) AS accumulated_co2e_kg,
    CURRENT_TIMESTAMP - (200000 - i) * interval '5 seconds' AS created_at
FROM generate_series(1, 200000) AS s(i)
ON CONFLICT (lot_number) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 3. Populate Lineage Edges (180,000+ DAG Edges across 6 Levels)
-- ----------------------------------------------------------------------------
-- Level 2 child lots (20,001..50,000) connect to Level 1 parent lots (1..20,000)
INSERT INTO lot_lineage_edges (parent_lot_id, child_lot_id, causing_event_id, input_quantity_used, input_unit, allocation_ratio)
SELECT
    ('a0000000-0000-0000-0000-' || lpad(to_hex(((i - 20001) % 20000) + 1), 12, '0'))::uuid AS parent_lot_id,
    ('a0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid AS child_lot_id,
    ('c0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid AS causing_event_id,
    800.0000 AS input_quantity_used,
    'KG' AS input_unit,
    1.000000 AS allocation_ratio
FROM generate_series(20001, 50000) AS s(i)
ON CONFLICT (parent_lot_id, child_lot_id) DO NOTHING;

-- Level 3 child lots (50,001..80,000) connect to Level 2 parent lots (20,001..50,000)
INSERT INTO lot_lineage_edges (parent_lot_id, child_lot_id, causing_event_id, input_quantity_used, input_unit, allocation_ratio)
SELECT
    ('a0000000-0000-0000-0000-' || lpad(to_hex(20000 + ((i - 50001) % 30000) + 1), 12, '0'))::uuid AS parent_lot_id,
    ('a0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid AS child_lot_id,
    ('c0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid AS causing_event_id,
    780.0000 AS input_quantity_used,
    'KG' AS input_unit,
    1.000000 AS allocation_ratio
FROM generate_series(50001, 80000) AS s(i)
ON CONFLICT (parent_lot_id, child_lot_id) DO NOTHING;

-- Level 4 child lots (80,001..120,000) connect to Level 3 parent lots (50,001..80,000)
INSERT INTO lot_lineage_edges (parent_lot_id, child_lot_id, causing_event_id, input_quantity_used, input_unit, allocation_ratio)
SELECT
    ('a0000000-0000-0000-0000-' || lpad(to_hex(50000 + ((i - 80001) % 30000) + 1), 12, '0'))::uuid AS parent_lot_id,
    ('a0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid AS child_lot_id,
    ('c0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid AS causing_event_id,
    500.0000 AS input_quantity_used,
    'KG' AS input_unit,
    1.000000 AS allocation_ratio
FROM generate_series(80001, 120000) AS s(i)
ON CONFLICT (parent_lot_id, child_lot_id) DO NOTHING;

-- Level 5 child lots (120,001..160,000) connect to Level 4 parent lots (80,001..120,000)
INSERT INTO lot_lineage_edges (parent_lot_id, child_lot_id, causing_event_id, input_quantity_used, input_unit, allocation_ratio)
SELECT
    ('a0000000-0000-0000-0000-' || lpad(to_hex(80000 + ((i - 120001) % 40000) + 1), 12, '0'))::uuid AS parent_lot_id,
    ('a0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid AS child_lot_id,
    ('c0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid AS causing_event_id,
    2500.0000 AS input_quantity_used,
    'PIECES' AS input_unit,
    1.000000 AS allocation_ratio
FROM generate_series(120001, 160000) AS s(i)
ON CONFLICT (parent_lot_id, child_lot_id) DO NOTHING;

-- Level 6 child lots (160,001..200,000) connect to Level 5 parent lots (120,001..160,000)
INSERT INTO lot_lineage_edges (parent_lot_id, child_lot_id, causing_event_id, input_quantity_used, input_unit, allocation_ratio)
SELECT
    ('a0000000-0000-0000-0000-' || lpad(to_hex(120000 + ((i - 160001) % 40000) + 1), 12, '0'))::uuid AS parent_lot_id,
    ('a0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid AS child_lot_id,
    ('c0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid AS causing_event_id,
    2500.0000 AS input_quantity_used,
    'PIECES' AS input_unit,
    1.000000 AS allocation_ratio
FROM generate_series(160001, 200000) AS s(i)
ON CONFLICT (parent_lot_id, child_lot_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 4. Populate 2,000,000 Immutable Hash-Chained Lot Events (10 events / lot)
-- ----------------------------------------------------------------------------
INSERT INTO lot_events (
    id,
    lot_id,
    sequence_no,
    event_type,
    facility_id,
    input_quantity,
    output_quantity,
    loss_percentage,
    unit,
    step_water_liters,
    step_energy_kwh,
    step_co2e_kg,
    mass_balance_flagged,
    previous_event_hash,
    event_hash,
    occurred_at,
    recorded_at
)
SELECT
    ('e0000000-0000-0000-0000-' || lpad(to_hex(e), 12, '0'))::uuid AS id,
    ('a0000000-0000-0000-0000-' || lpad(to_hex(lot_idx), 12, '0'))::uuid AS lot_id,
    seq_no AS sequence_no,
    CASE seq_no
        WHEN 1 THEN 'RECEIVE'
        WHEN 2 THEN 'SPIN'
        WHEN 3 THEN 'WEAVE'
        WHEN 4 THEN 'DYE'
        WHEN 5 THEN 'WASH'
        WHEN 6 THEN 'CUT'
        WHEN 7 THEN 'SEW'
        WHEN 8 THEN 'FINISH'
        WHEN 9 THEN 'PACK'
        ELSE        'SHIP'
    END AS event_type,
    CASE
        WHEN lot_idx <= 20000  THEN (SELECT fac_spin FROM seed_constants)
        WHEN lot_idx <= 50000  THEN (SELECT fac_fab FROM seed_constants)
        WHEN lot_idx <= 80000  THEN (SELECT fac_dye FROM seed_constants)
        WHEN lot_idx <= 120000 THEN (SELECT fac_cut FROM seed_constants)
        WHEN lot_idx <= 160000 THEN (SELECT fac_sew FROM seed_constants)
        ELSE                        (SELECT fac_pack FROM seed_constants)
    END AS facility_id,
    1000.0000 - (seq_no * 15.0) AS input_quantity,
    980.0000 - (seq_no * 15.0) AS output_quantity,
    2.00 AS loss_percentage,
    CASE WHEN lot_idx <= 80000 THEN 'KG' WHEN lot_idx <= 160000 THEN 'PIECES' ELSE 'CARTONS' END AS unit,
    25.5000 AS step_water_liters,
    6.2000 AS step_energy_kwh,
    1.4500 AS step_co2e_kg,
    FALSE AS mass_balance_flagged,
    CASE
        WHEN seq_no = 1 THEN '0000000000000000000000000000000000000000000000000000000000000000'
        ELSE md5('ev-' || lot_idx || '-' || (seq_no - 1)) || md5('salt-' || lot_idx || '-' || (seq_no - 1))
    END AS previous_event_hash,
    md5('ev-' || lot_idx || '-' || seq_no) || md5('salt-' || lot_idx || '-' || seq_no) AS event_hash,
    CURRENT_TIMESTAMP - (2000000 - e) * interval '1 second' AS occurred_at,
    CURRENT_TIMESTAMP - (2000000 - e) * interval '1 second' AS recorded_at
FROM (
    SELECT
        e,
        ((e - 1) / 10) + 1 AS lot_idx,
        ((e - 1) % 10) + 1 AS seq_no
    FROM generate_series(1, 2000000) AS g(e)
) sub
ON CONFLICT (lot_id, sequence_no) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 5. Published Digital Product Passports for Leaf Shipped Carton Lots
-- ----------------------------------------------------------------------------
INSERT INTO digital_passports (
    id,
    style_code,
    shipment_code,
    brand_id,
    root_carton_lot_id,
    version_no,
    is_latest,
    qr_code_token,
    etag_hash,
    public_payload,
    brand_payload,
    auditor_payload,
    published_at
)
SELECT
    ('d0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid AS id,
    'STYLE-TSHIRT-' || lpad(((i % 500) + 1)::text, 4, '0') AS style_code,
    'SHIP-2026-' || lpad(((i % 200) + 1)::text, 4, '0') AS shipment_code,
    (SELECT brand_id FROM seed_constants) AS brand_id,
    ('a0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0'))::uuid AS root_carton_lot_id,
    1 AS version_no,
    TRUE AS is_latest,
    'QR-BENCH-' || lpad(i::text, 7, '0') AS qr_code_token,
    md5('etag-' || i) || md5('salt-' || i) AS etag_hash,
    jsonb_build_object(
        'passportId', ('d0000000-0000-0000-0000-' || lpad(to_hex(i), 12, '0')),
        'styleCode', 'STYLE-TSHIRT-' || lpad(((i % 500) + 1)::text, 4, '0'),
        'productName', 'Organic Cotton Everyday Crewneck',
        'brandName', 'Patagonia Global',
        'versionNo', 1,
        'fibreComposition', jsonb_build_array(
            jsonb_build_object('fibreType', 'ORGANIC_COTTON', 'percentage', 80.0),
            jsonb_build_object('fibreType', 'RECYCLED_POLYESTER', 'percentage', 20.0)
        ),
        'origins', jsonb_build_array(
            jsonb_build_object('stepName', 'SPINNING', 'countryCode', 'BGD', 'facilityType', 'MILL'),
            jsonb_build_object('stepName', 'SEWING', 'countryCode', 'BGD', 'facilityType', 'FACTORY')
        ),
        'certifications', jsonb_build_array(
            jsonb_build_object('certificateType', 'GOTS', 'claimType', 'Organic Content', 'certifiedPercentage', 80.0, 'status', 'ACTIVE')
        ),
        'footprintWaterLiters', 245.50,
        'footprintEnergyKwh', 42.10,
        'footprintCo2eKg', 6.85,
        'careInstructions', 'Machine wash cold. Line dry in shade.',
        'recyclingGuidance', '100% recyclable through Patagonia Worn Wear program.'
    ) AS public_payload,
    jsonb_build_object('brandCode', 'BRAND-PAT-001', 'marginTier', 'STANDARD') AS brand_payload,
    jsonb_build_object('auditorFlag', 'CLEAN', 'massBalanceStatus', 'VERIFIED') AS auditor_payload,
    CURRENT_TIMESTAMP AS published_at
FROM generate_series(160001, 200000) AS s(i)
ON CONFLICT (style_code, shipment_code, version_no) DO NOTHING;

DROP TABLE seed_constants;

COMMIT;

-- ----------------------------------------------------------------------------
-- 6. Benchmark Sample IDs for QA1 Verification
-- ----------------------------------------------------------------------------
SELECT '=== 5 SAMPLE ROOT FABRIC LOT IDS (FOR FORWARD TRACE) ===' AS benchmark_sample_header;
SELECT id AS root_fabric_lot_id, lot_number, status, current_quantity, unit
FROM lots
WHERE lot_number IN (
    'LOT-BENCH-0000001',
    'LOT-BENCH-0000002',
    'LOT-BENCH-0000003',
    'LOT-BENCH-0000004',
    'LOT-BENCH-0000005'
)
ORDER BY lot_number;

SELECT '=== 5 SAMPLE SHIPPED CARTON LOT IDS (FOR BACKWARD TRACE) ===' AS benchmark_sample_header;
SELECT id AS shipped_carton_lot_id, lot_number, status, current_quantity, unit
FROM lots
WHERE lot_number IN (
    'LOT-BENCH-0160001',
    'LOT-BENCH-0160002',
    'LOT-BENCH-0160003',
    'LOT-BENCH-0160004',
    'LOT-BENCH-0160005'
)
ORDER BY lot_number;
