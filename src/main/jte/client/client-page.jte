@import formbox.notifs.EmailStatus
@import formbox.submission.SubmissionItem
@import java.time.format.DateTimeFormatter
@import java.util.List

@param List<SubmissionItem> validSubmissions
@param List<SubmissionItem> spamSubmissions

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Client Dashboard | Formbox</title>
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
    <a href="/">Powered By Formbox</a>
    <h1>Client Dashboard</h1>
</header>

<main class="container">

    @template.submission-viewer(submissions = validSubmissions, name = "Valid Submissions")

    @template.submission-viewer(submissions = spamSubmissions, name = "Spam Submissions")

</main>

<footer class="container">
    <small>&copy; 2026 Formbox. Edge-powered form management backend.</small>
</footer>

<script>
    window.history.replaceState(null, '', window.location.origin + window.location.pathname);
</script>

</body>
</html>