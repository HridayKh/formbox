@param boolean showManageSubscription
@param Long balanceLeft
@param String email

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Allowed Emails | Formbox</title>
    <meta name="description" content="Manage and verify your allowed email addresses on Formbox."/>
    <link rel="stylesheet" href="/assets/pico.pumpkin.min.css"/>
</head>
<body>

<header class="container">
    @template.dash.navbar(pageName="Allowed Emails", balanceLeft=balanceLeft, showManageSubscription=showManageSubscription, email=email)
    <a href="/dashboard">&larr; Back to Dashboard</a>
    <h1>Allowed Emails</h1>
</header>

<main class="container">
    <article>
        <header>
            <strong>Email Verification Management</strong>
        </header>
        <p>
            This page will allow you to manage and verify the email addresses you've authorized for use with Formbox
            (e.g. for email notifications and autoresponders).
        </p>
        <p>
            <mark>Coming soon!</mark>
        </p>
    </article>
</main>

<footer class="container">
    <small>&copy; 2026 Formbox. Edge-powered form management backend.</small>
</footer>

</body>
</html>
