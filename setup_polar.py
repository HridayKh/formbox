#!/usr/bin/env python3
import os
import sys
from polar_sdk import Polar

# 1. Load POLAR_API_KEY_DEV from .env
env_vars = {}
env_path = "/home/hridaykh/Code/hriday_tech/formbox/.env"
if os.path.exists(env_path):
    with open(env_path) as f:
        for line in f:
            if "=" in line:
                k, v = line.strip().split("=", 1)
                env_vars[k.strip()] = v.strip().strip('"')

api_key = env_vars.get("POLAR_API_KEY_DEV")
if not api_key:
    print("[-] Error: POLAR_API_KEY_DEV not found in .env file.")
    sys.exit(1)

print(f"[+] Loaded Polar API Key: {api_key[:10]}...")

# 2. Initialize Polar Sandbox Client
polar = Polar(server="sandbox", access_token=api_key)

# 3. Retrieve Organization info (no need for org_id since organization token is used)
try:
    orgs_res = polar.organizations.list_organizations()
    if not orgs_res or not orgs_res.result or not orgs_res.result.items:
        print("[-] Error: No organizations found in your Polar account.")
        sys.exit(1)
    
    org = orgs_res.result.items[0]
    print(f"[+] Using Organization: {org.name} (ID: {org.id}, Slug: {org.slug})")
except Exception as e:
    print(f"[-] Error listing organizations: {e}")
    sys.exit(1)

# Helpers for listing & matching existing resources to avoid duplicates
existing_meters = {}
try:
    meters_res = polar.meters.list()
    if meters_res and meters_res.result and meters_res.result.items:
        for m in meters_res.result.items:
            existing_meters[m.name] = m.id
except Exception as e:
    print(f"[!] Warning listing existing meters: {e}")

existing_benefits = {}
try:
    benefits_res = polar.benefits.list()
    if benefits_res and benefits_res.result and benefits_res.result.items:
        for b in benefits_res.result.items:
            existing_benefits[b.description] = b.id
except Exception as e:
    print(f"[!] Warning listing existing benefits: {e}")

existing_products = {}
try:
    products_res = polar.products.list()
    if products_res and products_res.result and products_res.result.items:
        for p in products_res.result.items:
            existing_products[p.name] = p.id
except Exception as e:
    print(f"[!] Warning listing existing products: {e}")


# 4. Create Submissions Meter
meter_name = "Form Submissions"
meter_id = existing_meters.get(meter_name)

if not meter_id:
    print(f"[*] Creating meter: {meter_name}...")
    try:
        # Note: organization_id is omitted because organization access token is used
        meter = polar.meters.create(request={
            "name": meter_name,
            "filter": {
                "conjunction": "and",
                "clauses": [
                    {"property": "name", "operator": "eq", "value": "form_submissions"}
                ]
            },
            "aggregation": {"func": "count"},
            "unit": "scalar"
        })
        meter_id = meter.id
        print(f"[+] Meter created successfully. ID: {meter_id}")
    except Exception as e:
        print(f"[-] Error creating meter: {e}")
        sys.exit(1)
else:
    print(f"[+] Meter already exists: {meter_name} (ID: {meter_id})")


# Update .env submission meter ID if it has changed or is empty
if env_vars.get("POLAR_SUBMISSION_METER_ID_DEV") != meter_id:
    print(f"[*] Updating POLAR_SUBMISSION_METER_ID_DEV in .env to {meter_id}...")
    lines = []
    updated = False
    with open(env_path) as f:
        for line in f:
            if line.startswith("POLAR_SUBMISSION_METER_ID_DEV="):
                lines.append(f'POLAR_SUBMISSION_METER_ID_DEV="{meter_id}"\n')
                updated = True
            else:
                lines.append(line)
    if not updated:
        lines.append(f'\nPOLAR_SUBMISSION_METER_ID_DEV="{meter_id}"\n')
    with open(env_path, "w") as f:
        f.writelines(lines)
    print("[+] .env file updated.")


# Helper to create/retrieve Feature Flag Benefit
def get_or_create_feature_flag(desc, meta):
    benefit_id = existing_benefits.get(desc)
    if not benefit_id:
        print(f"[*] Creating Feature Flag Benefit: {desc}...")
        try:
            benefit = polar.benefits.create(request={
                "type": "feature_flag",
                "description": desc,
                "properties": {},
                "metadata": meta
            })
            benefit_id = benefit.id
            print(f"[+] Benefit created. ID: {benefit_id}")
        except Exception as e:
            print(f"[-] Error creating benefit '{desc}': {e}")
            sys.exit(1)
    else:
        print(f"[+] Benefit already exists: {desc} (ID: {benefit_id})")
    return benefit_id


# Helper to create/retrieve Credits Benefit
def get_or_create_credits_benefit(desc, units):
    benefit_id = existing_benefits.get(desc)
    if not benefit_id:
        print(f"[*] Creating Credits Benefit: {desc} ({units} units)...")
        try:
            benefit = polar.benefits.create(request={
                "type": "meter_credit",
                "description": desc,
                "properties": {
                    "units": units,
                    "rollover": False,
                    "meter_id": meter_id
                }
            })
            benefit_id = benefit.id
            print(f"[+] Credits Benefit created. ID: {benefit_id}")
        except Exception as e:
            print(f"[-] Error creating credits benefit '{desc}': {e}")
            sys.exit(1)
    else:
        print(f"[+] Credits Benefit already exists: {desc} (ID: {benefit_id})")
    return benefit_id


# 5. Define and Create Benefits
benefits = {}

# Tier Identities
benefits["free_identity"] = get_or_create_feature_flag("Free Tier Identity", {"tier_name": "free", "tier_priority": "0"})
benefits["starter_identity"] = get_or_create_feature_flag("Starter Tier Identity", {"tier_name": "starter-v1", "tier_priority": "100"})
benefits["pro_identity"] = get_or_create_feature_flag("Pro Tier Identity", {"tier_name": "pro-v1", "tier_priority": "200"})

# Credits Benefits
benefits["starter_credits"] = get_or_create_credits_benefit("Starter Submissions Credits", 1000)
benefits["pro_credits"] = get_or_create_credits_benefit("Pro Submissions Credits", 50000)

# Boolean Feature Flags
boolean_flags = [
    ("Discord Notifications", "discord_notifs_allowed"),
    ("Turnstile Protection", "turnstile_allowed"),
    ("Custom Redirect URLs", "redirect_urls_allowed"),
    ("JSON Form Submissions", "json_forms_allowed"),
    ("File Uploads", "file_uploads_allowed"),
    ("Field Validations", "field_validations_allowed"),
    ("Slack Notifications", "slack_notifs_allowed"),
    ("Telegram Notifications", "telegram_notifs_allowed"),
    ("Custom Webhooks", "custom_webhooks_allowed"),
    ("CSV Exports", "csv_exports_allowed"),
    ("Email Digests", "email_digests_allowed"),
    ("ALTCHA Protection", "altcha_allowed"),
]
for name, key in boolean_flags:
    benefits[key] = get_or_create_feature_flag(f"Feature: {name}", {"feature_key": key})

# Numeric limits
benefits["starter_rate_limit"] = get_or_create_feature_flag("Limit: Starter Rate Limit (30 RPM)", {"limit_key": "max_rate_limit_rpm", "limit_value": "30"})
benefits["pro_rate_limit"] = get_or_create_feature_flag("Limit: Pro Rate Limit (90 RPM)", {"limit_key": "max_rate_limit_rpm", "limit_value": "90"})

benefits["starter_file_size"] = get_or_create_feature_flag("Limit: Starter Max File Size (5MB)", {"limit_key": "max_file_size_bytes", "limit_value": "5242880"})
benefits["pro_file_size"] = get_or_create_feature_flag("Limit: Pro Max File Size (25MB)", {"limit_key": "max_file_size_bytes", "limit_value": "26214400"})

benefits["starter_forms"] = get_or_create_feature_flag("Limit: Starter Forms (10)", {"limit_key": "forms_limit", "limit_value": "10"})
benefits["pro_forms"] = get_or_create_feature_flag("Limit: Pro Forms (Unlimited)", {"limit_key": "forms_limit", "limit_value": "2000000000"})

benefits["starter_storage"] = get_or_create_feature_flag("Limit: Starter Storage (1GB)", {"limit_key": "storage_limit_bytes", "limit_value": "1073741824"})
benefits["pro_storage"] = get_or_create_feature_flag("Limit: Pro Storage (10GB)", {"limit_key": "storage_limit_bytes", "limit_value": "10737418240"})


# 6. Create Products and Link Benefits
def setup_product(name, recurring_interval, price_dict, attached_benefit_keys):
    product_id = existing_products.get(name)
    benefit_ids = [benefits[k] for k in attached_benefit_keys if k in benefits]

    if not product_id:
        print(f"[*] Creating Product: {name}...")
        try:
            if recurring_interval == "one_time":
                product = polar.products.create(request={
                    "name": name,
                    "prices": [price_dict]
                })
            else:
                product = polar.products.create(request={
                    "name": name,
                    "prices": [price_dict],
                    "recurring_interval": recurring_interval
                })
            product_id = product.id
            print(f"[+] Product created successfully. ID: {product_id}")
        except Exception as e:
            print(f"[-] Error creating product '{name}': {e}")
            sys.exit(1)
    else:
        print(f"[+] Product already exists: {name} (ID: {product_id})")

    # Link benefits to product
    print(f"[*] Linking {len(benefit_ids)} benefits to Product: {name}...")
    try:
        polar.products.update_benefits(
            id=product_id,
            product_benefits_update={"benefits": benefit_ids}
        )
        print("[+] Benefits linked successfully.")
    except Exception as e:
        print(f"[-] Error linking benefits to product '{name}': {e}")

# Common feature flag sets
starter_feature_keys = [
    "discord_notifs_allowed", "turnstile_allowed", "redirect_urls_allowed",
    "json_forms_allowed", "file_uploads_allowed", "field_validations_allowed",
    "slack_notifs_allowed", "telegram_notifs_allowed"
]
pro_feature_keys = starter_feature_keys + [
    "custom_webhooks_allowed", "csv_exports_allowed", "email_digests_allowed", "altcha_allowed"
]

# A. Free Product
setup_product(
    name="Free Plan",
    recurring_interval="month",
    price_dict={"amount_type": "free"},
    attached_benefit_keys=["free_identity"]
)

# B. Starter Products
setup_product(
    name="Starter Monthly",
    recurring_interval="month",
    price_dict={"amount_type": "fixed", "price_amount": 1000}, # $10.00/mo
    attached_benefit_keys=["starter_identity", "starter_credits", "starter_rate_limit", "starter_file_size", "starter_forms", "starter_storage"] + starter_feature_keys
)
setup_product(
    name="Starter Annual",
    recurring_interval="year",
    price_dict={"amount_type": "fixed", "price_amount": 10000}, # $100.00/yr
    attached_benefit_keys=["starter_identity", "starter_credits", "starter_rate_limit", "starter_file_size", "starter_forms", "starter_storage"] + starter_feature_keys
)

# C. Pro Products
setup_product(
    name="Pro Monthly",
    recurring_interval="month",
    price_dict={"amount_type": "fixed", "price_amount": 3000}, # $30.00/mo
    attached_benefit_keys=["pro_identity", "pro_credits", "pro_rate_limit", "pro_file_size", "pro_forms", "pro_storage"] + pro_feature_keys
)
setup_product(
    name="Pro Annual",
    recurring_interval="year",
    price_dict={"amount_type": "fixed", "price_amount": 30000}, # $300.00/yr
    attached_benefit_keys=["pro_identity", "pro_credits", "pro_rate_limit", "pro_file_size", "pro_forms", "pro_storage"] + pro_feature_keys
)
setup_product(
    name="Pro Lifetime Deal (LTD)",
    recurring_interval="one_time",
    price_dict={"amount_type": "fixed", "price_amount": 19900}, # $199.00 one-time
    attached_benefit_keys=["pro_identity", "pro_credits", "pro_rate_limit", "pro_file_size", "pro_forms", "pro_storage"] + pro_feature_keys
)

print("\n[+] Polar Sandbox setup script completed successfully!")
