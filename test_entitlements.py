#!/usr/bin/env python3
import os
import sys
import json
import requests

# 1. Get user UUID and email directly to avoid database dependencies (like psycopg2)
tenant_id = input("Enter your Tenant UUID (e.g., from Supabase or console logs): ").strip()
if not tenant_id:
    print("[-] Tenant ID cannot be empty.")
    sys.exit(1)

email = input("Enter the tenant email: ").strip()
if not email:
    print("[-] Email cannot be empty.")
    sys.exit(1)

# 2. Load a.json and modify external_id
payload_path = "/home/hridaykh/Code/hriday_tech/formbox/a.json"
if not os.path.exists(payload_path):
    print(f"[-] Payload template not found at: {payload_path}")
    sys.exit(1)

with open(payload_path) as f:
    payload = json.load(f)

# Override fields in template payload
payload["data"]["external_id"] = str(tenant_id)
payload["data"]["email"] = email

# Assign some sample active subscriptions / benefits for Pro plan:
payload["data"]["active_subscriptions"] = [
    {
        "id": "sub_test_pro",
        "created_at": "2026-07-16T12:00:00Z",
        "modified_at": "2026-07-16T12:00:00Z",
        "metadata": {},
        "amount": 3000,
        "currency": "usd",
        "current_period_start": "2026-07-16T12:00:00Z",
        "current_period_end": "2026-08-16T12:00:00Z",
        "trial_start": None,
        "trial_end": None,
        "cancel_at_period_end": False,
        "canceled_at": None,
        "started_at": "2026-07-16T12:00:00Z",
        "ends_at": None,
        "product_id": "prod_pro_monthly",
        "discount_id": None,
        "meters": [
            {
                "created_at": "2026-07-16T12:00:00Z",
                "modified_at": "2026-07-16T12:00:00Z",
                "id": "meter_sub_pro",
                "consumed_units": 0,
                "credited_units": 50000,
                "amount": 0,
                "meter_id": "meter_id_submissions"
            }
        ],
        "custom_field_data": {}
    }
]

payload["data"]["granted_benefits"] = [
    {
        "id": "gb_pro_identity",
        "created_at": "2026-07-16T12:00:00Z",
        "modified_at": "2026-07-16T12:00:00Z",
        "granted_at": "2026-07-16T12:00:00Z",
        "benefit_id": "ben_pro_identity",
        "benefit_metadata": {
            "tier_name": "pro-v1",
            "tier_priority": "200"
        },
        "properties": {}
    },
    {
        "id": "gb_feature_discord",
        "created_at": "2026-07-16T12:00:00Z",
        "modified_at": "2026-07-16T12:00:00Z",
        "granted_at": "2026-07-16T12:00:00Z",
        "benefit_id": "ben_feature_discord",
        "benefit_metadata": {
            "feature_key": "discord_notifs_allowed"
        },
        "properties": {}
    },
    {
        "id": "gb_limit_rate",
        "created_at": "2026-07-16T12:00:00Z",
        "modified_at": "2026-07-16T12:00:00Z",
        "granted_at": "2026-07-16T12:00:00Z",
        "benefit_id": "ben_limit_rate",
        "benefit_metadata": {
            "limit_key": "max_rate_limit_rpm",
            "limit_value": "90"
        },
        "properties": {}
    }
]

# 3. Send POST request
url = "http://localhost:8080/webhooks/polar/test"
print(f"[*] Posting test payload to {url}...")
try:
    res = requests.post(url, json=payload, headers={"Content-Type": "application/json"})
    print(f"[+] Server Response Code: {res.status_code}")
    print(f"[+] Response JSON: {res.json()}")
except Exception as e:
    print(f"[-] Failed to send POST request: {e}")
