@param String pageName
@param long balanceLeft
@param boolean showManageSubscription
@param String email

<nav>
    <ul>
        <li><a href="/"><strong>Formbox</strong></a> → ${pageName}</li>
    </ul>
    <ul>
        @if(!showManageSubscription)
            <li><a href="/billing/portal" role="button">⚡ Upgrade</a></li>
        @endif
        <li>
            <details class="dropdown">
                <summary>Profile</summary>
                <ul dir="rtl">
                    <li><strong>${email}</strong></li>
                    <li><small>${balanceLeft} Submissions Left</small></li>
                    <li><hr/></li>
                    @if(showManageSubscription)
                        <li><a href="/billing/portal">Manage Subscription</a></li>
                    @endif
                    <li><a href="/docs/">Docs</a></li>
                    <li><a href="/dashboard/emails">Allowed Emails</a></li>
                    <li><a href="/dashboard/support">Support</a></li>
                    <li><hr/></li>
                    <li><a href="/auth/logout">Logout</a></li>
                </ul>
            </details>
        </li>
    </ul>
</nav>