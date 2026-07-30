@param String errorTitle
@param String errorMessage
@param org.springframework.http.HttpStatus errorStatus

<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Error</title>
</head>
<body>
<h1>${errorTitle}</h1>
<p>${errorMessage}</p>
<p>${errorStatus}</p>
<a href="/">Go to Main Page</a>
</body>
</html>