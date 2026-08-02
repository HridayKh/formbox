@param formbox.form.FormDto form

<!-- ========================= EMAIL ========================= -->
<details>
    <summary role="button" class="outline secondary">Email</summary>

    <form method="post" action="/forms/${form.folderId().toString()}/${form.id().toString()}/notifs/email">
        <fieldset>
            <label for="submitter-email-field">
                Submitter email field name
                <input type="text" id="submitter-email-field" name="submitter_email_field" placeholder="e.g. email"
                       disabled>
                <small>
                    Name of the form field that contains the submitter's own email address.
                </small>
            </label>
            <label for="submitter-email-notify-enabled">
                <input type="checkbox" id="submitter-email-notify-enabled" name="submitter_email_notify_enabled"
                       role="switch" disabled>
                Send the submitter an email
            </label>
        </fieldset>

        <button type="submit" disabled>Save</button>
    </form>
</details>

<!-- ========================= NOTIFICATIONS ========================= -->
<details>
    <summary role="button" class="outline secondary">Email Notifications</summary>

    <form method="post" action="/forms/${form.folderId().toString()}/${form.id().toString()}/notifs/notifications">
        <fieldset>
            <label for="email-notify-enabled">
                <input type="checkbox" id="email-notify-enabled" name="email_notify_enabled" role="switch" disabled>
                Send an email notification on submit
            </label>

            <label for="notify-to">
                To
                <input type="email" id="notify-to" name="notify_to" placeholder="you@example.com" multiple disabled>
            </label>

            <div class="grid">
                <label for="notify-cc">
                    CC
                    <input type="email" id="notify-cc" name="notify_cc" placeholder="cc@example.com" multiple disabled>
                </label>

                <label for="notify-bcc">
                    BCC
                    <input type="email" id="notify-bcc" name="notify_bcc" placeholder="bcc@example.com" multiple
                           disabled>
                </label>
            </div>
            <small>Separate multiple addresses with commas.</small>
        </fieldset>

        <button type="submit" disabled>Save</button>
    </form>
</details>

<!-- ========================= DISCORD ========================= -->
<details open>
    <summary role="button" class="secondary">Discord</summary>

    <article>
        <header>
            <h3>Discord</h3>
        </header>
        <form method="post" action="/forms/${form.folderId().toString()}/${form.id().toString()}/notifs/discord">
            <fieldset>
                <label for="discord-webhook-url">
                    Webhook URL
                    <input type="url" id="discord-webhook-url" name="webhookUrl"
                           value="${form.formNotifs().discordWebhookUrl()}"
                           placeholder="https://discord.com/api/webhooks/...">
                    <small>Leave empty to disable!</small>
                </label>

                <small><a href="#" target="_blank" rel="noopener noreferrer">How do I get a Discord webhook
                        URL?</a></small>

                <label>
                    Discord message body
                    <textarea rows="4" name="body"
                              placeholder="New submission from {{name}}: {{message}}">${form.formNotifs().discordBody()}</textarea>
                </label>
                <small>Supports form field variables like <code>{{field_name}}</code>.</small>
            </fieldset>

            <button type="submit">Save</button>
        </form>
    </article>
</details>

<!-- ========================= SLACK ========================= -->
<details>
    <summary role="button" class="outline secondary">Slack</summary>

    <form method="post" action="/forms/${form.folderId().toString()}/${form.id().toString()}/notifs/slack">
        <fieldset>
            <label for="slack-webhook-url">
                Webhook URL
                <input type="url" id="slack-webhook-url" name="slack_webhook_url"
                       placeholder="https://hooks.slack.com/services/..." disabled>
                <small>Leave empty to disable!</small>
            </label>
            <small><a href="#" target="_blank" rel="noopener noreferrer">How do I
                    get a Slack webhook URL?</a></small>

            <label for="slack-channel">
                Channel override <span>(optional)</span>
                <input type="text" id="slack-channel" name="slack_channel" placeholder="#submissions" disabled>
            </label>

            <label for="slack-body">
                Slack message body
                <textarea id="slack-body" name="slack_body" rows="4"
                          placeholder="New submission from {{name}}: {{message}}" disabled></textarea>
            </label>
            <small>Supports form field variables like <code>{{field_name}}</code>. Slack mrkdwn formatting is
                supported.</small>
        </fieldset>

        <button type="submit" disabled>Save</button>
    </form>
</details>

<!-- ========================= TELEGRAM ========================= -->
<details>
    <summary role="button" class="outline secondary">Telegram</summary>

    <form method="post" action="/forms/${form.folderId().toString()}/${form.id().toString()}/notifs/telegram">
        <fieldset>
            <div class="grid">
                <label for="telegram-bot-token">
                    Bot token
                    <input type="text" id="telegram-bot-token" name="telegram_bot_token" placeholder="123456789:AA..."
                           disabled>
                    <small>Leave empty to disable!</small>
                </label>

                <label for="telegram-chat-id">
                    Chat ID
                    <input type="text" id="telegram-chat-id" name="telegram_chat_id" placeholder="-1001234567890"
                           disabled>
                    <small>Leave empty to disable!</small>
                </label>
            </div>
            <small><a href="#" target="_blank" rel="noopener noreferrer"> How do I get a chat ID and bot
                    token?</a></small>

            <label for="telegram-body">
                Telegram message body
                <textarea id="telegram-body" name="telegram_body" rows="4"
                          placeholder="New submission from {{name}}: {{message}}" disabled></textarea>
            </label>
            <small>Supports form field variables like <code>{{field_name}}</code>. Basic HTML formatting is
                supported.</small>
        </fieldset>

        <button type="submit" disabled>Save</button>
    </form>
</details>

<!-- ========================= CUSTOM WEBHOOK ========================= -->
<details>
    <summary role="button" class="outline secondary">Custom Webhook</summary>

    <form method="post" action="/forms/${form.folderId().toString()}/${form.id().toString()}/notifs/webhook">
        <fieldset>
            <label for="webhook-enabled">
                <input type="checkbox" id="webhook-enabled" name="webhook_enabled" role="switch" disabled>
                Send a request to a custom webhook on submit
            </label>

            <div class="grid">
                <label for="webhook-method">
                    Method
                    <select id="webhook-method" name="webhook_method" disabled>
                        <option value="POST" selected>POST</option>
                        <option value="PUT">PUT</option>
                        <option value="PATCH">PATCH</option>
                        <option value="GET">GET</option>
                    </select>
                </label>

                <label for="webhook-content-type">
                    Content type
                    <select id="webhook-content-type" name="webhook_content_type" disabled>
                        <option value="application/json" selected>application/json</option>
                        <option value="application/x-www-form-urlencoded">application/x-www-form-urlencoded</option>
                    </select>
                </label>
            </div>

            <label for="webhook-url">
                Endpoint URL
                <input type="url" id="webhook-url" name="webhook_url" placeholder="https://example.com/webhook"
                       disabled>
            </label>

            <label for="webhook-headers">
                Custom headers <span>(optional, JSON)</span>
                <textarea id="webhook-headers" name="webhook_headers" rows="3"
                          placeholder='{"Authorization": "Bearer ..."}' disabled></textarea>
            </label>

            <label for="webhook-body">
                Body template <span>(optional, JSON)</span>
                <textarea id="webhook-body" name="webhook_body" rows="4"
                          placeholder='{"name": "{{name}}", "email": "{{email}}"}' disabled></textarea>
                <small>Leave blank to send the raw submission payload.</small>
            </label>
        </fieldset>

        <button type="submit" disabled>Save</button>
    </form>
</details>

<!-- ========================= ROUTING ========================= -->
<details>
    <summary role="button" class="outline secondary">Routing</summary>
    <p>Submission Routing Coming Soon!</p>
</details>