@import formbox.folder.FolderDto
@import formbox.form.FormDto
@import formbox.submission.SubmissionItem
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
@param List<SubmissionItem> validSubmissions
@param List<SubmissionItem> spamSubmissions

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
</header>

<%--
1. [x] Move folders
2. [x] Form settings (delete is here btw)
3. [ ] Intergrations and Routing
4. [ ] View submissions (Seperate Template to Render Submissions so Submission Info THere)
5. [ ] Export Submissions to csv
6. [ ] Client Creds (Single pregenerated cred that dont need db, coz they signed using form id)
7. [ ] Copy button for endpoint (the new presigned kind)
8. [ ] Links to templates, maybe even a template generator

notes:
- Change isSpam to spamReason
- file attachment stored as fieldName and fieldName_url

what to show per submission:
1. Timestamp
2. Email delivery status
3. Submission preview (truncated concatenation of field values)
4. spam reason
5. dropdown for full fields
--%>

<main class="container">
    <section id="form-top">
        <strong>Form Endpoint, Use This With <code>action="post"</code></strong>
        <div role="group">
            <label for="url-text"></label><input type="text" id="url-text"
                                                 value="https://formbox.hridaykh.in/f/${form.id().toString()}"
                                                 readonly/>
            <button onclick="copyURL()" id="copy-btn">Copy</button>
        </div>
    </section>

    <section id="move-folders">
        <details>
            <summary role="button" class="outline">Move this form, current folder: '${folderName}'</summary>
            <div class="grid">
                @for(FolderDto folder : folders)
                    !{if(folder.id().equals(form.folderId())) continue;}
                    <form action="/forms/${folder.id().toString()}/${form.id().toString()}/move" method="POST">
                        <button type="submit"
                                class="small outline"
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
            @template.dash.formUpdate(form = form)
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
</script>

</body>
</html>