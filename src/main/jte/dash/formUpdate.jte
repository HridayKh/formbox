<form action="/forms/update" method="post">

    <input type="hidden" name="id" value="">

    <h3>General Configuration</h3>

    <label for="name">Form Name</label><br>
    <input type="text" id="name" name="name" required><br><br>

    <label for="active">
        <input type="checkbox" id="active" name="active" value="true">
        Endpoint is Active & Accepting Submissions
    </label><br><br>

    <label for="redirectUrl">Custom Redirect URL</label><br>
    <input type="url" id="redirectUrl" name="redirectUrl" placeholder="https://yoursite.com/thank-you"><br><br>

    <h3>Security, Spam & Rate Limits</h3>

    <label for="honeypotName">Honeypot Trap Field Name</label><br>
    <input type="text" id="honeypotName" name="honeypotName" placeholder="_gotcha" required><br><br>

    <label for="turnstileSecretKey">Cloudflare Turnstile Secret Key</label><br>
    <input type="password" id="turnstileSecretKey" name="turnstileSecretKey" placeholder="0x4AAAAAA..."><br><br>

    <label for="altchaEnabled">
        <input type="checkbox" id="altchaEnabled" name="altchaEnabled" value="true">
        Enable Altcha Proof-of-Work Challenge
    </label><br><br>

    <label for="rateLimitRpm">Rate Limit (Requests Per Minute)</label><br>
    <input type="number" id="rateLimitRpm" name="rateLimitRpm" min="1" required><br><br>

    <h3>Capabilities & Format Controls</h3>

    <label for="allowFiles">
        <input type="checkbox" id="allowFiles" name="allowFiles" value="true">
        Allow File Attachment Uploads
    </label><br><br>

    <label for="allowHtmx">
        <input type="checkbox" id="allowHtmx" name="allowHtmx" value="true">
        Allow AJAX / HTMX Form Submissions
    </label><br><br>

    <label for="allowJson">
        <input type="checkbox" id="allowJson" name="allowJson" value="true">
        Allow JSON API Submissions (Content-Type: application/json)
    </label><br><br>

    <label for="emailDigestsEnabled">
        <input type="checkbox" id="emailDigestsEnabled" name="emailDigestsEnabled" value="true">
        Enable Daily Email Digests
    </label><br><br>

    <h3>Notification Integrations</h3>

    <label for="discordNotifs">
        <input type="checkbox" id="discordNotifs" name="discordNotifs" value="true">
        Discord Webhook Notifications
    </label><br><br>

    <label for="slackNotifs">
        <input type="checkbox" id="slackNotifs" name="slackNotifs" value="true">
        Slack Channel Notifications
    </label><br><br>

    <label for="telegramNotifs">
        <input type="checkbox" id="telegramNotifs" name="telegramNotifs" value="true">
        Telegram Bot Notifications
    </label><br><br>

    <label for="customWebhooks">
        <input type="checkbox" id="customWebhooks" name="customWebhooks" value="true">
        Custom Webhook Dispatch Endpoint
    </label><br><br>

    <h3>Custom Field Validation Rules</h3>

    <label for="fieldValidationsRaw">Validation Rules (one per line, e.g. email:required)</label><br>
    <textarea id="fieldValidationsRaw" name="fieldValidationsRaw" rows="4" placeholder="email:required&#10;age:min:18"></textarea><br><br>

    <button type="submit">Save Form Configurations</button>

</form>
