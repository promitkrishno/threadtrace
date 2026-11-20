#!/usr/bin/env bash
# ==============================================================================
# ThreadTrace - QA4 High Availability & DB-Down Resilience Verification
# Validates that public DPP endpoints survive a database outage via the
# in-memory cache fallback (serving HTTP 200 in < 2 seconds during DB failure).
# ==============================================================================

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SAMPLE_QR_TOKEN="${SAMPLE_QR_TOKEN:-QR-BENCH-0160001}"
DOCKER_COMPOSE_FILE="${DOCKER_COMPOSE_FILE:-docker-compose.yml}"

echo "======================================================================"
echo " Starting QA4 High Availability / Database Outage Resilience Test"
echo " Target Base URL: ${BASE_URL}"
echo " Testing QR Token: ${SAMPLE_QR_TOKEN}"
echo "======================================================================"

# ------------------------------------------------------------------------------
# Step 1: Pre-warm the In-Memory Passport Cache
# ------------------------------------------------------------------------------
echo -n "[STEP 1] Pre-warming cache while database is healthy... "
WARMUP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" "${BASE_URL}/api/v1/passports/${SAMPLE_QR_TOKEN}")
if [[ "${WARMUP_STATUS}" == "200" ]]; then
    echo "SUCCESS (HTTP 200 received, cache warmed)"
else
    echo "WARNING: Warmup request returned HTTP ${WARMUP_STATUS}. Proceeding..."
fi

# Also warm HTML route
curl -s -o /dev/null "${BASE_URL}/passports/${SAMPLE_QR_TOKEN}" || true

# ------------------------------------------------------------------------------
# Step 2: Simulate Database Outage (docker compose pause postgres)
# ------------------------------------------------------------------------------
echo "[STEP 2] Simulating database outage: pausing PostgreSQL container..."
docker compose -f "${DOCKER_COMPOSE_FILE}" pause postgres

# Ensure cleanup on unexpected exit
cleanup() {
    echo "[CLEANUP] Resuming PostgreSQL container..."
    docker compose -f "${DOCKER_COMPOSE_FILE}" unpause postgres || true
}
trap cleanup EXIT

echo "Database container paused. Validating outage..."
sleep 2

# ------------------------------------------------------------------------------
# Step 3: Query Public Passport during Database Outage
# ------------------------------------------------------------------------------
echo -n "[STEP 3] Querying Public Passport API during DB outage... "
START_TIME=$(date +%s%N)
OUTAGE_API_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "${BASE_URL}/api/v1/passports/${SAMPLE_QR_TOKEN}" || echo "TIMEOUT")
END_TIME=$(date +%s%N)
ELAPSED_MS=$(( (END_TIME - START_TIME) / 1000000 ))

echo "HTTP Status: ${OUTAGE_API_STATUS} (${ELAPSED_MS} ms)"

if [[ "${OUTAGE_API_STATUS}" != "200" ]]; then
    echo "FAILED: Expected HTTP 200 from cache fallback, got ${OUTAGE_API_STATUS}"
    exit 1
fi

if [[ ${ELAPSED_MS} -gt 2000 ]]; then
    echo "FAILED: Fallback latency ${ELAPSED_MS}ms exceeded 2000ms SLA"
    exit 1
fi

echo -n "[STEP 4] Querying Public Passport HTML Web Page during DB outage... "
HTML_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 3 "${BASE_URL}/passports/${SAMPLE_QR_TOKEN}" || echo "TIMEOUT")

if [[ "${HTML_STATUS}" == "200" ]]; then
    echo "SUCCESS: HTTP 200 OK served seamlessly from cache fallback!"
else
    echo "FAILED: HTML endpoint returned HTTP ${HTML_STATUS}"
    exit 1
fi

# ------------------------------------------------------------------------------
# Step 5: Resume Database Container
# ------------------------------------------------------------------------------
echo "[STEP 5] Restoring database availability: unpausing PostgreSQL..."
docker compose -f "${DOCKER_COMPOSE_FILE}" unpause postgres
trap - EXIT

sleep 2

echo "======================================================================"
echo " SUCCESS: QA4 Database Outage Resilience Verified!"
echo " In-memory cache fallback served requests with 0 downtime in <2s."
echo "======================================================================"
exit 0
