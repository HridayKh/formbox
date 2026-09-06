@param boolean showManageSubscription
@param Long balanceLeft
@param String email

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Support | Formbox</title>
    <meta name="description"
          content="Formbox Support - Contact support, give feedback, submit bugs, and request features."/>
    <link rel="stylesheet" href="/assets/pico.pumpkin.min.css"/>
    <script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>
    <style>
        .support-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(min(100%, 28rem), 1fr));
            gap: 1.5rem;
            margin-top: 1.5rem;
        }

        .support-card {
            border: 1px solid var(--pico-muted-border-color);
            border-radius: 0.75rem;
            overflow: hidden;
        }

        .support-card header {
            padding: 1rem 1.25rem;
            border-bottom: 1px solid var(--pico-muted-border-color);
            margin: 0;
        }

        .support-card header h3 {
            margin: 0;
            font-size: 1.05rem;
        }

        .support-card header small {
            color: var(--pico-muted-color);
        }

        /* Reduced font size styling for form elements */
        .support-card form {
            padding: 1.25rem;
            font-size: 0.85rem;
        }

        .support-card label {
            font-size: 0.8rem;
            margin-bottom: 0.25rem;
            font-weight: 600;
        }

        .support-card input,
        .support-card select,
        .support-card textarea {
            font-size: 0.85rem;
            padding: 0.4rem 0.6rem;
            margin-bottom: 0.75rem;
        }

        .support-card button {
            font-size: 0.85rem;
            padding: 0.5rem 1rem;
            margin-top: 0.5rem;
        }
    </style>
</head>
<body>

<header class="container">
    @template.dash.navbar(pageName="Support", balanceLeft=balanceLeft, showManageSubscription=showManageSubscription, email=email)
    <a href="/dashboard">&larr; Back to Dashboard</a>
    <h1>Support & Feedback</h1>
    <p>Have a question, found a bug, or want to request a feature? Use the forms below — they're all powered by
        Formbox!</p>
</header>
<main class="container">
    <div class="support-grid">
        <div class="support-card">
            <header>
                <h3>📧 Contact Support</h3>
                <small>Get help with your account or forms</small>
            </header>
            <form action="https://formbox.hridaykh.in/f/4298cea2-4e08-4cee-8ffd-e08f709a13db" method="POST">
                <input type="hidden" name="form_type" value="Contact Support">

                <div class="form-group">
                    <label for="contact-name">Your Name</label>
                    <input type="text" id="contact-name" name="name" placeholder="John Doe">
                </div>
                <div class="form-group">
                    <label for="contact-email">Email Address</label>
                    <input type="email" id="contact-email" name="email" value="${email}" required placeholder="you@example.com">
                </div>
                <div class="form-group">
                    <label for="contact-topic">Topic</label>
                    <select id="contact-topic" name="topic" required>
                        <option value="account">Account & Billing</option>
                        <option value="technical">Technical Support</option>
                        <option value="forms">Form Integration</option>
                        <option value="other">Other</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="contact-message">How can we help?</label>
                    <textarea id="contact-message" name="message" rows="3" required placeholder="Describe your issue..."></textarea>
                </div>

                <div class="cf-turnstile" data-sitekey="0x4AAAAAAD1xEDYqYOi7tGNB" style="margin-top: 1rem;"></div>
                <button type="submit">Send Message</button>
            </form>
        </div>

        <div class="support-card">
            <header>
                <h3>💬 Give Feedback</h3>
                <small>Tell us what you think about Formbox</small>
            </header>
            <form action="https://formbox.hridaykh.in/f/4298cea2-4e08-4cee-8ffd-e08f709a13db" method="POST">
                <input type="hidden" name="form_type" value="Give Feedback">

                <div class="form-group">
                    <label for="feedback-email">Email Address</label>
                    <input type="email" id="feedback-email" name="email" value="${email}" required placeholder="you@example.com">
                </div>
                <div class="form-group">
                    <label for="feedback-rating">Overall Experience</label>
                    <select id="feedback-rating" name="rating" required>
                        <option value="5">⭐⭐⭐⭐⭐ Excellent</option>
                        <option value="4">⭐⭐⭐⭐ Good</option>
                        <option value="3">⭐⭐⭐ Neutral</option>
                        <option value="2">⭐⭐ Poor</option>
                        <option value="1">⭐ Very Poor</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="feedback-message">Your Feedback</label>
                    <textarea id="feedback-message" name="feedback" rows="3" required placeholder="Share your thoughts or ideas..."></textarea>
                </div>

                <div class="cf-turnstile" data-sitekey="0x4AAAAAAD1xEDYqYOi7tGNB" style="margin-top: 1rem;"></div>
                <button type="submit">Submit Feedback</button>
            </form>
        </div>

        <div class="support-card">
            <header>
                <h3>🐛 Submit a Bug</h3>
                <small>Report something that isn't working right</small>
            </header>
            <form action="https://formbox.hridaykh.in/f/4298cea2-4e08-4cee-8ffd-e08f709a13db" method="POST">
                <input type="hidden" name="form_type" value="Bug Report">

                <div class="form-group">
                    <label for="bug-email">Email Address</label>
                    <input type="email" id="bug-email" name="email" value="${email}" required placeholder="you@example.com">
                </div>
                <div class="form-group">
                    <label for="bug-severity">Severity Level</label>
                    <select id="bug-severity" name="severity">
                        <option value="low">Low - Minor cosmetic issue</option>
                        <option value="medium" selected>Medium - Feature partially broken</option>
                        <option value="high">High - Key functionality unavailable</option>
                        <option value="critical">Critical - System crash or data loss</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="bug-description">Issue Description</label>
                    <textarea id="bug-description" name="issue" rows="2" required placeholder="What happened?"></textarea>
                </div>
                <div class="form-group">
                    <label for="bug-steps">Steps to Reproduce</label>
                    <textarea id="bug-steps" name="steps_to_reproduce" rows="2" placeholder="1. Go to... 2. Click on..."></textarea>
                </div>

                <div class="cf-turnstile" data-sitekey="0x4AAAAAAD1xEDYqYOi7tGNB" style="margin-top: 1rem;"></div>
                <button type="submit">Report Bug</button>
            </form>
        </div>

        <div class="support-card">
            <header>
                <h3>💡 Request a Feature</h3>
                <small>Suggest improvements or new functionality</small>
            </header>
            <form action="https://formbox.hridaykh.in/f/4298cea2-4e08-4cee-8ffd-e08f709a13db" method="POST">
                <input type="hidden" name="form_type" value="Feature Request">

                <div class="form-group">
                    <label for="feature-email">Email Address</label>
                    <input type="email" id="feature-email" name="email" value="${email}" required placeholder="you@example.com">
                </div>
                <div class="form-group">
                    <label for="feature-title">Feature Title</label>
                    <input type="text" id="feature-title" name="feature_title" required placeholder="Short summary">
                </div>
                <div class="form-group">
                    <label for="feature-usecase">Use Case / Urgency</label>
                    <select id="feature-usecase" name="importance">
                        <option value="nice_to_have">Nice to have</option>
                        <option value="important">Important for workflow</option>
                        <option value="essential">Essential requirement</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="feature-details">Details</label>
                    <textarea id="feature-details" name="feature_details" rows="3" required placeholder="Explain how this feature would help you..."></textarea>
                </div>

                <div class="cf-turnstile" data-sitekey="0x4AAAAAAD1xEDYqYOi7tGNB" style="margin-top: 1rem;"></div>
                <button type="submit">Submit Request</button>
            </form>
        </div>
    </div>
</main>
<footer class="container">
    <small>&copy; 2026 Formbox. Edge-powered form management backend.</small>
</footer>

</body>
</html>