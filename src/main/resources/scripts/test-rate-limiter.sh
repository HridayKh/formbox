#!/bin/bash

# Configuration - adjust ports or paths to match your local setup
BASE_URL="http://127.0.0.1:8080"
STRICT_URL="$BASE_URL/api/waitlist"
GENEROUS_URL="$BASE_URL/"

echo "=================================================="
echo " Starting Rate Limiter Test on Localhost"
echo "=================================================="

fire_requests() {
    local url=$1
    local count=$2
    echo "-> Sending $count rapid requests to: $url"

    for i in $(seq 1 $count); do
        # -s (silent), -o /dev/null (hide body), -w (print HTTP status code)
        STATUS=$(curl -s -o /dev/null -w "%{http_code}" "$url")
        echo "   Request #$i: HTTP $STATUS"
    done
}

# --- TEST 1: STRICT ROUTE (Capacity: 5, Refill: 0.2/sec) ---
echo -e "\n[TEST 1] Testing Strict Form Submissions..."
echo "Expected behavior: First 5 succeed (200), subsequent hits fail (429)."
fire_requests "$STRICT_URL" 7

# --- TEST 2: GENEROUS ROUTE (Capacity: 60, Refill: 3/sec) ---
echo -e "\n[TEST 2] Testing Generous Dashboard Route..."
echo "Expected behavior: All 10 requests should easily pass (200) due to high capacity."
fire_requests "$GENEROUS_URL" 10

# --- TEST 3: REFILL TIMING VALIDATION ---
echo -e "\n[TEST 3] Testing Refill Logic on Strict Route..."
echo "Waiting 5 seconds to allow exactly 1 token to refill on the strict route..."
sleep 5

echo "Firing 2 more requests to the strict endpoint..."
STATUS1=$(curl -s -o /dev/null -w "%{http_code}" "$STRICT_URL")
echo "   Request #1 (After 5s sleep): HTTP $STATUS1 (Should be 200)"
STATUS2=$(curl -s -o /dev/null -w "%{http_code}" "$STRICT_URL")
echo "   Request #2 (Immediate follow-up): HTTP $STATUS2 (Should be 429)"

echo -e "\n=================================================="
echo " Rate Limiter Test Complete"
echo "=================================================="