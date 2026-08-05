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
    <a href="/forms/${form.folderId().toString()}/${form.id().toString()}">Go Back</a>
    <h1>Managing Form '${form.name()}'</h1>
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