@param boolean loggedIn
<!DOCTYPE html>
<html lang="en">

<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Formbox — Form Backend for Static Sites | Spam Protection, Delivery Monitoring, Webhooks</title>
    <meta name="description"
          content="Formbox is an instant form backend for websites with no server. Point your HTML form at us and get spam protection, email delivery monitoring, webhooks, and a full submission dashboard."/>
    <link rel="stylesheet" href="/assets/styles.css"/>
</head>

<body>

<!-- ========================================================================
   NAVBAR
   ======================================================================== -->
<nav class="navbar" id="navbar">
    <div class="container-wide navbar-inner">

        <!-- Logo -->
        <a href="/" class="navbar-logo">
            <span class="logo-icon">F</span>
            Formbox
        </a>

        <!-- Desktop Nav Links -->
        <ul class="navbar-links">

            <!-- Product -->
            <li class="nav-item">
                <a href="" class="nav-link">Product <span class="caret">▾</span></a>
                <div class="nav-dropdown dropdown-lg">
                    <div class="dropdown-group">
                        <div class="dropdown-group-title">Features</div>
                        <a href="#" class="dropdown-link"><span class="link-icon">🛡</span> Spam Protection</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">📬</span> Delivery Monitoring</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">📎</span> File Uploads</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">🔔</span> Webhooks &amp; Notifications</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">👁</span> Client Views</a>
                    </div>
                    <div class="dropdown-group">
                        <div class="dropdown-group-title">Integrations</div>
                        <a href="#" class="dropdown-link"><span class="link-icon">💬</span> Discord</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">📢</span> Slack</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">✈️</span> Telegram</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">🔗</span> Custom Webhooks</a>
                    </div>
                    <div class="dropdown-group">
                        <div class="dropdown-group-title">Security</div>
                        <a href="#" class="dropdown-link"><span class="link-icon">🍯</span> Honeypot</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">☁️</span> Turnstile</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">🔐</span> Altcha</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">⏱</span> Rate Limiting</a>
                    </div>
                </div>
            </li>

            <!-- Solutions -->
            <li class="nav-item">
                <a href="" class="nav-link">Solutions <span class="caret">▾</span></a>
                <div class="nav-dropdown">
                    <div class="dropdown-group">
                        <div class="dropdown-group-title">Use Cases</div>
                        <a href="#" class="dropdown-link"><span class="link-icon">🏢</span> For Agencies</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">💼</span> For Freelancers</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">🚀</span> For SaaS</a>
                    </div>
                    <div class="dropdown-group">
                        <div class="dropdown-group-title">By Framework</div>
                        <a href="#" class="dropdown-link"><span class="link-icon">⚛️</span> Next.js</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">🚀</span> Astro</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">📝</span> Hugo</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">🎨</span> Webflow</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">🏔</span> Eleventy</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">📄</span> Static HTML</a>
                    </div>
                </div>
            </li>

            <!-- Compare -->
            <li class="nav-item">
                <a href="" class="nav-link">Compare <span class="caret">▾</span></a>
                <div class="nav-dropdown" style="min-width: 280px; grid-template-columns: 1fr;">
                    <div class="dropdown-group">
                        <div class="dropdown-group-title">Alternatives</div>
                        <a href="#" class="dropdown-link"><span class="link-icon">⚔️</span> vs Formspree</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">⚔️</span> vs Basin</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">⚔️</span> vs Getform</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">⚔️</span> vs FormSubmit</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">⚔️</span> vs Netlify Forms</a>
                    </div>
                </div>
            </li>

            <!-- Developers -->
            <li class="nav-item">
                <a href="" class="nav-link">Developers <span class="caret">▾</span></a>
                <div class="nav-dropdown dropdown-lg">
                    <div class="dropdown-group">
                        <div class="dropdown-group-title">Get Started</div>
                        <a href="#" class="dropdown-link"><span class="link-icon">⚡</span> Quickstart</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">📖</span> API Reference</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">📋</span> Changelog</a>
                    </div>
                    <div class="dropdown-group">
                        <div class="dropdown-group-title">Guides</div>
                        <a href="#" class="dropdown-link"><span class="link-icon">📄</span> HTML Forms</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">⚛️</span> Next.js</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">🚀</span> Astro</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">📝</span> Hugo</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">🎨</span> Webflow</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">🏔</span> Eleventy</a>
                    </div>
                    <div class="dropdown-group">
                        <div class="dropdown-group-title">Templates</div>
                        <a href="#" class="dropdown-link"><span class="link-icon">📩</span> Contact Form</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">💬</span> Feedback Form</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">📧</span> Newsletter Signup</a>
                        <a href="#" class="dropdown-link"><span class="link-icon">📎</span> File Upload Form</a>
                    </div>
                </div>
            </li>

            <!-- Pricing (no dropdown) -->
            <li class="nav-item">
                <a href="#pricing" class="nav-link">Pricing</a>
            </li>
        </ul>

        <!-- Actions -->
        <div class="navbar-actions">
            <a href="/auth/login" class="navbar-signin">Sign In</a>
            <a href="/auth/signup" class="navbar-signin">Sign Up</a>
            <a href="#waitlist" class="btn btn-primary btn-sm">Join Waitlist →</a>
        </div>

        <!-- Mobile toggle -->
        <button class="mobile-toggle" id="mobileToggle" aria-label="Open menu">
            <span></span><span></span><span></span>
        </button>
    </div>
</nav>

<!-- Mobile Nav Overlay -->
<div class="mobile-nav" id="mobileNav">
    <div class="mobile-nav-group">
        <div class="mobile-nav-group-title">Product</div>
        <a href="#" class="mobile-nav-link">Spam Protection</a>
        <a href="#" class="mobile-nav-link">Delivery Monitoring</a>
        <a href="#" class="mobile-nav-link">File Uploads</a>
        <a href="#" class="mobile-nav-link">Webhooks &amp; Notifications</a>
        <a href="#" class="mobile-nav-link">Client Views</a>
    </div>
    <div class="mobile-nav-group">
        <div class="mobile-nav-group-title">Integrations</div>
        <a href="#" class="mobile-nav-link">Discord</a>
        <a href="#" class="mobile-nav-link">Slack</a>
        <a href="#" class="mobile-nav-link">Telegram</a>
        <a href="#" class="mobile-nav-link">Custom Webhooks</a>
    </div>
    <div class="mobile-nav-group">
        <div class="mobile-nav-group-title">Solutions</div>
        <a href="#" class="mobile-nav-link">For Agencies</a>
        <a href="#" class="mobile-nav-link">For Freelancers</a>
        <a href="#" class="mobile-nav-link">For SaaS</a>
    </div>
    <div class="mobile-nav-group">
        <div class="mobile-nav-group-title">Compare</div>
        <a href="#" class="mobile-nav-link">vs Formspree</a>
        <a href="#" class="mobile-nav-link">vs Basin</a>
        <a href="#" class="mobile-nav-link">vs Getform</a>
        <a href="#" class="mobile-nav-link">vs FormSubmit</a>
        <a href="#" class="mobile-nav-link">vs Netlify Forms</a>
    </div>
    <div class="mobile-nav-group">
        <div class="mobile-nav-group-title">Developers</div>
        <a href="#" class="mobile-nav-link">Quickstart</a>
        <a href="#" class="mobile-nav-link">API Reference</a>
        <a href="#" class="mobile-nav-link">Guides</a>
        <a href="#" class="mobile-nav-link">Templates</a>
        <a href="#" class="mobile-nav-link">Changelog</a>
    </div>
    <div class="mobile-nav-group">
        <a href="#pricing" class="mobile-nav-link" style="font-weight:600;color:var(--text);">Pricing</a>
        <a href="/auth/login" class="mobile-nav-link" style="font-weight:600;color:var(--text);">Sign In</a>
        <a href="/auth/signup" class="mobile-nav-link" style="font-weight:600;color:var(--text);">Sign Up</a>
    </div>
    <div style="padding-top:16px;">
        <a href="#waitlist" class="btn btn-primary" style="width:100%;justify-content:center;">Join Waitlist →</a>
    </div>
</div>


<!-- ========================================================================
   HERO
   ======================================================================== -->
<section class="hero" id="hero">
    <div class="container">
        <div class="hero-grid">

            <!-- Left — Copy -->
            <div class="hero-content fade-up">
                <div class="hero-badge">
                    <span class="badge badge-accent">⚡ Waitlist</span>
                </div>
                <h1>Your forms,<br><span class="highlight">monitored.</span></h1>
                <p class="hero-subtitle">
                    Point any HTML form at Formbox and get instant submission storage, multi-layer spam protection,
                    email
                    delivery tracking, and real-time webhook notifications. No backend code. No dependencies.
                </p>
                <div class="hero-actions">
                    <a href="#waitlist" class="btn btn-primary">Join the Waitlist →</a>
                    <a href="#how-it-works" class="btn btn-ghost">See How It Works</a>
                </div>
            </div>

            <!-- Right — Mock Dashboard -->
            <div class="hero-visual fade-up" style="transition-delay:150ms;">
                <div class="mock-dashboard">
                    <div class="mock-titlebar">
                        <span class="mock-dot"></span>
                        <span class="mock-dot"></span>
                        <span class="mock-dot"></span>
                        <div class="mock-breadcrumb">
                            <span>Formbox</span>
                            <span class="sep">›</span>
                            <span>Contact Form</span>
                            <span class="sep">›</span>
                            <span style="color:var(--text);">Submission #1,847</span>
                        </div>
                    </div>

                    <div class="mock-body">
                        <div class="mock-event-header">
                            <span class="mock-event-title">New submission received</span>
                            <span class="mock-event-time">2s ago</span>
                        </div>

                        <div class="mock-badges">
                            <span class="badge badge-success">✓ Delivered</span>
                            <span class="badge badge-warning">🛡 Spam Clean</span>
                            <span class="badge badge-info">⚡ 38ms</span>
                        </div>

                        <div class="mock-fields">
                            <div class="mock-field">
                                <span class="mock-field-key">name</span>
                                <span class="mock-field-value">"Sarah Chen"</span>
                            </div>
                            <div class="mock-field">
                                <span class="mock-field-key">email</span>
                                <span class="mock-field-value">"sarah@example.com"</span>
                            </div>
                            <div class="mock-field">
                                <span class="mock-field-key">message</span>
                                <span class="mock-field-value">"Love the product! Quick question about…"</span>
                            </div>
                        </div>

                        <div class="mock-timeline">
                            <span class="mock-timeline-step done">✓ Received</span>
                            <span class="mock-timeline-arrow">→</span>
                            <span class="mock-timeline-step done">✓ Email sent</span>
                            <span class="mock-timeline-arrow">→</span>
                            <span class="mock-timeline-step done">✓ Delivered</span>
                            <span class="mock-timeline-arrow">→</span>
                            <span class="mock-timeline-step done">✓ Webhook fired</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>
</section>


<!-- ========================================================================
   SOCIAL PROOF / COMPATIBILITY
   ======================================================================== -->
<section class="compat section-sm">
    <div class="container">
        <div class="compat-inner">
            <span class="compat-label">Works with any stack</span>
            <div class="compat-items">
                <span class="compat-item"><span class="compat-icon">📄</span> HTML5</span>
                <span class="compat-item"><span class="compat-icon">⚛️</span> React</span>
                <span class="compat-item"><span class="compat-icon">▲</span> Next.js</span>
                <span class="compat-item"><span class="compat-icon">🚀</span> Astro</span>
                <span class="compat-item"><span class="compat-icon">📝</span> Hugo</span>
                <span class="compat-item"><span class="compat-icon">🏔</span> Eleventy</span>
                <span class="compat-item"><span class="compat-icon">🎨</span> Webflow</span>
                <span class="compat-item"><span class="compat-icon">🌐</span> Gatsby</span>
            </div>
        </div>
    </div>
</section>


<!-- ========================================================================
   WHY FORMBOX — 3 DIFFERENTIATORS
   ======================================================================== -->
<section class="section" id="why">
    <div class="container">
        <div class="section-header fade-up">
            <h2>Why teams choose Formbox</h2>
            <p>The form backend that does more than just catch POST requests.</p>
        </div>

        <div class="diff-grid stagger-children">
            <div class="diff-card fade-up">
                <div class="diff-icon">📬</div>
                <h3>Delivery Monitoring</h3>
                <p>Know if your form emails actually arrived. Track sent, delivered, and bounced states per
                    notification.
                    Answer "is my form broken?" before anyone asks.</p>
            </div>
            <div class="diff-card fade-up">
                <div class="diff-icon">🏢</div>
                <h3>Built for Agencies</h3>
                <p>Folder hierarchy, client credentials per form or folder, read-only client views. Manage 50 clients
                    from one
                    account without sharing your login.</p>
            </div>
            <div class="diff-card fade-up">
                <div class="diff-icon">⚡</div>
                <h3>Zero Dependencies</h3>
                <p>No npm install. No SDK. No build step. Point your HTML form's action attribute to your endpoint and
                    start
                    receiving submissions in seconds.</p>
            </div>
        </div>
    </div>
</section>


<!-- ========================================================================
   FEATURES SHOWCASE
   ======================================================================== -->
<section class="section" id="features" style="background:var(--bg-surface);">
    <div class="container">
        <div class="section-header fade-up">
            <h2>Everything you need to manage form submissions</h2>
            <p>From spam filtering to delivery tracking — one dashboard for all your forms.</p>
        </div>

        <div class="features-grid stagger-children">

            <!-- Feature 1: Submission Handling -->
            <div class="feature-card fade-up">
                <div class="feature-card-header">
                    <span class="feature-icon">📥</span>
                    <div>
                        <h3>Submission Handling</h3>
                        <p>Every submission stored, searchable, and exportable. Unique POST endpoint per form with
                            retention
                            windows per plan.</p>
                    </div>
                </div>
                <div class="feature-preview">
                    <div class="preview-row">
                        <span class="preview-label" style="color:var(--text-muted);">#1847</span>
                        <span style="color:var(--text);">Sarah Chen</span>
                        <span style="margin-left:auto;color:var(--text-muted);">2s ago</span>
                    </div>
                    <div class="preview-row">
                        <span class="preview-label" style="color:var(--text-muted);">#1846</span>
                        <span style="color:var(--text);">James Wilson</span>
                        <span style="margin-left:auto;color:var(--text-muted);">5m ago</span>
                    </div>
                    <div class="preview-row">
                        <span class="preview-label" style="color:var(--text-muted);">#1845</span>
                        <span style="color:var(--text);">Priya Sharma</span>
                        <span style="margin-left:auto;color:var(--text-muted);">12m ago</span>
                    </div>
                </div>
            </div>

            <!-- Feature 2: Spam Protection -->
            <div class="feature-card fade-up">
                <div class="feature-card-header">
                    <span class="feature-icon">🛡</span>
                    <div>
                        <h3>Multi-Layer Spam Protection</h3>
                        <p>Honeypot fields, submit-timing checks, domain locking, rate limiting, Altcha, and Cloudflare
                            Turnstile.
                            Layer them as you need.</p>
                    </div>
                </div>
                <div class="feature-preview">
                    <div class="preview-row">
                        <span class="badge badge-success" style="font-size:0.6875rem;">Honeypot ✓</span>
                        <span class="badge badge-success" style="font-size:0.6875rem;">Turnstile ✓</span>
                        <span class="badge badge-success" style="font-size:0.6875rem;">Rate Limit ✓</span>
                    </div>
                    <div class="preview-row" style="margin-top:4px;">
                        <span class="preview-label" style="color:var(--text-muted);">blocked</span>
                        <span style="color:var(--error);">12 spam this week</span>
                        <span style="margin-left:auto;color:var(--success);">0 false positives</span>
                    </div>
                </div>
            </div>

            <!-- Feature 3: Email Delivery Status -->
            <div class="feature-card fade-up">
                <div class="feature-card-header">
                    <span class="feature-icon">📬</span>
                    <div>
                        <h3>Email Delivery Status</h3>
                        <p>Track every notification email: sent, delivered, or bounced. Know instantly if your form
                            notifications
                            are silently failing.</p>
                    </div>
                </div>
                <div class="feature-preview">
                    <div class="preview-row">
                        <span style="color:var(--success);">●</span>
                        <span style="color:var(--text);">Sent</span>
                        <span style="color:var(--text-muted);">→</span>
                        <span style="color:var(--success);">●</span>
                        <span style="color:var(--text);">Delivered</span>
                        <span style="margin-left:auto;color:var(--text-muted);">owner@agency.com</span>
                    </div>
                    <div class="preview-row">
                        <span style="color:var(--success);">●</span>
                        <span style="color:var(--text);">Sent</span>
                        <span style="color:var(--text-muted);">→</span>
                        <span style="color:var(--error);">●</span>
                        <span style="color:var(--error);">Bounced</span>
                        <span style="margin-left:auto;color:var(--text-muted);">old@client.net</span>
                    </div>
                </div>
            </div>

            <!-- Feature 4: Client/Agency Structure -->
            <div class="feature-card fade-up">
                <div class="feature-card-header">
                    <span class="feature-icon">📂</span>
                    <div>
                        <h3>Client &amp; Agency Structure</h3>
                        <p>Organize forms into folders per client. Issue scoped credentials. Give clients read-only
                            access to
                            their own submissions.</p>
                    </div>
                </div>
                <div class="feature-preview">
                    <div class="preview-row">
                        <span style="color:var(--accent);">📁</span>
                        <span style="color:var(--text);">Acme Corp</span>
                        <span style="margin-left:auto;color:var(--text-muted);">3 forms</span>
                    </div>
                    <div class="preview-row" style="padding-left:20px;">
                        <span style="color:var(--text-muted);">└</span>
                        <span style="color:var(--text-secondary);">Contact Form</span>
                        <span style="margin-left:auto;">
                <span class="badge badge-accent" style="font-size:0.625rem;">client-view</span>
              </span>
                    </div>
                    <div class="preview-row">
                        <span style="color:var(--accent);">📁</span>
                        <span style="color:var(--text);">Globex Inc</span>
                        <span style="margin-left:auto;color:var(--text-muted);">5 forms</span>
                    </div>
                </div>
            </div>

            <!-- Feature 5: File Uploads -->
            <div class="feature-card fade-up">
                <div class="feature-card-header">
                    <span class="feature-icon">📎</span>
                    <div>
                        <h3>File Uploads</h3>
                        <p>Accept file attachments on any form. Files stored securely with MIME-type validation and
                            per-plan
                            storage limits.</p>
                    </div>
                </div>
                <div class="feature-preview">
                    <div class="preview-row">
                        <span style="color:var(--info);">📄</span>
                        <span style="color:var(--text);">resume.pdf</span>
                        <span style="margin-left:auto;color:var(--text-muted);">2.4 MB</span>
                    </div>
                    <div class="preview-row">
                        <span style="color:var(--info);">🖼</span>
                        <span style="color:var(--text);">screenshot.png</span>
                        <span style="margin-left:auto;color:var(--text-muted);">890 KB</span>
                    </div>
                </div>
            </div>

            <!-- Feature 6: Webhooks & Integrations -->
            <div class="feature-card fade-up">
                <div class="feature-card-header">
                    <span class="feature-icon">🔗</span>
                    <div>
                        <h3>Webhooks &amp; Integrations</h3>
                        <p>Get notified wherever you work. Discord, Slack, Telegram, email, or any custom webhook
                            endpoint.</p>
                    </div>
                </div>
                <div class="feature-preview">
                    <div class="preview-row">
                        <span style="color:var(--success);">●</span>
                        <span style="color:var(--text);">Discord</span>
                        <span style="margin-left:auto;color:var(--success);">200 OK</span>
                        <span style="color:var(--text-muted);margin-left:8px;">12ms</span>
                    </div>
                    <div class="preview-row">
                        <span style="color:var(--success);">●</span>
                        <span style="color:var(--text);">Slack</span>
                        <span style="margin-left:auto;color:var(--success);">200 OK</span>
                        <span style="color:var(--text-muted);margin-left:8px;">45ms</span>
                    </div>
                    <div class="preview-row">
                        <span style="color:var(--success);">●</span>
                        <span style="color:var(--text);">Custom (HubSpot)</span>
                        <span style="margin-left:auto;color:var(--success);">201 Created</span>
                        <span style="color:var(--text-muted);margin-left:8px;">89ms</span>
                    </div>
                </div>
            </div>

        </div>
    </div>
</section>


<!-- ========================================================================
   HOW IT WORKS
   ======================================================================== -->
<section class="section" id="how-it-works">
    <div class="container">
        <div class="section-header fade-up">
            <h2>Three steps to never miss a submission</h2>
            <p>No backend. No SDK. No build step. Just your HTML form and a Formbox endpoint.</p>
        </div>

        <div class="steps-grid stagger-children">

            <!-- Step 1 -->
            <div class="step-card fade-up">
                <div class="step-number">1</div>
                <h3>Point your form</h3>
                <p>Set the <code>action</code> attribute on any HTML form to your Formbox endpoint URL.</p>
                <div class="step-preview">
                    <span class="code-tag">&lt;form</span> <span class="code-accent">action</span>=<span
                            class="code-string">"https://formbox.dev/f/abc123"</span><br>
                    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<span class="code-accent">method</span>=<span
                            class="code-string">"POST"</span><span class="code-tag">&gt;</span><br>
                    &nbsp;&nbsp;<span class="code-tag">&lt;input</span> <span class="code-accent">name</span>=<span
                            class="code-string">"email"</span> <span class="code-tag">/&gt;</span><br>
                    &nbsp;&nbsp;<span class="code-tag">&lt;button</span> <span class="code-accent">type</span>=<span
                            class="code-string">"submit"</span><span class="code-tag">&gt;</span>Send<span
                            class="code-tag">&lt;/button&gt;</span><br>
                    <span class="code-tag">&lt;/form&gt;</span>
                </div>
            </div>

            <!-- Step 2 -->
            <div class="step-card fade-up">
                <div class="step-number">2</div>
                <h3>We process at the edge</h3>
                <p>Submissions pass through multi-layer spam protection and field validation automatically.</p>
                <div class="step-preview">
                    <div class="preview-row"><span style="color:var(--success);">✓</span>
                        <span>Honeypot check</span><span
                                style="margin-left:auto;color:var(--text-muted);">clean</span></div>
                    <div class="preview-row"><span style="color:var(--success);">✓</span>
                        <span>Turnstile verified</span><span
                                style="margin-left:auto;color:var(--text-muted);">passed</span></div>
                    <div class="preview-row"><span style="color:var(--success);">✓</span> <span>Rate limit</span><span
                                style="margin-left:auto;color:var(--text-muted);">3/15 rpm</span></div>
                    <div class="preview-row"><span style="color:var(--success);">✓</span>
                        <span>Origin allowed</span><span
                                style="margin-left:auto;color:var(--text-muted);">mysite.com</span></div>
                </div>
            </div>

            <!-- Step 3 -->
            <div class="step-card fade-up">
                <div class="step-number">3</div>
                <h3>You get notified</h3>
                <p>Receive notifications via email, Discord, Slack, Telegram, or any custom webhook instantly.</p>
                <div class="step-preview">
                    <div class="preview-row"><span>📧</span> <span>Email notification</span><span
                                style="margin-left:auto;color:var(--success);">delivered</span></div>
                    <div class="preview-row"><span>💬</span> <span>Discord webhook</span><span
                                style="margin-left:auto;color:var(--success);">200 OK</span></div>
                    <div class="preview-row"><span>📢</span> <span>Slack webhook</span><span
                                style="margin-left:auto;color:var(--success);">200 OK</span></div>
                    <div class="preview-row"><span>🔗</span> <span>Custom → HubSpot</span><span
                                style="margin-left:auto;color:var(--success);">201</span></div>
                </div>
            </div>

        </div>
    </div>
</section>


<!-- ========================================================================
   INTEGRATIONS
   ======================================================================== -->
<section class="section-sm"
         style="border-top:1px solid var(--border);border-bottom:1px solid var(--border);background:var(--bg-surface);">
    <div class="container">
        <div class="section-header fade-up" style="margin-bottom:40px;">
            <h2>Integrations</h2>
            <p>Get submissions where you already work.</p>
        </div>
        <div class="integrations-grid stagger-children">
            <div class="integration-card fade-up">
                <div class="integration-icon">💬</div>
                <span>Discord</span>
            </div>
            <div class="integration-card fade-up">
                <div class="integration-icon">📢</div>
                <span>Slack</span>
            </div>
            <div class="integration-card fade-up">
                <div class="integration-icon">✈️</div>
                <span>Telegram</span>
            </div>
            <div class="integration-card fade-up">
                <div class="integration-icon">📧</div>
                <span>Email</span>
            </div>
            <div class="integration-card fade-up">
                <div class="integration-icon">🔗</div>
                <span>Webhooks</span>
            </div>
            <div class="integration-card fade-up">
                <div class="integration-icon">📊</div>
                <span>CSV Export</span>
            </div>
        </div>
    </div>
</section>


<!-- ========================================================================
   PRICING
   ======================================================================== -->
<section class="section" id="pricing">
    <div class="container">
        <div class="section-header fade-up">
            <h2>Simple, transparent pricing</h2>
            <p>Start free. Scale as you grow. No hidden fees.</p>
        </div>

        <div class="pricing-grid stagger-children">

            <!-- Free -->
            <div class="pricing-card fade-up">
                <div class="pricing-header">
                    <div class="pricing-tier">Free</div>
                    <div class="pricing-amount">
                        <span class="pricing-dollar">$0</span>
                        <span class="pricing-period">/ month</span>
                    </div>
                    <div class="pricing-annual">Free forever</div>
                </div>
                <div class="pricing-features">
                    <div class="pricing-feature"><span class="pricing-check">✓</span> 1 form</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> 100 submissions / month</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> 2 week data retention</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> 50 MB file storage</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Honeypot spam filter</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Origin locking</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Discord webhook</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Client read-only view</div>
                    <div class="pricing-feature"><span class="pricing-cross">✗</span> Email notifications</div>
                    <div class="pricing-feature"><span class="pricing-cross">✗</span> CSV export</div>
                </div>
                <a href="#waitlist" class="btn btn-ghost">Get Started Free</a>
            </div>

            <!-- Starter (featured) -->
            <div class="pricing-card featured fade-up">
                <div class="pricing-popular">
                    <span class="badge badge-accent">Most Popular</span>
                </div>
                <div class="pricing-header">
                    <div class="pricing-tier">Starter</div>
                    <div class="pricing-amount">
                        <span class="pricing-dollar">$9</span>
                        <span class="pricing-period">/ month</span>
                    </div>
                    <div class="pricing-annual">$90 / year — 2 months free</div>
                </div>
                <div class="pricing-features">
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Unlimited forms</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> 2,500 submissions / month</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> 2 month data retention</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> 1 GB file storage</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Email notifications</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Cloudflare Turnstile</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Slack, Discord &amp; Telegram
                    </div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Custom webhooks</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> CSV export</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Custom meta fields</div>
                </div>
                <a href="#waitlist" class="btn btn-primary">Choose Starter →</a>
            </div>

            <!-- Pro -->
            <div class="pricing-card fade-up">
                <div class="pricing-header">
                    <div class="pricing-tier">Pro</div>
                    <div class="pricing-amount">
                        <span class="pricing-dollar">$19</span>
                        <span class="pricing-period">/ month</span>
                    </div>
                    <div class="pricing-annual">$190 / year — 2 months free</div>
                </div>
                <div class="pricing-features">
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Unlimited forms</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> 15,000 submissions / month</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> 6 month data retention</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> 10 GB file storage</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Email delivery status</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Autoresponders</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Altcha (privacy CAPTCHA)</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Multi-variable sort</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Pre-sorted CSV export</div>
                    <div class="pricing-feature"><span class="pricing-check">✓</span> Overdue/stale indicators</div>
                </div>
                <a href="#waitlist" class="btn btn-ghost">Choose Pro</a>
            </div>

        </div>
    </div>
</section>


<!-- ========================================================================
   FAQ
   ======================================================================== -->
<section class="section" id="faq" style="background:var(--bg-surface);">
    <div class="container">
        <div class="section-header fade-up">
            <h2>Frequently asked questions</h2>
        </div>

        <div class="faq-list">
            <details class="faq-item fade-up">
                <summary>Do I need to install any npm packages or SDKs?</summary>
                <div class="faq-answer">
                    No. Formbox requires zero client-side JavaScript or build dependencies. Set your HTML form's
                    <code>action</code> attribute to your Formbox endpoint and you're done. Works with plain HTML,
                    React,
                    Next.js, Astro, Hugo, Webflow, Eleventy, and any static site generator.
                </div>
            </details>

            <details class="faq-item fade-up">
                <summary>How does the multi-layer spam protection work?</summary>
                <div class="faq-answer">
                    Formbox uses a defense-in-depth approach: invisible honeypot fields catch bots, submit-timing checks
                    flag
                    instant submissions, domain/origin locking prevents cross-site abuse, per-endpoint rate limiting
                    stops
                    floods, and optional Cloudflare Turnstile or Altcha proof-of-work challenges stop sophisticated bots
                    — all
                    without affecting real users.
                </div>
            </details>

            <details class="faq-item fade-up">
                <summary>What makes delivery monitoring different from other form backends?</summary>
                <div class="faq-answer">
                    Most form backends tell you a submission was received but have no idea if the notification email
                    actually
                    landed in your inbox. Formbox tracks sent, delivered, and bounced states per notification email per
                    recipient. If your email silently fails, you'll know immediately — before a client ever asks "is the
                    form
                    broken?"
                </div>
            </details>

            <details class="faq-item fade-up">
                <summary>Can I redirect visitors to my own thank-you page?</summary>
                <div class="faq-answer">
                    Yes. On paid plans, you can set a custom redirect URL per form so visitors are sent to your own
                    confirmation
                    page after submission instead of the default Formbox thank-you page.
                </div>
            </details>

            <details class="faq-item fade-up">
                <summary>What happens when I hit my monthly submission limit?</summary>
                <div class="faq-answer">
                    New submissions will be rejected with a clear error response until the next billing cycle or until
                    you
                    upgrade your plan. Existing submissions remain accessible and are never deleted before your
                    retention window
                    expires.
                </div>
            </details>

            <details class="faq-item fade-up">
                <summary>Can my clients see their own submissions?</summary>
                <div class="faq-answer">
                    Yes. You can issue scoped credentials per form or per folder. Clients get a read-only view of their
                    submissions without access to your account, other clients, or billing. New forms added to their
                    folder are
                    automatically visible — no need to re-issue credentials.
                </div>
            </details>
        </div>
    </div>
</section>


<!-- ========================================================================
   WAITLIST CTA
   ======================================================================== -->
<section class="section cta-section" id="waitlist">
    <div class="container">
        <div class="cta-inner fade-up">
            <h2>Get early access</h2>
            <p>Join the waitlist and be the first to know when Formbox opens to the public. Early members lock in beta
                pricing.</p>
            <form class="waitlist-form" id="waitlistForm" novalidate>
                <input type="email" class="waitlist-input" id="waitlistEmail" name="email" placeholder="you@domain.com"
                       required autocomplete="email"/>
                <button type="submit" class="btn btn-primary">Join Waitlist</button>
            </form>
            <p class="waitlist-note">No spam. Unsubscribe anytime. We'll only email you when the beta opens.</p>
        </div>
    </div>
</section>


<!-- ========================================================================
   FOOTER
   ======================================================================== -->
<footer class="footer">
    <div class="container">
        <div class="footer-grid">

            <!-- Brand -->
            <div class="footer-brand">
                <a href="/" class="navbar-logo">
                    <span class="logo-icon">F</span>
                    Formbox
                </a>
                <p>The form backend for static sites. Spam protection, delivery monitoring, and webhooks — no server
                    required.
                </p>
            </div>

            <!-- Product -->
            <div>
                <div class="footer-col-title">Product</div>
                <ul class="footer-links">
                    <li><a href="#">Spam Protection</a></li>
                    <li><a href="#">Delivery Monitoring</a></li>
                    <li><a href="#">File Uploads</a></li>
                    <li><a href="#">Webhooks &amp; Notifs</a></li>
                    <li><a href="#">Client Views</a></li>
                    <li><a href="#pricing">Pricing</a></li>
                </ul>
                <div class="footer-col-title" style="margin-top:24px;">Security</div>
                <ul class="footer-links">
                    <li><a href="#">Honeypot</a></li>
                    <li><a href="#">Turnstile</a></li>
                    <li><a href="#">Altcha</a></li>
                    <li><a href="#">Rate Limiting</a></li>
                </ul>
            </div>

            <!-- Integrations -->
            <div>
                <div class="footer-col-title">Integrations</div>
                <ul class="footer-links">
                    <li><a href="#">Discord</a></li>
                    <li><a href="#">Slack</a></li>
                    <li><a href="#">Telegram</a></li>
                    <li><a href="#">Custom Webhooks</a></li>
                    <li><a href="#">CSV Export</a></li>
                    <li><a href="#">Email Notifications</a></li>
                </ul>
            </div>

            <!-- Developers -->
            <div>
                <div class="footer-col-title">Developers</div>
                <ul class="footer-links">
                    <li><a href="#">Quickstart</a></li>
                    <li><a href="#">API Reference</a></li>
                    <li><a href="#">Changelog</a></li>
                    <li><a href="#">HTML Forms Guide</a></li>
                    <li><a href="#">Next.js Guide</a></li>
                    <li><a href="#">Astro Guide</a></li>
                    <li><a href="#">Hugo Guide</a></li>
                    <li><a href="#">Webflow Guide</a></li>
                    <li><a href="#">Eleventy Guide</a></li>
                </ul>
            </div>

            <!-- Compare -->
            <div>
                <div class="footer-col-title">Compare</div>
                <ul class="footer-links">
                    <li><a href="#">vs Formspree</a></li>
                    <li><a href="#">vs Basin</a></li>
                    <li><a href="#">vs Getform</a></li>
                    <li><a href="#">vs FormSubmit</a></li>
                    <li><a href="#">vs Netlify Forms</a></li>
                </ul>
                <div class="footer-col-title" style="margin-top:24px;">Templates</div>
                <ul class="footer-links">
                    <li><a href="#">Contact Form</a></li>
                    <li><a href="#">Feedback Form</a></li>
                    <li><a href="#">Newsletter Signup</a></li>
                    <li><a href="#">File Upload Form</a></li>
                </ul>
            </div>

            <!-- Solutions -->
            <div>
                <div class="footer-col-title">Solutions</div>
                <ul class="footer-links">
                    <li><a href="#">For Agencies</a></li>
                    <li><a href="#">For Freelancers</a></li>
                    <li><a href="#">For SaaS</a></li>
                    <li><a href="#">Next.js</a></li>
                    <li><a href="#">Astro</a></li>
                    <li><a href="#">Hugo</a></li>
                    <li><a href="#">Webflow</a></li>
                    <li><a href="#">Eleventy</a></li>
                    <li><a href="#">Static HTML</a></li>
                </ul>
            </div>

            <!-- Company + Legal -->
            <div>
                <div class="footer-col-title">Company</div>
                <ul class="footer-links">
                    <li><a href="#">About</a></li>
                    <li><a href="#">Blog</a></li>
                    <li><a href="#">Status Page</a></li>
                </ul>
                <div class="footer-col-title" style="margin-top:24px;">Legal</div>
                <ul class="footer-links">
                    <li><a href="#">Privacy Policy</a></li>
                    <li><a href="#">Terms of Service</a></li>
                    <li><a href="#">Cookie Policy</a></li>
                </ul>
            </div>

        </div>

        <!-- Bottom bar -->
        <div class="footer-bottom">
            <p>&copy; 2026 Formbox. All rights reserved.</p>
            <div class="footer-bottom-links">
                <a href="#">Privacy</a>
                <a href="#">Terms</a>
                <a href="#">Status</a>
            </div>
        </div>
    </div>
</footer>

<script src="/assets/script.js"></script>
</body>

</html>