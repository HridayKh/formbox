@import java.util.List

@param boolean showManageSubscription
@param Long balanceLeft
@param String email
@param String msg
@param List<String> verifiedEmails

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
    <p>Verify email addresses to use them in form notification settings (To, CC, BCC).</p>
    @if(msg != null && !msg.isBlank())
        <mark>${msg}</mark>
    @endif
</header>

<main class="container">

    <section>
        <h2>Add a New Email</h2>
        <form method="post" action="/dashboard/emails">
            <fieldset role="group">
                <input type="email" name="email" placeholder="you@example.com" required autofocus/>
                <input type="submit" value="Send Verification Email"/>
            </fieldset>
            <small>A verification email will be sent with a link to confirm the address.</small>
        </form>
    </section>

    <section>
        <h2>Verified Emails</h2>
        @if(verifiedEmails == null || verifiedEmails.isEmpty())
            <p>No verified emails yet. Add one above to get started.</p>
        @else
            <table>
                <thead>
                    <tr>
                        <th>Email Address</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    @for(String verifiedEmail : verifiedEmails)
                        <tr>
                            <td>${verifiedEmail}</td>
                            <td>
                                <form method="post" action="/dashboard/emails-remove" style="margin: 0;">
                                    <input type="hidden" name="email" value="${verifiedEmail}"/>
                                    <button type="submit" class="outline contrast small"
                                            onclick="return confirm('Remove ${verifiedEmail}?');">
                                        Remove
                                    </button>
                                </form>
                            </td>
                        </tr>
                    @endfor
                </tbody>
            </table>
        @endif
    </section>

</main>

<footer class="container">
    <small>&copy; 2026 Formbox. Edge-powered form management backend.</small>
</footer>

<script>
    window.history.replaceState(null, '', window.location.origin + window.location.pathname);
</script>

</body>
</html>
