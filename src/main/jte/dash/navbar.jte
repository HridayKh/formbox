@param String pageName
@param long balanceLeft
@param boolean showManageSubscription
@param String email
@param Long storageUsed = null
@param Long storageLimit = null

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
                    @if(storageUsed != null && storageLimit != null)
                        <li><small>Storage: ${String.format("%.2f", storageUsed / 1048576.0)} MB / ${String.format("%.2f", storageLimit / 1048576.0)} MB</small></li>
                    @endif
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