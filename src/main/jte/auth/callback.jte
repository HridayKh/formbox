<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Verifying Session | Formbox</title>
    <link rel="stylesheet" href="/assets/pico.pumpkin.min.css"/>
    <script src="https://cdn.jsdelivr.net/npm/htmx.org@2.0.10/dist/htmx.min.js"></script>
</head>
<body>

    <main class="container" style="max-width: 500px; padding-top: 5rem; text-align: center;">
        <article>
            <header>
                <h3 aria-busy="true">Verifying Session</h3>
            </header>
            <p>Finalizing authentication, please wait a moment...</p>
        </article>
    </main>

    <script>
        function getHashParams() {
            const hash = window.location.hash.substring(1);
            const params = new URLSearchParams(hash);
            return {
                access_token: params.get("access_token"),
                refresh_token: params.get("refresh_token"),
                expires_in: params.get("expires_in") || 3600
            };
        }

        window.addEventListener("DOMContentLoaded", () => {
            if (window.location.hash) {
                const tokens = getHashParams();
                htmx.ajax('POST', '/auth/session-callback', { values: tokens });
            } else {
                window.location.href = "/auth/login";
            }
        });
    </script>
</body>
</html>