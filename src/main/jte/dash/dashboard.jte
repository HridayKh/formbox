@import formbox.folder.FolderDto
@import formbox.form.FormDto
@import formbox.dashboard.FolderFormDTO
@import java.util.List

@param boolean showManageSubscription
@param Long balanceLeft
@param String email
@param String msg
@param List<FolderFormDTO> folderForms
@param boolean allowRedirUrl

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <title>Form Endpoints Dashboard | Formbox</title>
    <meta name="description" content="Formbox - Form backend for static sites powered by Cloudflare edge."/>
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/@picocss/pico@2/css/pico.pumpkin.min.css"/>
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
    @if(msg != null && !msg.isBlank())
        <div class="container">
            <mark>${msg}</mark>
        </div>
    @endif

</header>

<main class="container">
    <section id="create-folder">
        <form method="post" action="/forms">
            <fieldset role="group">
                <input name="folderName" placeholder="New Folder Name Here" required>
                <input type="submit" value="Create Folder"/>
            </fieldset>
        </form>
    </section>

    <section id="list-folders">
        @for(FolderFormDTO folderForm : folderForms)
            !{FolderDto folder = folderForm.folder();}
            <details>
                <summary role="button" class="outline"><strong>${folder.name()}</strong></summary>
                <div role="group">
                    <span></span>
                    <button class="small" onclick="openM('create-${folder.id().toString()}')">
                        + Create Form
                    </button>
                    <span></span>
                    <button class="secondary small" onclick="openM('rename-${folder.id().toString()}')">
                        Rename This Folder
                    </button>
                    <span></span>
                    <form action="/forms/${folder.id().toString()}/delete" method="POST">
                        <button type="submit"
                                class="contrast outline small"
                                onclick="return confirm('Are you sure you want to delete this folderId?');">
                            Delete This Folder
                        </button>
                    </form>
                    <span></span>
                </div>
                <div class="grid">
                    @for(FormDto form : folderForm.forms())
                        <div class="form-card">
                            <a href="/forms/${folder.id().toString()}/${form.id().toString()}">${form.name()}</a>
                        </div>
                    @endfor
                </div>
                <dialog id="rename-${folder.id().toString()}">
                    <article>
                        <header>
                            <button aria-label="Close" rel="prev"
                                    onclick="closeM('rename-${folder.id().toString()}')"></button>
                            <h3>Rename Folder ${folder.name()}</h3>
                        </header>

                        <form action="/forms/${folder.id().toString()}/rename" method="post">
                            <fieldset role="group">
                                <input type="text" name="newName" placeholder="Enter new folder name" required
                                       autofocus/>
                                <input type="submit" value="Rename"/>
                            </fieldset>
                        </form>
                        <footer>
                            <button onclick="closeM('rename-${folder.id().toString()}')"
                                    class="small secondary outline">Cancel
                            </button>
                        </footer>
                    </article>
                </dialog>
                <dialog id="create-${folder.id().toString()}">
                    <article>
                        <header>
                            <button aria-label="Close" rel="prev"
                                    onclick="closeM('create-${folder.id().toString()}')"></button>
                            <h3>Create Form in ${folder.name()}</h3>
                        </header>

                        <form action="/forms/${folder.id().toString()}" method="post">
                            <label>
                                Name
                                <input type="text" name="formName" placeholder="Enter new form name" required
                                       autofocus/>
                            </label>
                            <label>
                                Redirect Url
                                <input type="url" name="redirectUrl" disabled="${!allowRedirUrl}"
                                       placeholder="Enter Redirect Url"/>
                            </label>
                            @if(!allowRedirUrl)
                                <small>
                                    Please upgrade <a href="/billing/portal">here</a> to set redirect urls!
                                </small>
                            @endif
                            <footer>
                                <fieldset role="group">
                                    <input type="submit" value="Create"/>
                                    <button onclick="closeM('create-${folder.id().toString()}')"
                                            class="small secondary outline">Cancel
                                    </button>
                                </fieldset>
                            </footer>
                        </form>
                    </article>
                </dialog>
            </details>
        @endfor
    </section>
</main>

<footer class="container">
    <small>&copy; 2026 Formbox. Edge-powered form management backend.</small>
</footer>

<script>
    window.history.replaceState(null, '', window.location.origin + window.location.pathname);

    function closeM(modalId) {
        document.getElementById(modalId).close()
    }

    function openM(modalId) {
        document.getElementById(modalId).showModal()
    }
</script>

</body>
</html>