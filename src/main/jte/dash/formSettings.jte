@param formbox.form.FormDto form

<form action="/forms/${form.id().toString()}/update" method="post">

    <input type="hidden" name="id" value="${form.id().toString()}">

    <h3>General Configuration</h3>

    <label for="name">Form Name
        <input type="text" id="name" name="name" required value="${form.name()}">
    </label>

    <label for="active">
        <input type="checkbox" id="active" name="active" value="true" checked="${form.isActive()}">
        Endpoint is Active & Accepting Submissions
    </label>

    <label for="redirectUrl">Custom Redirect URL
        <input type="url" id="redirectUrl" name="redirectUrl" placeholder="https://yoursite.com/thank-you"
               value="${form.redirectUrl() != null ? form.redirectUrl() : ""}">
    </label>

    <h3>Security, Spam & Rate Limits</h3>

    <label for="honeypotName">Honeypot Trap Field Name
        <input type="text" id="honeypotName" name="honeypotName" placeholder="_gotcha" required
               value="${form.honeypotName()}">
    </label>

    <label for="turnstileSecretKey">Cloudflare Turnstile Secret Key
        <input type="password" id="turnstileSecretKey" name="turnstileSecretKey" placeholder="0x4AAAAAA..."
               value="${form.turnstileSecretKey() != null ? form.turnstileSecretKey() : ""}">
    </label>

    <label for="altchaEnabled">
        <input type="checkbox" id="altchaEnabled" name="altchaEnabled" value="true">
        Enable Altcha Proof-of-Work Challenge
    </label>

    <label for="rateLimitRpm">Rate Limit (Requests Per Minute)
        <input type="number" id="rateLimitRpm" name="rateLimitRpm" min="1" required value="${form.rateLimitRpm()}">
    </label>

    <h3>Capabilities & Format Controls</h3>

    <label for="allowFiles">
        <input type="checkbox" id="allowFiles" name="allowFiles" value="true" checked="${form.allowFiles()}">
        Allow File Attachment Uploads
    </label>

    <label for="allowHtmx">
        <input type="checkbox" id="allowHtmx" name="allowHtmx" value="true" checked="${form.allowHtmx()}">
        Allow AJAX / HTMX Form Submissions
    </label>

    <label for="allowJson">
        <input type="checkbox" id="allowJson" name="allowJson" value="true" checked="${form.allowJson()}">
        Allow JSON API Submissions (Content-Type: application/json)
    </label>

    <h3>Custom Field Validation Rules</h3>

    <label for="fieldValidationsRaw">Validation Rules (one per line, e.g. email:required)
        <textarea id="fieldValidationsRaw" name="fieldValidationsRaw" rows="4"
                  placeholder="email:required&#10;age:min:18">${form.fieldValidations() != null ? String.join("\n", form.fieldValidations()) : ""}</textarea>
    </label>

    <button type="submit">Save Form Configurations</button>

</form>

<hr/>

<form action="/forms/${form.folderId().toString()}/${form.id().toString()}/delete" method="POST">
    <h2>DANGER ZONE</h2>
    <button type="submit"
            class="contrast outline small"
            onclick="return confirm('Are you sure you want to delete this form (${form.name()})?');">
        Delete This Form
    </button>
</form>

<hr/>
