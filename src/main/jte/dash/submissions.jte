@import formbox.form.FormDto
@import formbox.submission.SubmissionItem
@import java.util.List

@param Long balanceLeft
@param boolean showManageSubscription
@param String email
@param FormDto form
@param String folderName
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
</head>
<body>

<header class="container">
    @template.dash.navbar(pageName=folderName + " → " + form.name(), balanceLeft=balanceLeft, showManageSubscription=showManageSubscription, email=email)
    <div style="display: flex; justify-content: space-between; align-items: center;">
        <a href="/forms/${form.folderId().toString()}/${form.id().toString()}">&larr; Back to Form</a>
        <form action="/forms/${form.folderId().toString()}/${form.id().toString()}/export-csv" method="post" style="margin: 0;">
            <button type="submit" class="secondary outline small" style="margin: 0;">
                Export Submissions CSV
            </button>
        </form>
    </div>
    <h1>Submissions for '${form.name()}'</h1>
</header>

<main class="container">

    @template.submission-viewer(submissions = validSubmissions, name = "Valid Submissions")

    @template.submission-viewer(submissions = spamSubmissions, name = "Spam Submissions")

</main>

<footer class="container">
    <small>&copy; 2026 Formbox. Edge-powered form management backend.</small>
</footer>

</body>
</html>