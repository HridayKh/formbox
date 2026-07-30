@param String pageName
@param long balanceLeft
@param boolean showManageSubscription
@param String email

<nav>
    <ul>
        <li><a href="/"><strong>Formbox</strong></a> → ${pageName}</li>
    </ul>
    <ul>
        <li>${balanceLeft} Submissions Left</li>
        <li>
            <a href="/billing/portal">
                @if(showManageSubscription)
                    Manage Subscriptions
                @else
                    Upgrade
                @endif
            </a>
        </li>
        <li><i>${email}</i></li>
        <li><a href="/auth/logout">Logout</a></li>
    </ul>
</nav>