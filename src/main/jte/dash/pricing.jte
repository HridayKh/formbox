@import formbox.shared.Entitlements
@import java.util.Map

@param boolean showManageSubscription
@param Long balanceLeft
@param String email
@param Entitlements entitlements
@param Map<String, String> products
@param String msg = null
@param Long storageUsed = null
@param Long storageLimit = null

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Plans & Pricing | Formbox</title>
    <meta name="description" content="Choose a Formbox subscription plan that fits your form submission and storage needs."/>
    <link rel="stylesheet" href="/assets/pico.pumpkin.min.css"/>
    <style>
        .pricing-header-box {
            text-align: center;
            margin-bottom: 2rem;
        }
        .pricing-toggle {
            display: flex;
            justify-content: center;
            align-items: center;
            gap: 1rem;
            margin-top: 1.5rem;
            margin-bottom: 2rem;
        }
        .pricing-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(min(100%, 20rem), 1fr));
            gap: 1.5rem;
            margin-top: 1.5rem;
        }
        .pricing-card {
            border: 1px solid var(--pico-muted-border-color);
            border-radius: 0.75rem;
            padding: 1.5rem;
            display: flex;
            flex-direction: column;
            justify-content: space-between;
            background: var(--pico-card-background-color);
            position: relative;
        }
        .pricing-card.featured {
            border: 2px solid var(--pico-primary);
            box-shadow: 0 0 15px rgba(255, 115, 0, 0.15);
        }
        .badge-popular {
            position: absolute;
            top: -0.75rem;
            right: 1.5rem;
            background: var(--pico-primary);
            color: white;
            font-size: 0.75rem;
            font-weight: bold;
            padding: 0.2rem 0.6rem;
            border-radius: 1rem;
            text-transform: uppercase;
        }
        .price-display {
            font-size: 2.2rem;
            font-weight: 800;
            margin: 0.5rem 0;
            color: var(--pico-color);
        }
        .price-period {
            font-size: 0.9rem;
            color: var(--pico-muted-color);
            font-weight: normal;
        }
        .feature-list {
            list-style: none;
            padding: 0;
            margin: 1.5rem 0;
        }
        .feature-list li {
            padding: 0.4rem 0;
            display: flex;
            align-items: center;
            gap: 0.5rem;
            font-size: 0.95rem;
        }
        .feature-check {
            color: var(--pico-primary);
            font-weight: bold;
        }
        .tier-badge {
            display: inline-block;
            padding: 0.2rem 0.6rem;
            border-radius: 0.4rem;
            font-size: 0.8rem;
            font-weight: bold;
            text-transform: uppercase;
            background: var(--pico-primary-background);
            color: var(--pico-primary);
        }
    </style>
</head>
<body>

<header class="container">
    @template.dash.navbar(pageName="Plans & Pricing", balanceLeft=balanceLeft, showManageSubscription=showManageSubscription, email=email, storageUsed=storageUsed, storageLimit=storageLimit)
    @if(msg != null && !msg.isBlank())
        <div class="container">
            <mark>${msg}</mark>
        </div>
    @endif
</header>

<main class="container">
    <div class="pricing-header-box">
        <a href="/dashboard">&larr; Back to Dashboard</a>
        <h1 style="margin-top: 0.5rem;">Subscription Plans</h1>
        <p>Simple, transparent pricing. Upgrade anytime to get more form submissions, higher file storage, and premium integrations.</p>
        
        <div style="margin-top: 1rem;">
            <span>Your Current Plan: </span>
            <span class="tier-badge">${entitlements != null ? entitlements.tierName() : "free"}</span>
            @if(showManageSubscription)
                <div style="margin-top: 0.75rem;">
                    <a href="/billing/manage" role="button" class="outline">Manage Existing Subscription in Polar Customer Portal &rarr;</a>
                </div>
            @endif
        </div>

        <div class="pricing-toggle">
            <button id="btn-monthly" class="small" onclick="switchInterval('month')">Monthly Billing</button>
            <button id="btn-annual" class="small outline" onclick="switchInterval('year')">Annual Billing <small style="color:var(--pico-primary); font-weight:bold;">(Save 2 Months!)</small></button>
        </div>
    </div>

    <div class="pricing-grid">
        <!-- Free Tier -->
        <div class="pricing-card">
            <div>
                <h3>Free</h3>
                <div class="price-display">$0 <span class="price-period">/ month</span></div>
                <small style="color: var(--pico-muted-color);">Perfect for testing and personal projects</small>
                
                <ul class="feature-list">
                    <li><span class="feature-check">✓</span> 1 Active Form</li>
                    <li><span class="feature-check">✓</span> 100 Submissions / month</li>
                    <li><span class="feature-check">✓</span> 50 MB Storage limit</li>
                    <li><span class="feature-check">✓</span> 14 Days Retention</li>
                    <li><span class="feature-check">✓</span> Honeypot Spam Filter</li>
                    <li><span class="feature-check">✓</span> Discord Webhooks</li>
                </ul>
            </div>

            <div>
                @if(entitlements != null && entitlements.isFree())
                    <button disabled class="outline" style="width: 100%;">Current Plan</button>
                @else
                    <button disabled class="secondary outline" style="width: 100%;">Included</button>
                @endif
            </div>
        </div>

        <!-- Starter Tier -->
        <div class="pricing-card featured">
            <span class="badge-popular">Most Popular</span>
            <div>
                <h3>Starter</h3>
                <div class="price-display" id="starter-price">$10 <span class="price-period">/ month</span></div>
                <small id="starter-subtext" style="color: var(--pico-muted-color);">Billed monthly</small>
                
                <ul class="feature-list">
                    <li><span class="feature-check">✓</span> 10 Active Forms</li>
                    <li><span class="feature-check">✓</span> 1,000 Submissions / month</li>
                    <li><span class="feature-check">✓</span> 100 MB File Storage</li>
                    <li><span class="feature-check">✓</span> 30 Days Retention</li>
                    <li><span class="feature-check">✓</span> Cloudflare Turnstile CAPTCHA</li>
                    <li><span class="feature-check">✓</span> Custom Webhooks &amp; CSV Export</li>
                    <li><span class="feature-check">✓</span> Redirect URLs &amp; Email Notifications</li>
                </ul>
            </div>

            <div>
                @if(entitlements != null && "starter".equalsIgnoreCase(entitlements.tierName()))
                    <button disabled class="outline" style="width: 100%;">Current Plan</button>
                @else
                    <form method="post" action="/billing/checkout">
                        <input type="hidden" name="product_id" id="starter-product-id" value="${products.getOrDefault("starter_month", "820ba1ad-d808-4a0c-908e-35371f426186")}"/>
                        <button type="submit" style="width: 100%;">Subscribe to Starter &rarr;</button>
                    </form>
                @endif
            </div>
        </div>

        <!-- Pro Tier -->
        <div class="pricing-card">
            <div>
                <h3>Pro</h3>
                <div class="price-display" id="pro-price">$30 <span class="price-period">/ month</span></div>
                <small id="pro-subtext" style="color: var(--pico-muted-color);">Billed monthly</small>
                
                <ul class="feature-list">
                    <li><span class="feature-check">✓</span> 25 Active Forms</li>
                    <li><span class="feature-check">✓</span> 10,000 Submissions / month</li>
                    <li><span class="feature-check">✓</span> 1 GB File Storage</li>
                    <li><span class="feature-check">✓</span> 90 Days Retention</li>
                    <li><span class="feature-check">✓</span> Slack, Discord &amp; Telegram</li>
                    <li><span class="feature-check">✓</span> Custom Webhooks &amp; JSON Forms</li>
                    <li><span class="feature-check">✓</span> Altcha CAPTCHA &amp; Email Digests</li>
                </ul>
            </div>

            <div>
                @if(entitlements != null && "pro".equalsIgnoreCase(entitlements.tierName()))
                    <button disabled class="outline" style="width: 100%;">Current Plan</button>
                @else
                    <form method="post" action="/billing/checkout">
                        <input type="hidden" name="product_id" id="pro-product-id" value="${products.getOrDefault("pro_month", "1dd24a52-785f-491a-99d8-6040ac678660")}"/>
                        <button type="submit" class="contrast" style="width: 100%;">Subscribe to Pro &rarr;</button>
                    </form>
                @endif
            </div>
        </div>
    </div>
</main>

<script>
    const starterMonthId = "${products.getOrDefault("starter_month", "820ba1ad-d808-4a0c-908e-35371f426186")}";
    const starterYearId = "${products.getOrDefault("starter_year", "21c1ec9d-21e8-4641-926b-3ed1b4b8d15a")}";
    const proMonthId = "${products.getOrDefault("pro_month", "1dd24a52-785f-491a-99d8-6040ac678660")}";
    const proYearId = "${products.getOrDefault("pro_year", "42b707b5-6d03-4ed6-8880-f3d509a1435b")}";

    function switchInterval(interval) {
        const btnMonthly = document.getElementById('btn-monthly');
        const btnAnnual = document.getElementById('btn-annual');
        const starterPrice = document.getElementById('starter-price');
        const starterSubtext = document.getElementById('starter-subtext');
        const starterInput = document.getElementById('starter-product-id');
        const proPrice = document.getElementById('pro-price');
        const proSubtext = document.getElementById('pro-subtext');
        const proInput = document.getElementById('pro-product-id');

        if (interval === 'year') {
            btnMonthly.classList.add('outline');
            btnAnnual.classList.remove('outline');
            
            starterPrice.innerHTML = '$100 <span class="price-period">/ year</span>';
            starterSubtext.innerText = 'Billed annually ($8.33/mo)';
            if (starterInput) starterInput.value = starterYearId;

            proPrice.innerHTML = '$280 <span class="price-period">/ year</span>';
            proSubtext.innerText = 'Billed annually ($23.33/mo)';
            if (proInput) proInput.value = proYearId;
        } else {
            btnMonthly.classList.remove('outline');
            btnAnnual.classList.add('outline');

            starterPrice.innerHTML = '$10 <span class="price-period">/ month</span>';
            starterSubtext.innerText = 'Billed monthly';
            if (starterInput) starterInput.value = starterMonthId;

            proPrice.innerHTML = '$30 <span class="price-period">/ month</span>';
            proSubtext.innerText = 'Billed monthly';
            if (proInput) proInput.value = proMonthId;
        }
    }
</script>

<footer class="container" style="margin-top: 3rem;">
    <small>&copy; 2026 Formbox. All rights reserved.</small>
</footer>

</body>
</html>
