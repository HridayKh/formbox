@import formbox.form.FormDto
@import formbox.notifs.EmailStatus
@import formbox.submission.SubmissionItem
@import java.time.format.DateTimeFormatter
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
    <a href="/forms/${form.folderId().toString()}/${form.id().toString()}">Go Back</a>
    <h1>Managing Form '${form.name()}'</h1>
</header>

<main class="container">

    <article>
        <header>
            <strong>Valid Submissions</strong> (${validSubmissions.size()})
        </header>

        @if(validSubmissions.isEmpty())
            <p><em>No submissions yet.</em></p>
        @else
            <table>
                <thead>
                <tr>
                    <th scope="col">Timestamp</th>
                    <th scope="col">Autoresponse</th>
                    <th scope="col">Preview</th>
                    <th scope="col"></th>
                </tr>
                </thead>
                <tbody>
                @for(SubmissionItem s : validSubmissions)
                    !{String preview = s.payload().entrySet().stream()
                        .filter(entry -> !entry.getKey().endsWith("__url"))
                        .map(entry -> entry.getValue() != null ? entry.getValue().toString() : "")
                        .collect(java.util.stream.Collectors.joining(" | "));}
                    !{ String shortPreview = preview.length() > 50 ? preview.substring(0, 50) + "…" : preview; }
                    <tr>
                        <td>${s.createdAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}</td>
                        <td>
                            @if(s.emailAutoresponseEmailStatus() == null)
                                <small style="color: var(--pico-muted-color);">DISABLED</small>
                            @else
                                @if(s.emailAutoresponseEmailStatus() == null)
                                    <mark style="background-color: #6c757d; color: #ffffff;">DISABLED</mark>
                                @elseif(s.emailAutoresponseEmailStatus() == EmailStatus.DELIVERED)
                                    <mark style="background-color: #28a745; color: #ffffff;">DELIVERED</mark>
                                @elseif(s.emailAutoresponseEmailStatus() == EmailStatus.SENT)
                                    <mark style="background-color: #17a2b8; color: #ffffff;">SENT</mark>
                                @elseif(s.emailAutoresponseEmailStatus() == EmailStatus.SOFT_BOUNCE)
                                    <mark style="background-color: #fd7e14; color: #ffffff;">SOFT BOUNCE</mark>
                                @elseif(s.emailAutoresponseEmailStatus() == EmailStatus.HARD_BOUNCE)
                                    <mark style="background-color: #dc3545; color: #ffffff;">HARD BOUNCE</mark>
                                @elseif(s.emailAutoresponseEmailStatus() == EmailStatus.MARKED_AS_SPAM)
                                    <mark style="background-color: #6f42c1; color: #ffffff;">SPAM</mark>
                                @endif
                            @endif
                        </td>
                        <td>${shortPreview}</td>
                        <td>
                            <details>
                                <summary role="button" class="secondary small outline">Details</summary>
                                <dl>
                                    @for(String key : s.payload().keySet())
                                        !{if(key.endsWith("__url")) continue;}
                                        <dt>${key}</dt>
                                        <dd>
                                            @if(s.payload().containsKey(key + "__url") && !s.payload().getOrDefault(key + "__url", "").isBlank())
                                                <a href="${s.payload().get(key + "__url")}">${s.payload().get(key)}</a>
                                            @else
                                                ${s.payload().get(key)}
                                            @endif
                                        </dd>
                                    @endfor
                                </dl>
                            </details>
                        </td>
                    </tr>
                @endfor
                </tbody>
            </table>
        @endif
    </article>

    <article>
        <header>
            <strong>Spam Submissions</strong> (${spamSubmissions.size()})
        </header>

        @if(spamSubmissions.isEmpty())
            <p><em>No spam caught yet.</em></p>
        @else
            <table>
                <thead>
                <tr>
                    <th scope="col">Timestamp</th>
                    <th scope="col">Autoresponse</th>
                    <th scope="col">Preview</th>
                    <%--                    <th scope="col">Spam Reason</th>--%>
                    <th scope="col"></th>
                </tr>
                </thead>
                <tbody>
                @for(SubmissionItem s : spamSubmissions)
                    !{String preview = s.payload().entrySet().stream()
                        .filter(entry -> !entry.getKey().endsWith("__url"))
                        .map(entry -> entry.getValue() != null ? entry.getValue().toString() : "")
                        .collect(java.util.stream.Collectors.joining(" | "));}
                    !{ String shortPreview = preview.length() > 50 ? preview.substring(0, 50) + "…" : preview; }
                    <%--                    !{ String spamReason = s.payload().getOrDefault("spamReason", "Unspecified"); }--%>
                    <tr>
                        <td>${s.createdAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}</td>
                        <td>
                            @if(s.emailAutoresponseEmailStatus() == null)
                                <mark>DISABLED</mark>
                            @else
                                @if(s.emailAutoresponseEmailStatus() == null)
                                    <mark style="background-color: #6c757d; color: #ffffff;">DISABLED</mark>
                                @elseif(s.emailAutoresponseEmailStatus() == EmailStatus.DELIVERED)
                                    <mark style="background-color: #28a745; color: #ffffff;">DELIVERED</mark>
                                @elseif(s.emailAutoresponseEmailStatus() == EmailStatus.SENT)
                                    <mark style="background-color: #17a2b8; color: #ffffff;">SENT</mark>
                                @elseif(s.emailAutoresponseEmailStatus() == EmailStatus.SOFT_BOUNCE)
                                    <mark style="background-color: #fd7e14; color: #ffffff;">SOFT BOUNCE</mark>
                                @elseif(s.emailAutoresponseEmailStatus() == EmailStatus.HARD_BOUNCE)
                                    <mark style="background-color: #dc3545; color: #ffffff;">HARD BOUNCE</mark>
                                @elseif(s.emailAutoresponseEmailStatus() == EmailStatus.MARKED_AS_SPAM)
                                    <mark style="background-color: #6f42c1; color: #ffffff;">SPAM</mark>
                                @endif
                            @endif
                        </td>
                        <td>${shortPreview}</td>
                        <%--                        <td>${spamReason}</td>--%>
                        <td>
                            <details>
                                <summary role="button" class="secondary small outline">Details</summary>
                                <dl>
                                    @for(String key : s.payload().keySet())
                                        !{if(key.endsWith("__url")) continue;}
                                        <dt>${key}</dt>
                                        <dd>
                                            @if(s.payload().containsKey(key + "__url") && !s.payload().getOrDefault(key + "__url", "").isBlank())
                                                <a href="${s.payload().get(key + "__url")}">${s.payload().get(key)}</a>
                                            @else
                                                ${s.payload().get(key)}
                                            @endif
                                        </dd>
                                    @endfor
                                </dl>
                            </details>
                        </td>
                    </tr>
                @endfor
                </tbody>
            </table>
        @endif
    </article>

</main>

<footer class="container">
    <small>&copy; 2026 Formbox. Edge-powered form management backend.</small>
</footer>

</body>
</html>