@param boolean showManageSubscription
@param Long balanceLeft
@param String email

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Support | Formbox</title>
    <meta name="description" content="Formbox Support - Contact support, give feedback, submit bugs, and request features."/>
    <link rel="stylesheet" href="/assets/pico.pumpkin.min.css"/>
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
        .support-card iframe {
            width: 100%;
            min-height: 22rem;
            border: none;
            display: block;
        }
    </style>
</head>
<body>

<header class="container">
    @template.dash.navbar(pageName="Support", balanceLeft=balanceLeft, showManageSubscription=showManageSubscription, email=email)
    <a href="/dashboard">&larr; Back to Dashboard</a>
    <h1>Support & Feedback</h1>
    <p>Have a question, found a bug, or want to request a feature? Use the forms below — they're all powered by Formbox!</p>
</header>

<main class="container">
    <div class="support-grid">
        <div class="support-card">
            <header>
                <h3>📧 Contact Support</h3>
                <small>Get help with your account or forms</small>
            </header>
            <iframe
                src="https://formbox.hridaykh.in/f/abcd1234"
                title="Contact Support Form"
                loading="lazy"
            ></iframe>
        </div>

        <div class="support-card">
            <header>
                <h3>💬 Give Feedback</h3>
                <small>Tell us what you think about Formbox</small>
            </header>
            <iframe
                src="https://formbox.hridaykh.in/f/abcd1234"
                title="Give Feedback Form"
                loading="lazy"
            ></iframe>
        </div>

        <div class="support-card">
            <header>
                <h3>🐛 Submit a Bug</h3>
                <small>Report something that isn't working right</small>
            </header>
            <iframe
                src="https://formbox.hridaykh.in/f/abcd1234"
                title="Submit Bug Form"
                loading="lazy"
            ></iframe>
        </div>

        <div class="support-card">
            <header>
                <h3>💡 Request a Feature</h3>
                <small>Suggest improvements or new functionality</small>
            </header>
            <iframe
                src="https://formbox.hridaykh.in/f/abcd1234"
                title="Request Feature Form"
                loading="lazy"
            ></iframe>
        </div>
    </div>
</main>

<footer class="container">
    <small>&copy; 2026 Formbox. Edge-powered form management backend.</small>
</footer>

</body>
</html>
