@import in.hridaykh.formbox.model.dto.CachedForm

@param CachedForm cachedForm

<head>
    @if(cachedForm.name() != null)
        <meta name="description" content="${cachedForm.name()}">
    @endif
    <title>${cachedForm.honeypotName()}</title>
</head>
<body>
<h1>${cachedForm.honeypotName()}</h1>
<p>Welcome to my example page!</p>
</body>
