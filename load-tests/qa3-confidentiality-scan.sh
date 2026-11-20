#!/usr/bin/env bash
# ==============================================================================
# ThreadTrace - QA3 Confidentiality & Multi-Tenant Isolation Scanner
# Verifies zero confidential data leaks on public endpoints and HTML views,
# and asserts HTTP 403 Forbidden on cross-brand unauthorized access attempts.
# ==============================================================================

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
SAMPLE_QR_TOKEN="${SAMPLE_QR_TOKEN:-QR-BENCH-0160001}"
SAMPLE_LOT_ID="${SAMPLE_LOT_ID:-a0000000-0000-0000-0000-000000027101}"

CONFIDENTIAL_TERMS=(
    "exact_address"
    "unit_price_bdt"
    "worker_count"
    "259 W Santa Clara"
    "Plot 12, Gazipur"
    "4500.00"
    "420.50"
)

TOTAL_VIOLATIONS=0

echo "======================================================================"
echo " Starting QA3 Confidentiality & Multi-Tenant Security Scan"
echo " Target Base URL: ${BASE_URL}"
echo " Sample QR Token: ${SAMPLE_QR_TOKEN}"
echo "======================================================================"

# ------------------------------------------------------------------------------
# Test 1: Scan Public JSON Digital Product Passport Endpoint
# ------------------------------------------------------------------------------
echo -n "[TEST 1] Scanning Public DPP JSON (GET /api/v1/passports/${SAMPLE_QR_TOKEN})... "
PUBLIC_JSON_RESPONSE=$(curl -s -f "${BASE_URL}/api/v1/passports/${SAMPLE_QR_TOKEN}" || echo "ERROR_REQUEST_FAILED")

if [[ "${PUBLIC_JSON_RESPONSE}" == "ERROR_REQUEST_FAILED" ]]; then
    echo "FAILED: Public JSON endpoint unreachable."
    TOTAL_VIOLATIONS=$((TOTAL_VIOLATIONS + 1))
else
    LEAK_COUNT=0
    for term in "${CONFIDENTIAL_TERMS[@]}"; do
        if echo "${PUBLIC_JSON_RESPONSE}" | grep -iq "${term}"; then
            echo -e "\n  [CRITICAL LEAK] Found confidential term '${term}' in public JSON payload!"
            LEAK_COUNT=$((LEAK_COUNT + 1))
        fi
    done

    if [[ ${LEAK_COUNT} -eq 0 ]]; then
        echo "PASSED (0 confidential fields detected)"
    else
        TOTAL_VIOLATIONS=$((TOTAL_VIOLATIONS + LEAK_COUNT))
    fi
fi

# ------------------------------------------------------------------------------
# Test 2: Scan Public Thymeleaf HTML DPP Web Page
# ------------------------------------------------------------------------------
echo -n "[TEST 2] Scanning Public DPP HTML (GET /passports/${SAMPLE_QR_TOKEN})... "
PUBLIC_HTML_RESPONSE=$(curl -s -f "${BASE_URL}/passports/${SAMPLE_QR_TOKEN}" || echo "ERROR_REQUEST_FAILED")

if [[ "${PUBLIC_HTML_RESPONSE}" == "ERROR_REQUEST_FAILED" ]]; then
    echo "FAILED: Public HTML endpoint unreachable."
    TOTAL_VIOLATIONS=$((TOTAL_VIOLATIONS + 1))
else
    LEAK_COUNT=0
    for term in "${CONFIDENTIAL_TERMS[@]}"; do
        if echo "${PUBLIC_HTML_RESPONSE}" | grep -iq "${term}"; then
            echo -e "\n  [CRITICAL LEAK] Found confidential term '${term}' in public HTML view!"
            LEAK_COUNT=$((LEAK_COUNT + 1))
        fi
    done

    if [[ ${LEAK_COUNT} -eq 0 ]]; then
        echo "PASSED (0 confidential fields in HTML markup)"
    else
        TOTAL_VIOLATIONS=$((TOTAL_VIOLATIONS + LEAK_COUNT))
    fi
fi

# ------------------------------------------------------------------------------
# Test 3: Scan Public Lineage Trace Endpoint
# ------------------------------------------------------------------------------
echo -n "[TEST 3] Scanning Public Lineage Trace (GET /api/v1/trace/backward/${SAMPLE_LOT_ID})... "
TRACE_RESPONSE=$(curl -s -f "${BASE_URL}/api/v1/trace/backward/${SAMPLE_LOT_ID}" || echo "ERROR_REQUEST_FAILED")

if [[ "${TRACE_RESPONSE}" == "ERROR_REQUEST_FAILED" ]]; then
    echo "FAILED: Trace endpoint unreachable."
    TOTAL_VIOLATIONS=$((TOTAL_VIOLATIONS + 1))
else
    LEAK_COUNT=0
    for term in "unit_price_bdt" "worker_count" "exact_address"; do
        if echo "${TRACE_RESPONSE}" | grep -iq "${term}"; then
            echo -e "\n  [CRITICAL LEAK] Found confidential term '${term}' in lineage trace response!"
            LEAK_COUNT=$((LEAK_COUNT + 1))
        fi
    done

    if [[ ${LEAK_COUNT} -eq 0 ]]; then
        echo "PASSED (Lineage trace sanitised)"
    else
        TOTAL_VIOLATIONS=$((TOTAL_VIOLATIONS + LEAK_COUNT))
    fi
fi

# ------------------------------------------------------------------------------
# Test 4: Enforce Per-Brand Multi-Tenant Isolation (Assert HTTP 403 on Cross-Brand Access)
# ------------------------------------------------------------------------------
echo -n "[TEST 4] Verifying Tenant Isolation (EcoThreads requesting Patagonia Passport)... "
HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -u "BRAND-ECO-001:brandpass" "${BASE_URL}/api/v1/passports/${SAMPLE_QR_TOKEN}/brand")

if [[ "${HTTP_STATUS}" == "403" ]]; then
    echo "PASSED (HTTP 403 Forbidden correctly returned for cross-tenant access)"
else
    echo "FAILED: Expected HTTP 403 Forbidden but received HTTP ${HTTP_STATUS}!"
    TOTAL_VIOLATIONS=$((TOTAL_VIOLATIONS + 1))
fi

# ------------------------------------------------------------------------------
# Test 5: Enforce Auditor RBAC (Assert HTTP 403 when Brand attempts Auditor View)
# ------------------------------------------------------------------------------
echo -n "[TEST 5] Verifying Auditor RBAC (Brand attempting Auditor Endpoint)... "
AUDITOR_STATUS=$(curl -s -o /dev/null -w "%{http_code}" -u "BRAND-PAT-001:brandpass" "${BASE_URL}/api/v1/passports/${SAMPLE_QR_TOKEN}/auditor")

if [[ "${AUDITOR_STATUS}" == "403" ]]; then
    echo "PASSED (HTTP 403 Forbidden correctly returned for non-auditor caller)"
else
    echo "FAILED: Expected HTTP 403 Forbidden but received HTTP ${AUDITOR_STATUS}!"
    TOTAL_VIOLATIONS=$((TOTAL_VIOLATIONS + 1))
fi

# ------------------------------------------------------------------------------
# Summary & Exit Code
# ------------------------------------------------------------------------------
echo "======================================================================"
if [[ ${TOTAL_VIOLATIONS} -eq 0 ]]; then
    echo " SUCCESS: All QA3 confidentiality and tenant isolation checks passed!"
    echo " Zero confidential fields leaked; multi-tenant RBAC enforced."
    exit 0
else
    echo " FAILURE: Total confidentiality/security violations: ${TOTAL_VIOLATIONS}"
    exit 1
fi
