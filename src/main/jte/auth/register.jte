@template.auth.layout(title = "Create Account", content = @`
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
`)