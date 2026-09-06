#!/bin/bash

# Configuration
BASE_URL="http://127.0.0.1:8080"
STRICT_URL="$BASE_URL/auth/login"
GENEROUS_URL="$BASE_URL/"
HEADER_NAME="CF-Connecting-IP"

echo "=================================================="
echo " Starting Rate Limiter Test on Localhost"
echo "=================================================="

fire_requests() {
    local url=$1
    local count=$2
    local ip=$3
    echo "-> Sending $count rapid requests to: $url (IP: $ip)"

    for i in $(seq 1 $count); do
        # -s (silent), -o /dev/null (hide body), -w (print HTTP status code)
        STATUS=$(curl -s -o /dev/null -w "%{http_code}" -H "$HEADER_NAME: $ip" "$url")
        echo "   Request #$i: HTTP $STATUS"
    done
}

# --- TEST 1: STRICT ROUTE (Capacity: 4, Refill: 0.1/sec) ---
IP_STRICT="10.0.0.1"
echo -e "\n[TEST 1] Testing Strict Route (Capacity: 4)..."
echo "Expected behavior: First 4 succeed (200), requests 5-6 fail (429)."
fire_requests "$STRICT_URL" 6 "$IP_STRICT"

# --- TEST 2: GENEROUS ROUTE (Capacity: 10, Refill: 1/sec) ---
IP_NORMAL="10.0.0.2"
echo -e "\n[TEST 2] Testing Normal Route (Capacity: 10)..."
echo "Expected behavior: First 10 succeed (200), requests 11-12 fail (429)."
fire_requests "$GENEROUS_URL" 12 "$IP_NORMAL"

# --- TEST 3: REFILL TIMING VALIDATION ---
# Refill rate is 0.1 tokens/sec -> exactly 1 token refills every 10 seconds.
echo -e "\n[TEST 3] Testing Refill Logic on Strict Route for IP $IP_STRICT..."
echo "Waiting 10 seconds to allow exactly 1 token to refill..."
sleep 10

echo "Firing 2 follow-up requests..."
STATUS1=$(curl -s -o /dev/null -w "%{http_code}" -H "$HEADER_NAME: $IP_STRICT" "$STRICT_URL")
echo "   Request #1 (After 10s sleep): HTTP $STATUS1 (Should be 200)"
STATUS2=$(curl -s -o /dev/null -w "%{http_code}" -H "$HEADER_NAME: $IP_STRICT" "$STRICT_URL")
echo "   Request #2 (Immediate follow-up): HTTP $STATUS2 (Should be 429)"

# --- TEST 4: IP ISOLATION ---
IP_FRESH="10.0.0.3"
echo -e "\n[TEST 4] Testing IP Isolation..."
echo "Verifying that a rate-limited IP doesn't block a fresh IP..."
STATUS_ISO=$(curl -s -o /dev/null -w "%{http_code}" -H "$HEADER_NAME: $IP_FRESH" "$STRICT_URL")
echo "   Request from fresh IP ($IP_FRESH): HTTP $STATUS_ISO (Should be 200)"

echo -e "\n=================================================="
echo " Rate Limiter Test Complete"
echo "=================================================="