@param String msg

@template.auth.layout(title = "Sign In", content = @`
    <section>
        <hgroup>
            <h1>Sign In to Formbox</h1>
            <ins id="alert-container">
                @if(msg != null)
                    @if(msg.equals("check_email"))
                        Registration successful! Please check your email for the confirmation link.
                    @elseif(msg.equals("logged_out"))
                        You have been safely logged out.
                    @elseif(msg.equals("unauthorized"))
                        Please log in to access your dashboard.
                    @elseif(msg.equals("magic_link_invalid"))
                        Invalid magic link!
                    @endif
                @endif
            </ins>
        </hgroup>
    </section>

    <section>
        <form hx-post="/auth/login"
              hx-target="#alert-container"
              hx-swap="outerHTML"
              hx-on::before-request="this.querySelector('button[type=submit]:focus')?.setAttribute('aria-busy', 'true')"
              hx-on::after-request="this.querySelectorAll('button').forEach(b => b.setAttribute('aria-busy', 'false'))">
            <fieldset>
                <label>
                    Email
                    <input type="email" name="email" placeholder="you@example.com" required autocomplete="email"/>
                </label>
                <label>
                    Password
                    <input id="password-input" type="password" name="password" autocomplete="current-password"/>
                </label>
            </fieldset>

            <div class="cf-turnstile" data-sitekey="0x4AAAAAAD1xEDYqYOi7tGNB" style="margin-top: 1rem;"></div>

            <button type="submit"
                    name="action"
                    value="password_login"
                    onclick="document.getElementById('password-input').required = true">
                Sign In
            </button>

            <button type="submit"
                    name="action"
                    value="magic_link"
                    onclick="document.getElementById('password-input').required = false">
                Send Magic Link
            </button>
        </form>
        <p>Don't have an account? <a href="/auth/signup">Register here →</a></p>
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