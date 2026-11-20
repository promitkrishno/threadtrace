// ============================================================================
// ThreadTrace - QA1 Lineage Trace Speed Load Test (k6)
// Targets: GET /api/v1/trace/backward/{lotId} and GET /api/v1/trace/forward/{lotId}
// Dataset: 200,000 lots, 2,000,000 events, 6 supply chain levels
// SLA Assertion: p95 latency < 2000ms (2 seconds) under concurrent load
// ============================================================================

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    lineage_trace_load: {
      executor: 'ramping-vus',
      startVUs: 1,
      stages: [
        { duration: '10s', target: 10 },  // Ramp up to 10 concurrent investigators
        { duration: '30s', target: 20 },  // Sustained load
        { duration: '10s', target: 0 },   // Ramp down
      ],
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    // Quality Attribute QA1 SLA: p95 < 2000 ms (2 seconds)
    http_req_duration: ['p(95)<2000', 'p(99)<3000'],
    http_req_failed: ['rate<0.01'], // 99% success rate
    checks: ['rate>0.99'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Sample Root Lots (Level 1 Spinning / Fabric - for Forward Trace)
const ROOT_LOT_IDS = [
  'a0000000-0000-0000-0000-000000000001',
  'a0000000-0000-0000-0000-000000000002',
  'a0000000-0000-0000-0000-000000000003',
  'a0000000-0000-0000-0000-000000000004',
  'a0000000-0000-0000-0000-000000000005',
];

// Sample Shipped Carton Lots (Level 6 Carton / DPP - for Backward Trace across 6 levels)
const CARTON_LOT_IDS = [
  'a0000000-0000-0000-0000-000000027101', // lot 160001 (0x27101)
  'a0000000-0000-0000-0000-000000027102', // lot 160002 (0x27102)
  'a0000000-0000-0000-0000-000000027103', // lot 160003 (0x27103)
  'a0000000-0000-0000-0000-000000027104', // lot 160004 (0x27104)
  'a0000000-0000-0000-0000-000000027105', // lot 160005 (0x27105)
];

export default function () {
  const cartonLotId = CARTON_LOT_IDS[Math.floor(Math.random() * CARTON_LOT_IDS.length)];
  const rootLotId = ROOT_LOT_IDS[Math.floor(Math.random() * ROOT_LOT_IDS.length)];

  // 1. Backward Trace: Carton -> Assembly -> Cut -> Dye -> Fabric -> Yarn (6 levels)
  const backwardRes = http.get(`${BASE_URL}/api/v1/trace/backward/${cartonLotId}`, {
    headers: { 'Accept': 'application/json' },
    tags: { name: 'BackwardTrace' },
  });

  check(backwardRes, {
    'backward trace status is 200': (r) => r.status === 200,
    'backward trace returns nodes': (r) => {
      try {
        const json = r.json();
        return json.tracePath && json.tracePath.length > 0;
      } catch (e) {
        return false;
      }
    },
    'backward trace latency < 2000ms': (r) => r.timings.duration < 2000,
  });

  // 2. Forward Trace: Yarn -> Fabric -> Dye -> Cut -> Assembly -> Carton (6 levels)
  const forwardRes = http.get(`${BASE_URL}/api/v1/trace/forward/${rootLotId}`, {
    headers: { 'Accept': 'application/json' },
    tags: { name: 'ForwardTrace' },
  });

  check(forwardRes, {
    'forward trace status is 200': (r) => r.status === 200,
    'forward trace returns nodes': (r) => {
      try {
        const json = r.json();
        return json.tracePath && json.tracePath.length > 0;
      } catch (e) {
        return false;
      }
    },
    'forward trace latency < 2000ms': (r) => r.timings.duration < 2000,
  });

  sleep(0.5);
}
