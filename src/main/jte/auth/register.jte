<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Create Account | Formbox</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.pumpkin.min.css"/>
    <script src="https://cdn.jsdelivr.net/npm/htmx.org@2.0.10/dist/htmx.min.js"></script>
    <link rel="preconnect" href="https://challenges.cloudflare.com"/>
    <style>
        body {
            padding-inline: 15em
        }

        @media (max-width: 880px) {
            body {
                padding-inline: 5em
            }
        }

        @media (max-width: 575px) {
            body {
                padding-inline: 3em
            }
        }
    </style>
</head>
<body>

<header class="container">
    <nav>
        <ul>
            <li><strong><a href="/">← Back to Home Page</a></strong></li>
        </ul>
    </nav>
</header>

<main class="container">
    <section>
        <hgroup>
            <h1>Create a Formbox Account</h1>
            <ins id="alert-container"></ins>
        </hgroup>
    </section>

    <section>
        <form hx-post="/auth/signup" hx-target="#alert-container" hx-swap="outerHTML"
              hx-on::before-request="this.querySelector('button').setAttribute('aria-busy', 'true')"
              hx-on::after-request="this.querySelector('button').setAttribute('aria-busy', 'false')">
            <fieldset>
                <label>
                    Email
                    <input type="email" name="email" placeholder="you@example.com" required autocomplete="email"/>
                </label>
                <label>
                    Password
                    <input type="password" name="password" required autocomplete="current-password"/>
                </label>
            </fieldset>
            <div class="cf-turnstile" data-sitekey="0x4AAAAAAD1xEDYqYOi7tGNB" style="margin-top: 1rem;"></div>
            <button type="submit" aria-busy="false">Sign Up</button>
        </form>
        <p>Don't have an account? <a href="/auth/login">Sign In here →</a></p>
    </section>

    <section>
        <details>
            <summary role="button" class="outline">Didn't receive confirmation email?</summary>
            <ins id="alert-container-2"></ins>
            <form hx-post="/auth/resend-confirmation" hx-target="#alert-container-2" hx-swap="outerHTML"
                  hx-on::before-request="this.querySelector('button').setAttribute('aria-busy', 'true')"
                  hx-on::after-request="this.querySelector('button').setAttribute('aria-busy', 'false')">
                <fieldset role="group">
                    <label>
                        <input type="email" name="email" placeholder="you@example.com" autocomplete="email" required/>
                    </label>
                    <button type="submit" aria-busy="false">Resend Confirmation Link</button>
                </fieldset>
                <div class="cf-turnstile" data-sitekey="0x4AAAAAAD1xEDYqYOi7tGNB" style="margin-top: 1rem;"></div>
            </form>
        </details>
    </section>
</main>

<script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>
</body>
</html>