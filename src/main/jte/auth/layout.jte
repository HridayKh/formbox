@param String title
@param gg.jte.Content content

<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>${title} | Formbox</title>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.pumpkin.min.css"/>
    <script src="https://cdn.jsdelivr.net/npm/htmx.org@2.0.10/dist/htmx.min.js"></script>
    <link rel="preconnect" href="https://challenges.cloudflare.com"/>
    <style>
        body {
            padding-inline: 15em
        }

        @media (max-width: 880px) {
            body {
                padding-inline: 5em
            }
        }

        @media (max-width: 575px) {
            body {
                padding-inline: 3em
            }
        }
    </style>
</head>
<body>

<header class="container">
    <nav>
        <ul>
            <li><strong><a href="/">← Back to Home Page</a></strong></li>
        </ul>
    </nav>
</header>

<main class="container">
    ${content}
</main>

<script src="https://challenges.cloudflare.com/turnstile/v0/api.js" async defer></script>
</body>
</html>
