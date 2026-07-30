@param boolean showManageSubscription
@param Long balanceLeft
@param String email
@param String msg

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Form Endpoints Dashboard | Formbox</title>
    <meta name="description" content="Formbox - Form backend for static sites powered by Cloudflare edge."/>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.pumpkin.min.css"/>
    <script src="https://cdn.jsdelivr.net/npm/htmx.org@2.0.10/dist/htmx.min.js"></script>
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
    @template.dash.navbar(pageName="Dashboard", balanceLeft=balanceLeft, showManageSubscription=showManageSubscription, email=email)
    @if(msg != null)
        <div class="container">
            <mark>${msg}</mark>
        </div>
    @endif

</header>

<main class="container">
    <section id="create-folder"></section>
    <section id="list-folders">
        <details>
            <summary role="button" class="outline"><strong> ACME Corporation 1234 </strong></summary>
            <div role="group">
                <span></span>
                <a href="#" role="button" class="small">
                    + Create Form
                </a>
                <span></span>
                <a href="#" role="button" class="secondary small">
                    Rename This Folder
                </a>
                <span></span>
                <a href="#" role="button" class="contrast outline small">
                    Delete This Folder
                </a>
                <span></span>
            </div>
            <table data-theme="dark">
                <tbody>
                <tr>
                    <td><a href="#">Manage Form Form-Name</a></td>
                    <td><a href="#">Manage Form Form-Name</a></td>
                    <td><a href="#">Manage Form Form-Name</a></td>
                </tr>
                <tr>
                    <td><a href="#">Manage Form Form-Name</a></td>
                    <td><a href="#">Manage Form Form-Name</a></td>
                    <td><a href="#">Manage Form Form-Name</a></td>
                </tr>
                </tbody>
            </table>
        </details>
    </section>
</main>

<footer class="container">
    <small>&copy; 2026 Formbox. Edge-powered form management backend.</small>
</footer>

</body>
</html>