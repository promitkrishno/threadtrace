// ============================================================================
// ThreadTrace - QA4 Public Passport Delivery Performance Load Test (k6)
// Targets: GET /passports/{qrToken} (HTML) and GET /api/v1/passports/{qrToken} (JSON)
// SLA: p95 latency < 2000ms simulating mobile QR-scan traffic with HTTP caching
// ============================================================================

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    mobile_consumer_scans: {
      executor: 'ramping-arrival-rate',
      startRate: 5,
      timeUnit: '1s',
      preAllocatedVUs: 20,
      maxVUs: 50,
      stages: [
        { duration: '10s', target: 20 },  // Ramp up to 20 scans/sec
        { duration: '30s', target: 50 },  // 50 concurrent scans/sec
        { duration: '10s', target: 5 },   // Ramp down
      ],
    },
  },
  thresholds: {
    // Quality Attribute QA4 SLA: Mobile consumer page load p95 < 2000ms
    http_req_duration: ['p(95)<2000', 'p(99)<2500'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Sample QR tokens published on leaf carton lots
const QR_TOKENS = [
  'QR-BENCH-0160001',
  'QR-BENCH-0160002',
  'QR-BENCH-0160003',
  'QR-BENCH-0160004',
  'QR-BENCH-0160005',
];

export default function () {
  const qrToken = QR_TOKENS[Math.floor(Math.random() * QR_TOKENS.length)];

  // 1. Mobile browser navigates to Thymeleaf Web UI: GET /passports/{qrToken}
  const htmlRes = http.get(`${BASE_URL}/passports/${qrToken}`, {
    headers: {
      'User-Agent': 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148',
      'Accept': 'text/html,application/xhtml+xml',
    },
    tags: { name: 'PassportHtmlPage' },
  });

  check(htmlRes, {
    'passport HTML status is 200': (r) => r.status === 200,
    'passport HTML contains style': (r) => r.body && r.body.includes('Digital Product Passport'),
    'passport HTML latency < 2000ms': (r) => r.timings.duration < 2000,
  });

  // 2. Client makes conditional API request with ETag caching: GET /api/v1/passports/{qrToken}
  const apiRes = http.get(`${BASE_URL}/api/v1/passports/${qrToken}`, {
    headers: { 'Accept': 'application/json' },
    tags: { name: 'PassportJsonInitial' },
  });

  const etag = apiRes.headers['ETag'] || apiRes.headers['Etag'];

  check(apiRes, {
    'passport JSON status is 200': (r) => r.status === 200,
    'passport JSON has ETag header': () => etag !== undefined && etag !== null,
    'passport JSON latency < 2000ms': (r) => r.timings.duration < 2000,
  });

  // 3. Conditional repeat request (validating 304 Not Modified cache hit)
  if (etag) {
    const cachedRes = http.get(`${BASE_URL}/api/v1/passports/${qrToken}`, {
      headers: {
        'Accept': 'application/json',
        'If-None-Match': etag,
      },
      tags: { name: 'PassportJsonCached' },
    });

    check(cachedRes, {
      'conditional request returns 304 Not Modified': (r) => r.status === 304,
      'cached response latency < 50ms': (r) => r.timings.duration < 50,
    });
  }

  sleep(0.2);
}
