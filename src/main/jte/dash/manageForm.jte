@import formbox.folder.FolderDto
@import formbox.form.FormDto
@import formbox.shared.Entitlements
@import java.util.List

@param boolean showManageSubscription
@param Long balanceLeft
@param String email
@param String msg
@param List<FolderDto> folders
@param FormDto form
@param String folderName
@param Entitlements entitlements
@param String creds

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Form Endpoints Dashboard | Formbox</title>
    <meta name="description" content="Formbox - Form backend for static sites powered by Cloudflare edge."/>
    <link rel="stylesheet" href="/assets/pico.pumpkin.min.css"/>
    <style>
        .small {
            margin-bottom: 0;
            padding: 0.25rem 0.75rem;
            font-size: 0.85rem;
        }
    </style>
</head>
<body>

<header class="container">
    @template.dash.navbar(pageName=folderName + " → " + form.name(), balanceLeft=balanceLeft, showManageSubscription=showManageSubscription, email=email)
    <a href="/dashboard">Go To Dashboard</a>
    <h1>Managing Form '${form.name()}'</h1>
    @if(msg != null && !msg.isBlank())
        <div class="container">
            <mark>${msg}</mark>
        </div>
    @endif
    <div role="search">
        <a role="button" class="outline"
           href="/forms/${form.folderId().toString()}/${form.id().toString()}/view-submissions">
            View Submissions
        </a>
    </div>
</header>

<%--
1. [x] Move folders
2. [x] Form settings (delete is here btw)
3. [x] Intergrations and Routing
4. [x] View submissions (Seperate Template to Render Submissions so Submission Info THere)
5. [ ] Client Creds (Single pregenerated cred that dont need db, coz they signed using form id)
6. [ ] Export Submissions to csv
7. [x] Copy button for endpoint
8. [ ] Links to templates, maybe even a template generator
--%>

<main class="container">
    <section id="form-top">
        <article>
            <header style="display: flex; justify-content: space-between; align-items: center; gap: 1rem;">
                <strong>Form Endpoint</strong>
                <small>Use with <code>action="POST"</code></small>
            </header>

            <!-- Hidden data field for clipboard JS -->
            <span style="display: none" id="url-text">https://formbox.hridaykh.in/f/${form.id().toString()}</span>

            <div>
                <label for="form-endpoint-url">
                    <small><strong>Submission Endpoint URL</strong></small>
                </label>
                <fieldset role="group" style="margin-bottom: 0;">
                    <input
                            id="form-endpoint-url"
                            type="text"
                            value="https://formbox.hridaykh.in/f/${form.id().toString()}"
                            readonly
                            aria-label="Form Endpoint URL"
                    />
                    <button onclick="copyURL()" id="copy-btn">
                        Copy
                    </button>
                </fieldset>
            </div>
        </article>
    </section>

    <section id="client-creds">
        <article>
            <header>
                <strong>Client Page Credentials</strong>
            </header>

            <!-- Hidden data fields for clipboard JS -->
            <span style="display: none" id="creds-text">${creds}</span>

            <div style="display: flex; flex-direction: column; gap: 1rem;">
                <!-- Field 1: Endpoint Link -->
                <div>
                    <label>
                        <small><strong>Client Page Link</strong></small>
                        <br/>
                        <a
                                href="https://formbox.hridaykh.in/c/${form.id().toString()}"
                                target="_blank"
                                rel="noopener noreferrer"
                        >
                            https://formbox.hridaykh.in/c/${form.id().toString()}
                        </a>
                    </label>
                </div>

                <!-- Field 2: Password -->
                <div>
                    <label for="client-page-password">
                        <small><strong>Client Page Password</strong></small>
                    </label>
                    <fieldset role="group" style="margin-bottom: 0;">
                        <input
                                id="client-page-password"
                                type="text"
                                value="${creds}"
                                readonly
                                aria-label="Client Page Password"
                        />
                        <button onclick="copyCREDS()" id="creds-copy-btn">
                            Copy
                        </button>
                    </fieldset>
                </div>
            </div>
        </article>
    </section>

    <section id="move-folders">
        <details>
            <summary role="button" class="outline">Move this form, current folder: '${folderName}'</summary>
            <div class="grid">
                @for(FolderDto folder : folders)
                    !{if(folder.id().equals(form.folderId())) continue;}
                    <form action="/forms/${folder.id().toString()}/${form.id().toString()}/move" method="POST">
                        <button type="submit"
                                class="small outline secondary"
                                onclick="return confirm('Are you sure you want to move this form to ${folder.name()}?');">
                            Move form to ${folder.name()}
                        </button>
                    </form>
                @endfor
            </div>
        </details>
    </section>

    <section id="form-update">
        <details>
            <summary role="button" class="outline">Settings</summary>
            @template.dash.formSettings(form = form)
        </details>
    </section>

    <section id="notifs-integrations">
        <details>
            <summary role="button" class="outline">Email, Notifications, Integrations, and Routing</summary>
            <div style="padding-inline: 5em">
                @template.dash.formNotifs(form = form)
            </div>
        </details>
    </section>
</main>

<footer class="container">
    <small>&copy; 2026 Formbox. Edge-powered form management backend.</small>
</footer>

<script>
    window.history.replaceState(null, '', window.location.origin + window.location.pathname);

    function copyURL() {
        const textToCopy = document.getElementById("url-text").innerText;

        navigator.clipboard.writeText(textToCopy).then(() => {
            const btn = document.getElementById("copy-btn");

            // Update button visual state
            btn.innerText = "Copied!";
            btn.classList.add("copied");

            // Reset button after 2 seconds
            setTimeout(() => {
                btn.innerText = "Copy";
                btn.classList.remove("copied");
            }, 2000);
        }).catch(err => {
            console.error("Failed to copy text: ", err);
            alert("Failed to copy to clipboard.");
        });
    }

    function copyCREDS() {
        const textToCopy = document.getElementById("creds-text").innerText;

        navigator.clipboard.writeText(textToCopy).then(() => {
            const btn = document.getElementById("creds-copy-btn");

            // Update button visual state
            btn.innerText = "Copied!";
            btn.classList.add("copied");

            // Reset button after 2 seconds
            setTimeout(() => {
                btn.innerText = "Copy";
                btn.classList.remove("copied");
            }, 2000);
        }).catch(err => {
            console.error("Failed to copy text: ", err);
            alert("Failed to copy to clipboard.");
        });
    }

</script>

</body>
</html>