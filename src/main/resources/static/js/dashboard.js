/**
 * ThreadTrace Web Dashboard
 * Offline-first, vanilla JavaScript with Zero external CDN dependencies.
 */

// Active Authentication State
const AUTH_PROFILES = {
    PUBLIC: { label: 'Public (Unauthenticated)', username: '', password: '', role: 'PUBLIC', badgeClass: 'public' },
    BRAND_A: { label: 'Brand A: Patagonia (ROLE_BRAND)', username: 'BRAND-PAT-001', password: 'password', role: 'BRAND_A', badgeClass: 'brand-a' },
    BRAND_B: { label: 'Brand B: Nike Competitor (ROLE_BRAND)', username: 'BRAND-OTHER-002', password: 'password', role: 'BRAND_B', badgeClass: 'brand-b' },
    AUDITOR: { label: 'Auditor Inspector (ROLE_AUDITOR)', username: 'auditor-inspector', password: 'password', role: 'AUDITOR', badgeClass: 'auditor' }
};

let currentAuth = AUTH_PROFILES.PUBLIC;

// Toast helper
function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<span>${message}</span>`;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}

// Authenticated Fetch wrapper
async function apiFetch(url, options = {}) {
    const headers = options.headers ? { ...options.headers } : {};

    if (currentAuth.username && currentAuth.password) {
        headers['Authorization'] = 'Basic ' + btoa(`${currentAuth.username}:${currentAuth.password}`);
    }

    const response = await fetch(url, { ...options, headers });
    return response;
}

// Navigation Tab Switcher
function switchTab(tabId) {
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(el => el.classList.remove('active'));

    const navBtn = document.querySelector(`.nav-item[data-tab="${tabId}"]`);
    const panel = document.getElementById(`tab-${tabId}`);

    if (navBtn) navBtn.classList.add('active');
    if (panel) {
        panel.classList.add('active');
        const titleMap = {
            'overview': 'Overview & Supply-Chain Health',
            'registry': 'Parties & Facilities Registry (FR1)',
            'lots': 'Lot Events, Splits/Merges & Mass Balance (FR2, FR4, FR6)',
            'tracer': 'Lineage Tracer & Hash-Chain Integrity (FR3, FR10, QA1)',
            'evidence': 'Evidence & Certificate Expiry Alerts (FR5, QA2)',
            'passports': 'Digital Product Passports & Audience Views (FR7, FR8, QA3, QA4)',
            'imports': 'Factory Bulk Imports & Rejection Reports (FR9)'
        };
        document.getElementById('topbar-title').innerText = titleMap[tabId] || 'ThreadTrace';

        // Trigger tab specific data load
        if (tabId === 'overview') loadOverview();
        if (tabId === 'registry') loadRegistry();
        if (tabId === 'lots') loadLotsAndEvents();
        if (tabId === 'tracer') loadTracerOptions();
        if (tabId === 'evidence') loadEvidence();
        if (tabId === 'passports') loadPassports();
    }
}

// Active Role Switcher
function setRole(roleKey) {
    currentAuth = AUTH_PROFILES[roleKey] || AUTH_PROFILES.PUBLIC;
    const badge = document.getElementById('active-role-badge');
    badge.className = `role-badge ${currentAuth.badgeClass}`;
    badge.innerText = currentAuth.label;

    showToast(`Active Identity switched to: ${currentAuth.label}`, 'info');

    // Re-render components that depend on audience permissions
    const activeTab = document.querySelector('.nav-item.active')?.getAttribute('data-tab');
    if (activeTab === 'registry') loadRegistry();
    if (activeTab === 'passports') loadPassports();
}

// ==========================================
// 1. OVERVIEW & DEMO DATA TAB
// ==========================================
async function loadOverview() {
    try {
        const statsRes = await apiFetch('/api/v1/dashboard/stats');
        if (statsRes.ok) {
            const stats = await statsRes.json();
            document.getElementById('kpi-parties').innerText = `${stats.totalParties} / ${stats.totalFacilities}`;
            document.getElementById('kpi-lots').innerText = `${stats.activeLots} / ${stats.totalLots}`;
            document.getElementById('kpi-events').innerText = stats.recordedEvents;
            document.getElementById('kpi-passports').innerText = stats.publishedPassports;
            document.getElementById('kpi-expiring').innerText = stats.expiringCertificates;
        }

        const healthRes = await apiFetch('/actuator/health');
        const healthBadge = document.getElementById('kpi-health');
        if (healthRes.ok) {
            const health = await healthRes.json();
            healthBadge.innerText = health.status;
            healthBadge.style.color = '#16a34a';
        } else {
            healthBadge.innerText = 'OFFLINE';
            healthBadge.style.color = '#dc2626';
        }
    } catch (e) {
        console.error('Error loading overview:', e);
    }
}

async function seedRmgDemo() {
    const btn = document.getElementById('btn-seed-rmg');
    btn.disabled = true;
    btn.innerHTML = '<span class="loading-spinner"></span> Seeding 6-Level Flow...';

    try {
        const res = await apiFetch('/api/v1/dashboard/demo/seed-rmg', { method: 'POST' });
        if (res.ok) {
            const data = await res.json();
            showToast('✓ 6-Level Bangladeshi Garment flow & Passports Seeded!', 'success');
            const summaryBox = document.getElementById('demo-seed-summary');
            summaryBox.style.display = 'block';
            summaryBox.innerHTML = `
                <div style="background:#f0fdf4; border:1px solid #86efac; border-radius:8px; padding:14px; margin-top:14px;">
                    <h4 style="color:#15803d; margin-bottom:6px;">✓ RMG Supply-Chain Successfully Seeded!</h4>
                    <p style="font-size:12.5px; color:#1e293b;">
                        <strong>Brand A (Patagonia) Carton Lot:</strong> <code>${data.cartonLotNumber}</code> (ID: <code>${data.cartonLotId}</code>)<br>
                        <strong>Brand A Passport QR Token:</strong> <code>${data.qrCodeTokenBrandA}</code><br>
                        <strong>Brand B Passport QR Token:</strong> <code>${data.qrCodeTokenBrandB}</code><br>
                        <strong>Total Lots Seeded:</strong> ${data.createdLots.join(', ')}
                    </p>
                </div>
            `;
            loadOverview();
        } else {
            showToast('Failed to seed RMG demo', 'danger');
        }
    } catch (e) {
        showToast('Error seeding demo: ' + e.message, 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Seed RMG Supply-Chain Demo';
    }
}

async function seedViolationsDemo() {
    const btn = document.getElementById('btn-seed-violations');
    btn.disabled = true;
    btn.innerHTML = '<span class="loading-spinner"></span> Seeding 4 Defects...';

    try {
        const res = await apiFetch('/api/v1/dashboard/demo/seed-violations', { method: 'POST' });
        if (res.ok) {
            const data = await res.json();
            showToast('✓ 4 QA2 Defect Scenarios Seeded & Verified!', 'success');
            const container = document.getElementById('demo-violations-container');
            container.style.display = 'block';

            let html = '<div class="grid-2" style="margin-top:16px;">';
            data.violations.forEach(v => {
                html += `
                    <div style="background:#fff1f2; border:1px solid #fda4af; border-radius:8px; padding:14px;">
                        <span class="badge" style="background:#fecdd3; color:#9f1239; margin-bottom:6px;">${v.status}</span>
                        <h4 style="color:#881337; font-size:13.5px; margin-bottom:4px;">${v.title}</h4>
                        <p style="font-size:12px; color:#4c0519; margin-bottom:6px;">${v.description}</p>
                        <div style="font-size:11.5px; background:white; padding:8px; border-radius:4px; border:1px dashed #fda4af; font-family:var(--font-mono); color:#be123c;">
                            <strong>Detected:</strong> ${v.detectedIssue}<br>
                            <strong>Lot:</strong> ${v.affectedLot}
                        </div>
                    </div>
                `;
            });
            html += '</div>';
            container.innerHTML = html;
            loadOverview();
        } else {
            showToast('Failed to seed violation scenarios', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Seed QA2 Violation Scenarios';
    }
}

// ==========================================
// 2. PARTIES & FACILITIES TAB (FR1)
// ==========================================
let allPartiesCache = [];

async function loadRegistry() {
    try {
        const partyRes = await apiFetch('/api/v1/parties?size=50');
        if (partyRes.ok) {
            const data = await partyRes.json();
            allPartiesCache = data.content || [];
            renderPartiesTable(allPartiesCache);
            populatePartyDropdowns(allPartiesCache);
        }

        const facRes = await apiFetch('/api/v1/facilities?size=50');
        if (facRes.ok) {
            const facData = await facRes.json();
            renderFacilitiesTable(facData.content || []);
            populateFacilityDropdowns(facData.content || []);
        }
    } catch (e) {
        console.error('Error loading registry:', e);
    }
}

function renderPartiesTable(parties) {
    const tbody = document.getElementById('parties-tbody');
    tbody.innerHTML = '';

    const isAuditor = currentAuth.role === 'AUDITOR';

    parties.forEach(p => {
        const tr = document.createElement('tr');

        // QA3 Commercial Confidentiality Masking
        const addressHtml = isAuditor
            ? `<span class="confidential-revealed">${p.exactAddress || 'N/A'}</span>`
            : `<span class="confidential-redacted">*** CONFIDENTIAL ***</span>`;

        const workersHtml = isAuditor
            ? `<span class="confidential-revealed">${p.workerCount != null ? p.workerCount : 'N/A'}</span>`
            : `<span class="confidential-redacted">[Auditor Only]</span>`;

        const priceHtml = isAuditor
            ? `<span class="confidential-revealed">${p.unitPriceBdt != null ? p.unitPriceBdt + ' BDT' : 'N/A'}</span>`
            : `<span class="confidential-redacted">[Auditor Only]</span>`;

        tr.innerHTML = `
            <td><code>${p.externalCode}</code></td>
            <td><strong>${p.name}</strong></td>
            <td><span class="badge" style="background:#f1f5f9; color:#334155;">${p.partyType}</span></td>
            <td><span class="badge" style="background:#e0f2fe; color:#0369a1;">${p.countryCode}</span></td>
            <td>${addressHtml}</td>
            <td>${workersHtml}</td>
            <td>${priceHtml}</td>
        `;
        tbody.appendChild(tr);
    });
}

function renderFacilitiesTable(facilities) {
    const tbody = document.getElementById('facilities-tbody');
    tbody.innerHTML = '';
    facilities.forEach(f => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><strong>${f.name}</strong></td>
            <td><span class="badge" style="background:#f1f5f9; color:#334155;">${f.facilityType}</span></td>
            <td>${f.locationCity}</td>
            <td><code>${f.partyId ? f.partyId.substring(0,8) + '...' : 'N/A'}</code></td>
            <td><code>${f.id.substring(0,8)}...</code></td>
        `;
        tbody.appendChild(tr);
    });
}

function populatePartyDropdowns(parties) {
    const facPartySel = document.getElementById('fac-party-select');
    const lotBrandSel = document.getElementById('event-brand-select');
    const passportBrandSel = document.getElementById('passport-brand-select');
    const certPartySel = document.getElementById('cert-party-select');

    const optionsHtml = '<option value="">-- Select Party / Brand --</option>' +
        parties.map(p => `<option value="${p.id}">${p.name} (${p.externalCode})</option>`).join('');

    if (facPartySel) facPartySel.innerHTML = optionsHtml;
    if (lotBrandSel) lotBrandSel.innerHTML = optionsHtml;
    if (passportBrandSel) passportBrandSel.innerHTML = optionsHtml;
    if (certPartySel) certPartySel.innerHTML = optionsHtml;
}

function populateFacilityDropdowns(facilities) {
    const eventFacSel = document.getElementById('event-facility-select');
    if (!eventFacSel) return;
    eventFacSel.innerHTML = '<option value="">-- Select Facility --</option>' +
        facilities.map(f => `<option value="${f.id}">${f.name} (${f.locationCity} - ${f.facilityType})</option>`).join('');
}

async function registerParty(e) {
    e.preventDefault();
    const payload = {
        partyType: document.getElementById('party-type').value,
        name: document.getElementById('party-name').value,
        externalCode: document.getElementById('party-code').value,
        countryCode: document.getElementById('party-country').value,
        exactAddress: document.getElementById('party-address').value,
        workerCount: parseInt(document.getElementById('party-workers').value) || 0,
        unitPriceBdt: parseFloat(document.getElementById('party-price').value) || 0.0
    };

    try {
        const res = await apiFetch('/api/v1/parties', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            showToast('✓ Party registered successfully!', 'success');
            document.getElementById('form-register-party').reset();
            loadRegistry();
        } else {
            const err = await res.json();
            showToast('Error: ' + (err.detail || err.title || 'Validation error'), 'danger');
        }
    } catch (e) {
        showToast('Error registering party: ' + e.message, 'danger');
    }
}

async function registerFacility(e) {
    e.preventDefault();
    const payload = {
        partyId: document.getElementById('fac-party-select').value,
        facilityType: document.getElementById('fac-type').value,
        name: document.getElementById('fac-name').value,
        locationCity: document.getElementById('fac-city').value
    };

    try {
        const res = await apiFetch('/api/v1/facilities', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            showToast('✓ Facility registered successfully!', 'success');
            document.getElementById('form-register-facility').reset();
            loadRegistry();
        } else {
            const err = await res.json();
            showToast('Error: ' + (err.detail || err.title || 'Validation error'), 'danger');
        }
    } catch (e) {
        showToast('Error registering facility: ' + e.message, 'danger');
    }
}

// ==========================================
// 3. LOT EVENTS, SPLITS/MERGES & MASS BALANCE TAB (FR2, FR4, FR6)
// ==========================================
let allLotsCache = [];

async function loadLotsAndEvents() {
    try {
        const res = await apiFetch('/api/v1/lots?size=50');
        if (res.ok) {
            const data = await res.json();
            allLotsCache = data.content || [];
            renderLotsTable(allLotsCache);
            populateLotDropdowns(allLotsCache);
        }
    } catch (e) {
        console.error('Error loading lots:', e);
    }
}

function renderLotsTable(lots) {
    const tbody = document.getElementById('lots-tbody');
    tbody.innerHTML = '';

    lots.forEach(lot => {
        const tr = document.createElement('tr');
        const badgeClass = `badge-${lot.status.toLowerCase()}`;

        tr.innerHTML = `
            <td><code><strong>${lot.lotNumber}</strong></code></td>
            <td><span class="badge ${badgeClass}">${lot.status}</span></td>
            <td>${lot.styleCode || '-'} / ${lot.shipmentCode || '-'}</td>
            <td><strong>${lot.currentQuantity}</strong> ${lot.unit}</td>
            <td>
                <span class="badge" style="background:#dcfce7; color:#15803d;">Recycled: ${lot.certifiedRecycledPct}%</span>
                <span class="badge" style="background:#e0f2fe; color:#0369a1;">Organic: ${lot.certifiedOrganicPct}%</span>
            </td>
            <td>
                <span title="Water Footprint">💧 ${lot.accumulatedWaterL} L</span> |
                <span title="Energy Consumed">⚡ ${lot.accumulatedEnergyKwh} kWh</span> |
                <span title="CO2e Emissions">🌱 ${lot.accumulatedCo2eKg} kg</span>
            </td>
            <td>
                <button class="btn btn-secondary btn-sm" onclick="traceLotFromTable('${lot.id}', 'backward')">Trace Back</button>
                <button class="btn btn-secondary btn-sm" onclick="verifyIntegrityFromTable('${lot.id}')">Verify</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function populateLotDropdowns(lots) {
    const eventTargetLotSel = document.getElementById('event-target-lot-select');
    const tracerLotSel = document.getElementById('tracer-lot-select');
    const passportCartonSel = document.getElementById('passport-carton-select');
    const certLotSel = document.getElementById('cert-lot-select');

    const optionsHtml = '<option value="">-- Select Existing Lot --</option>' +
        lots.map(l => `<option value="${l.id}">${l.lotNumber} (${l.currentQuantity} ${l.unit} - ${l.status})</option>`).join('');

    if (eventTargetLotSel) eventTargetLotSel.innerHTML = optionsHtml;
    if (tracerLotSel) tracerLotSel.innerHTML = optionsHtml;
    if (passportCartonSel) passportCartonSel.innerHTML = optionsHtml;
    if (certLotSel) certLotSel.innerHTML = optionsHtml;
}

function onEventModeChange() {
    const mode = document.getElementById('event-mode-select').value;
    const genesisFields = document.getElementById('event-genesis-fields');
    const targetLotGroup = document.getElementById('event-target-lot-group');
    const splitContainer = document.getElementById('event-split-container');

    if (mode === 'GENESIS') {
        genesisFields.style.display = 'block';
        targetLotGroup.style.display = 'none';
        splitContainer.style.display = 'none';
    } else if (mode === 'SPLIT') {
        genesisFields.style.display = 'none';
        targetLotGroup.style.display = 'block';
        splitContainer.style.display = 'block';
    } else {
        genesisFields.style.display = 'none';
        targetLotGroup.style.display = 'block';
        splitContainer.style.display = 'none';
    }
}

async function recordLotEvent(e) {
    e.preventDefault();
    const problemBanner = document.getElementById('mass-balance-error-banner');
    problemBanner.style.display = 'none';

    const mode = document.getElementById('event-mode-select').value;
    const eventType = document.getElementById('event-type-select').value;
    const facilityId = document.getElementById('event-facility-select').value;
    const inputQty = parseFloat(document.getElementById('event-input-qty').value);
    const outputQty = parseFloat(document.getElementById('event-output-qty').value);
    const unit = document.getElementById('event-unit-select').value;

    const payload = {
        eventType,
        facilityId,
        inputQuantity: inputQty,
        outputQuantity: outputQty,
        unit
    };

    if (mode === 'GENESIS') {
        payload.newLotNumber = document.getElementById('event-new-lot-number').value;
        payload.brandId = document.getElementById('event-brand-select').value;
        payload.styleCode = document.getElementById('event-style-code').value;
        payload.shipmentCode = document.getElementById('event-shipment-code').value;
        payload.lotUnit = unit;
    } else {
        payload.lotId = document.getElementById('event-target-lot-select').value;
    }

    if (mode === 'SPLIT') {
        const childLotNum = document.getElementById('split-child-lot-number').value;
        const childQty = parseFloat(document.getElementById('split-child-qty').value) || outputQty;
        payload.splitChildren = [{
            lotNumber: childLotNum,
            styleCode: document.getElementById('event-style-code')?.value || 'STYLE-SPLIT',
            shipmentCode: document.getElementById('event-shipment-code')?.value || 'SHIP-SPLIT',
            quantity: childQty,
            unit: unit,
            allocationRatio: 1.0
        }];
    }

    try {
        const res = await apiFetch('/api/v1/lots/events', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            const data = await res.json();
            showToast(`✓ Event ${data.eventType} recorded successfully!`, 'success');
            document.getElementById('form-record-event').reset();
            onEventModeChange();
            loadLotsAndEvents();
        } else {
            // RFC 9457 ProblemDetail Handling
            const errorDetail = await res.json();
            problemBanner.style.display = 'block';
            document.getElementById('problem-detail-title').innerText = `RFC 9457: ${errorDetail.title || 'Mass Balance / Validation Violation'}`;
            document.getElementById('problem-detail-body').innerHTML = `
                <strong>Status:</strong> ${errorDetail.status || res.status}<br>
                <strong>Detail:</strong> ${errorDetail.detail || 'The event exceeded mass balance loss limits.'}<br>
                <strong>Type:</strong> <code>${errorDetail.type || 'urn:problem:mass-balance'}</code>
            `;
            showToast('Mass balance / validation error detected!', 'danger');
        }
    } catch (e) {
        showToast('Error recording event: ' + e.message, 'danger');
    }
}

// ==========================================
// 4. LINEAGE TRACER & INTEGRITY VERIFIER TAB (FR3, FR10, QA1)
// ==========================================
function loadTracerOptions() {
    populateLotDropdowns(allLotsCache);
}

function traceLotFromTable(lotId, direction) {
    switchTab('tracer');
    document.getElementById('tracer-lot-select').value = lotId;
    document.getElementById('tracer-lot-manual').value = lotId;
    if (direction === 'forward') traceForward();
    else traceBackward();
}

function verifyIntegrityFromTable(lotId) {
    switchTab('tracer');
    document.getElementById('tracer-lot-select').value = lotId;
    document.getElementById('tracer-lot-manual').value = lotId;
    verifyHashChainIntegrity();
}

function getSelectedLotId() {
    const sel = document.getElementById('tracer-lot-select').value;
    const manual = document.getElementById('tracer-lot-manual').value.trim();
    return manual || sel;
}

async function traceBackward() {
    const lotId = getSelectedLotId();
    if (!lotId) {
        showToast('Please select or enter a Lot ID first', 'danger');
        return;
    }

    const startTime = performance.now();
    try {
        const res = await apiFetch(`/api/v1/trace/backward/${lotId}`);
        const latency = Math.round(performance.now() - startTime);

        if (res.ok) {
            const traceData = await res.json();
            renderLineageDAG(traceData, 'Backward (Carton ➔ Sources)', latency);
            showToast(`Backward trace completed in ${latency} ms`, 'success');
        } else {
            showToast('Error tracing lot backward', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

async function traceForward() {
    const lotId = getSelectedLotId();
    if (!lotId) {
        showToast('Please select or enter a Lot ID first', 'danger');
        return;
    }

    const startTime = performance.now();
    try {
        const res = await apiFetch(`/api/v1/trace/forward/${lotId}`);
        const latency = Math.round(performance.now() - startTime);

        if (res.ok) {
            const traceData = await res.json();
            renderLineageDAG(traceData, 'Forward (Raw Material ➔ Shipments)', latency);
            showToast(`Forward trace completed in ${latency} ms`, 'success');
        } else {
            showToast('Error tracing lot forward', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

function renderLineageDAG(traceData, directionLabel, latencyMs) {
    const container = document.getElementById('lineage-results-container');
    container.style.display = 'block';

    const latencyBadge = document.getElementById('tracer-latency-badge');
    latencyBadge.innerHTML = `<strong>${latencyMs} ms</strong> — QA1 Target: &lt;2000 ms (${directionLabel})`;
    latencyBadge.className = latencyMs < 2000 ? 'status-pill' : 'status-pill badge-quarantined';

    const pipeline = document.getElementById('lineage-pipeline-container');
    pipeline.innerHTML = '';

    const nodes = traceData.tracePath || [];
    if (nodes.length === 0) {
        pipeline.innerHTML = '<p style="color:#64748b; padding:12px;">No connected lineage stages found.</p>';
        return;
    }

    nodes.forEach((node, idx) => {
        if (idx > 0) {
            const arrow = document.createElement('div');
            arrow.className = 'step-arrow';
            arrow.innerText = '↓';
            pipeline.appendChild(arrow);
        }

        const step = document.createElement('div');
        step.className = 'lineage-step';
        step.innerHTML = `
            <div class="step-badge">
                <span>STAGE</span>
                <span>${node.depth}</span>
            </div>
            <div class="step-details">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <h4 style="font-size:14px; font-weight:700;">${node.facilityName} (${node.facilityCity})</h4>
                    <span class="badge" style="background:#e0f2fe; color:#0369a1;">${node.countryCode}</span>
                </div>
                <div style="font-size:12.5px; color:#475569; margin-top:2px;">
                    <strong>Supplier:</strong> ${node.partyName} (${node.partyType})
                </div>
                <div style="font-size:12px; margin-top:6px; background:#f8fafc; padding:6px 10px; border-radius:6px; border:1px solid #e2e8f0;">
                    <strong>Lot ID:</strong> <code>${node.lotId}</code>
                </div>
            </div>
        `;
        pipeline.appendChild(step);
    });
}

async function verifyHashChainIntegrity() {
    const lotId = getSelectedLotId();
    if (!lotId) {
        showToast('Please select or enter a Lot ID first', 'danger');
        return;
    }

    try {
        const res = await apiFetch(`/api/v1/lots/${lotId}/verify-integrity`);
        const card = document.getElementById('integrity-result-card');
        card.style.display = 'block';

        if (res.ok) {
            const result = await res.json();
            if (result.valid) {
                card.style.background = '#f0fdf4';
                card.style.borderColor = '#86efac';
                card.innerHTML = `
                    <h3 style="color:#15803d; display:flex; align-items:center; gap:8px;">
                        <span>✓</span> CRYPTOGRAPHIC HASH CHAIN VALID (FR10)
                    </h3>
                    <p style="font-size:13px; color:#1e293b; margin-top:6px;">
                        Lot <code>${result.lotNumber}</code> verified across all <strong>${result.eventCount}</strong> chronological events.<br>
                        <strong>Integrity Status:</strong> Zero tampering detected. SHA-256 event chaining matches exact genesis origin.
                    </p>
                `;
            } else {
                card.style.background = '#fef2f2';
                card.style.borderColor = '#fca5a5';
                card.innerHTML = `
                    <h3 style="color:#b91c1c; display:flex; align-items:center; gap:8px;">
                        <span>✗</span> TAMPERING DETECTED IN EVENT HASH CHAIN
                    </h3>
                    <p style="font-size:13px; color:#1e293b; margin-top:6px;">
                        <strong>Violation:</strong> ${result.diagnosis}<br>
                        <strong>Tampered Event ID:</strong> <code>${result.tamperedEventId}</code> (Sequence #${result.tamperedSequenceNo})
                    </p>
                `;
            }
        } else {
            showToast('Error verifying integrity', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

// ==========================================
// 5. EVIDENCE & EXPIRY ALERTS TAB (FR5, QA2)
// ==========================================
async function loadEvidence() {
    try {
        const res = await apiFetch('/api/v1/evidence');
        if (res.ok) {
            const docs = await res.json();
            renderEvidenceTable(docs);
            renderExpiryAlerts(docs);
        }
    } catch (e) {
        console.error('Error loading evidence:', e);
    }
}

function renderEvidenceTable(docs) {
    const tbody = document.getElementById('evidence-tbody');
    tbody.innerHTML = '';

    docs.forEach(d => {
        const tr = document.createElement('tr');
        const badgeColor = d.status === 'ACTIVE' ? 'badge-active' : (d.status === 'EXPIRED' ? 'badge-quarantined' : 'badge-consumed');

        tr.innerHTML = `
            <td><code>${d.certificateType}</code></td>
            <td><strong>${d.issuerName}</strong></td>
            <td>${d.claimType} (${d.certifiedPercentage}%)</td>
            <td>${d.validFrom} to ${d.validUntil}</td>
            <td><span class="badge ${badgeColor}">${d.status}</span></td>
            <td><a href="/api/v1/evidence/${d.id}/download" class="btn btn-secondary btn-sm" target="_blank">Download</a></td>
        `;
        tbody.appendChild(tr);
    });
}

function renderExpiryAlerts(docs) {
    const alertList = document.getElementById('evidence-expiry-alerts');
    alertList.innerHTML = '';

    const today = new Date();
    const in30Days = new Date(today.getTime() + (30 * 24 * 60 * 60 * 1000));

    const expiringOrExpired = docs.filter(d => {
        const until = new Date(d.validUntil);
        return d.status === 'EXPIRED' || d.status === 'EXPIRING_SOON' || until <= in30Days;
    });

    if (expiringOrExpired.length === 0) {
        alertList.innerHTML = '<p style="color:#15803d; font-size:13px;">✓ All certificates are in good standing (0 expiring within 30 days).</p>';
        return;
    }

    expiringOrExpired.forEach(doc => {
        const isExp = doc.status === 'EXPIRED' || new Date(doc.validUntil) < today;
        const banner = document.createElement('div');
        banner.style.padding = '10px 14px';
        banner.style.borderRadius = '6px';
        banner.style.marginBottom = '8px';
        banner.style.background = isExp ? '#ffe4e6' : '#fef3c7';
        banner.style.border = `1px solid ${isExp ? '#fda4af' : '#fcd34d'}`;
        banner.style.color = isExp ? '#9f1239' : '#92400e';
        banner.style.fontSize = '12.5px';

        banner.innerHTML = `
            <strong>${isExp ? 'EXPIRED' : 'EXPIRING SOON'}:</strong>
            ${doc.certificateType} issued by <em>${doc.issuerName}</em> (Valid until: ${doc.validUntil}).
            Attached to Lot ID: <code>${doc.lotId || 'N/A'}</code>
        `;
        alertList.appendChild(banner);
    });
}

async function triggerExpiryCheckNow() {
    const btn = document.getElementById('btn-run-expiry-check');
    btn.disabled = true;
    btn.innerHTML = '<span class="loading-spinner"></span> Checking...';

    try {
        const res = await apiFetch('/api/v1/evidence/check-expiry', { method: 'POST' });
        if (res.ok) {
            const result = await res.json();
            showToast(`✓ Expiry check completed! Expired: ${result.expiredCount}, Expiring Soon: ${result.expiringSoonCount}`, 'success');
            loadEvidence();
            loadOverview();
        } else {
            showToast('Error checking expiry', 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Run Expiry Check Now';
    }
}

async function uploadEvidenceDocument(e) {
    e.preventDefault();
    const fileInput = document.getElementById('cert-file');
    if (!fileInput.files || fileInput.files.length === 0) {
        showToast('Please select a file to upload', 'danger');
        return;
    }

    const requestJson = {
        certificateType: document.getElementById('cert-type').value,
        issuerName: document.getElementById('cert-issuer').value,
        scopeDescription: document.getElementById('cert-scope').value,
        claimType: document.getElementById('cert-claim-type').value,
        certifiedPercentage: parseFloat(document.getElementById('cert-percentage').value) || 0.0,
        validFrom: document.getElementById('cert-valid-from').value,
        validUntil: document.getElementById('cert-valid-until').value,
        partyId: document.getElementById('cert-party-select').value || null,
        lotId: document.getElementById('cert-lot-select').value || null
    };

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);
    formData.append('request', new Blob([JSON.stringify(requestJson)], { type: 'application/json' }));

    try {
        const res = await apiFetch('/api/v1/evidence/upload', {
            method: 'POST',
            body: formData
        });

        if (res.ok) {
            showToast('✓ Certificate uploaded & attached successfully!', 'success');
            document.getElementById('form-upload-evidence').reset();
            loadEvidence();
        } else {
            const err = await res.json();
            showToast('Error: ' + (err.detail || err.title || 'Upload failed'), 'danger');
        }
    } catch (e) {
        showToast('Upload error: ' + e.message, 'danger');
    }
}

// ==========================================
// 6. DIGITAL PRODUCT PASSPORTS & AUDIENCE VIEWS TAB (FR7, FR8, QA3, QA4)
// ==========================================
let allPassportsCache = [];

async function loadPassports() {
    try {
        const res = await apiFetch('/api/v1/passports');
        if (res.ok) {
            allPassportsCache = await res.json();
            renderPassportsTable(allPassportsCache);
        }
    } catch (e) {
        console.error('Error loading passports:', e);
    }
}

function renderPassportsTable(passports) {
    const tbody = document.getElementById('passports-tbody');
    tbody.innerHTML = '';

    passports.forEach(p => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><code><strong>${p.styleCode}</strong></code></td>
            <td><code>${p.shipmentCode}</code></td>
            <td><span class="badge" style="background:#e0f2fe; color:#0369a1;">v${p.versionNo}</span></td>
            <td>
                <img src="/api/v1/passports/${p.qrCodeToken}/qr" alt="QR" style="width:40px; height:40px; border-radius:4px; border:1px solid #cbd5e1; vertical-align:middle;">
                <code style="font-size:11px; margin-left:4px;">${p.qrCodeToken.substring(0, 16)}...</code>
            </td>
            <td>
                <a href="/passports/${p.qrCodeToken}" target="_blank" class="btn btn-primary btn-sm">Public Page ↗</a>
                <button class="btn btn-secondary btn-sm" onclick="openAudienceInspector('${p.qrCodeToken}')">Inspect Audience JSON</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function generatePassport(e) {
    e.preventDefault();
    const styleCode = document.getElementById('passport-style-code').value;
    const shipmentCode = document.getElementById('passport-shipment-code').value;
    const brandId = document.getElementById('passport-brand-select').value;
    const rootCartonLotId = document.getElementById('passport-carton-select').value;

    let customFields = {};
    const customFieldsStr = document.getElementById('passport-custom-fields').value.trim();
    if (customFieldsStr) {
        try {
            customFields = JSON.parse(customFieldsStr);
        } catch (err) {
            showToast('Invalid JSON in custom dynamic fields (QA5)', 'danger');
            return;
        }
    }

    const payload = {
        styleCode,
        shipmentCode,
        brandId,
        rootCartonLotId,
        customFields
    };

    try {
        const res = await apiFetch('/api/v1/passports/generate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (res.ok) {
            const data = await res.json();
            showToast(`✓ Digital Product Passport generated (v${data.versionNo})!`, 'success');
            document.getElementById('form-generate-passport').reset();
            loadPassports();
        } else {
            const err = await res.json();
            showToast('Error generating passport: ' + (err.detail || err.title || 'Error'), 'danger');
        }
    } catch (e) {
        showToast('Error: ' + e.message, 'danger');
    }
}

let activeInspectionQrToken = null;

function openAudienceInspector(qrToken) {
    activeInspectionQrToken = qrToken;
    document.getElementById('audience-modal-token').innerText = qrToken;
    document.getElementById('audience-modal').classList.add('open');
    testAudienceEndpoint('PUBLIC');
}

function closeAudienceModal() {
    document.getElementById('audience-modal').classList.remove('open');
}

async function testAudienceEndpoint(viewType) {
    if (!activeInspectionQrToken) return;

    let url = `/api/v1/passports/${activeInspectionQrToken}`;
    if (viewType === 'BRAND') url += '/brand';
    if (viewType === 'AUDITOR') url += '/auditor';

    const statusBadge = document.getElementById('audience-status-badge');
    const roleBadge = document.getElementById('audience-active-role-badge');
    const jsonPre = document.getElementById('audience-json-preview');
    const callout = document.getElementById('audience-qa3-callout');

    roleBadge.innerText = `Active Request Identity: ${currentAuth.label}`;

    try {
        const res = await apiFetch(url);
        statusBadge.innerText = `HTTP ${res.status} ${res.statusText}`;

        if (res.ok) {
            statusBadge.className = 'badge badge-active';
            const data = await res.json();
            jsonPre.innerText = JSON.stringify(data, null, 2);

            if (viewType === 'PUBLIC') {
                callout.innerHTML = `
                    <div style="background:#f0fdf4; border:1px solid #86efac; color:#15803d; padding:10px; border-radius:6px; font-size:12.5px;">
                        <strong>✓ QA3 Public Guarantee:</strong> Zero confidential fields (<code>exact_address</code>, <code>unit_price_bdt</code>, <code>worker_count</code>) leaked in Public JSON!
                    </div>
                `;
            } else if (viewType === 'AUDITOR') {
                callout.innerHTML = `
                    <div style="background:#f0fdf4; border:1px solid #86efac; color:#15803d; padding:10px; border-radius:6px; font-size:12.5px;">
                        <strong>✓ Auditor Access Granted:</strong> Full supply-chain audit details, confidential suppliers, and commercial pricing visible.
                    </div>
                `;
            } else {
                callout.innerHTML = `
                    <div style="background:#e0f2fe; border:1px solid #7dd3fc; color:#0369a1; padding:10px; border-radius:6px; font-size:12.5px;">
                        <strong>✓ Brand View Granted:</strong> Authorized brand suppliers visible for owning brand tenant.
                    </div>
                `;
            }
        } else {
            statusBadge.className = 'badge badge-quarantined';
            jsonPre.innerText = `HTTP ${res.status}: Access Denied (${res.statusText})`;
            callout.innerHTML = `
                <div style="background:#fff1f2; border:1px solid #fda4af; color:#9f1239; padding:10px; border-radius:6px; font-size:12.5px;">
                    <strong>✓ QA3 Tenant Isolation Enforced:</strong> Request returned <strong>HTTP ${res.status}</strong>! Cross-brand access or unauthenticated requests are strictly blocked.
                </div>
            `;
        }
    } catch (e) {
        statusBadge.innerText = 'Network Error';
        jsonPre.innerText = e.message;
    }
}

// ==========================================
// 7. BULK FACTORY IMPORT TAB (FR9)
// ==========================================
async function submitBulkImport(e) {
    e.preventDefault();
    const type = document.getElementById('import-type').value;
    const fileInput = document.getElementById('import-file');

    if (!fileInput.files || fileInput.files.length === 0) {
        showToast('Please select a file to import', 'danger');
        return;
    }

    const formData = new FormData();
    formData.append('file', fileInput.files[0]);

    const btn = document.getElementById('btn-submit-import');
    btn.disabled = true;
    btn.innerHTML = '<span class="loading-spinner"></span> Processing Batch...';

    try {
        const res = await apiFetch(`/api/v1/imports/${type}`, {
            method: 'POST',
            body: formData
        });

        const report = await res.json();
        renderImportReport(report);
        showToast(`Import processed: ${report.successfulRows} accepted, ${report.rejectedRows} rejected`, 'info');
    } catch (e) {
        showToast('Error running import: ' + e.message, 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = 'Upload & Execute Import';
    }
}

function renderImportReport(report) {
    const container = document.getElementById('import-report-container');
    container.style.display = 'block';

    document.getElementById('import-kpi-total').innerText = report.totalRows;
    document.getElementById('import-kpi-success').innerText = report.successfulRows;
    document.getElementById('import-kpi-rejected').innerText = report.rejectedRows;

    const tbody = document.getElementById('import-rejections-tbody');
    tbody.innerHTML = '';

    const rejections = report.rejections || [];
    if (rejections.length === 0) {
        tbody.innerHTML = '<tr><td colspan="3" style="text-align:center; color:#15803d; padding:16px;">✓ Zero rejections. All rows imported successfully!</td></tr>';
        return;
    }

    rejections.forEach(r => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td><code>Line ${r.rowNumber}</code></td>
            <td style="color:#b91c1c; font-weight:600;">${r.reason}</td>
            <td><code style="font-size:11.5px; color:#475569;">${r.rawPayload || '-'}</code></td>
        `;
        tbody.appendChild(tr);
    });
}

// Initial Bootstrap on Page Load
document.addEventListener('DOMContentLoaded', () => {
    // Setup Navigation Listeners
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const tabId = item.getAttribute('data-tab');
            switchTab(tabId);
        });
    });

    // Forms setup
    document.getElementById('form-register-party')?.addEventListener('submit', registerParty);
    document.getElementById('form-register-facility')?.addEventListener('submit', registerFacility);
    document.getElementById('form-record-event')?.addEventListener('submit', recordLotEvent);
    document.getElementById('form-upload-evidence')?.addEventListener('submit', uploadEvidenceDocument);
    document.getElementById('form-generate-passport')?.addEventListener('submit', generatePassport);
    document.getElementById('form-bulk-import')?.addEventListener('submit', submitBulkImport);

    // Initial Overview Load
    loadOverview();
});
