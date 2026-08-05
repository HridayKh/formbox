@import formbox.notifs.EmailStatus
@import formbox.submission.SubmissionItem
@import java.time.format.DateTimeFormatter
@import java.util.List

@param List<SubmissionItem> submissions
@param String name = "Form Submissions!"

!{if (submissions == null) submissions = List.of();}

<article style="font-size: 0.75em">
    <header>
        <strong>${name}</strong> (${submissions.size()})
    </header>

    @if(submissions.isEmpty())
        <p><em>No ${name} yet.</em></p>
    @else
        <table>
            <thead>
            <tr>
                <th scope="col">Timestamp</th>
                <th scope="col">Autoresponse Email</th>
                <th scope="col">Notification Email</th>
                <th scope="col">Preview</th>
                <th scope="col"></th>
            </tr>
            </thead>
            <tbody>
            @for(SubmissionItem s : submissions)
                !{String preview = s.payload().entrySet().stream()
                        .filter(entry -> !entry.getKey().endsWith("__url"))
                        .map(entry -> entry.getValue() != null ? entry.getValue() : "")
                        .collect(java.util.stream.Collectors.joining(" | "));}
                !{ String shortPreview = preview.length() > 50 ? preview.substring(0, 50) + "…" : preview; }
                <tr>
                    <td>${s.createdAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))}</td>
                    <td>
                        @if(s.emailAutoresponseEmailStatus() == null)
                            <small style="color: var(--pico-muted-color);">DISABLED</small>
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
                    </td>
                    <td>
                        @if(s.emailNotifStatus() == null)
                            <small style="color: var(--pico-muted-color);">DISABLED</small>
                        @elseif(s.emailNotifStatus() == EmailStatus.DELIVERED)
                            <mark style="background-color: #28a745; color: #ffffff;">DELIVERED</mark>
                        @elseif(s.emailNotifStatus() == EmailStatus.SENT)
                            <mark style="background-color: #17a2b8; color: #ffffff;">SENT</mark>
                        @elseif(s.emailNotifStatus() == EmailStatus.SOFT_BOUNCE)
                            <mark style="background-color: #fd7e14; color: #ffffff;">SOFT BOUNCE</mark>
                        @elseif(s.emailNotifStatus() == EmailStatus.HARD_BOUNCE)
                            <mark style="background-color: #dc3545; color: #ffffff;">HARD BOUNCE</mark>
                        @elseif(s.emailNotifStatus() == EmailStatus.MARKED_AS_SPAM)
                            <mark style="background-color: #6f42c1; color: #ffffff;">SPAM</mark>
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