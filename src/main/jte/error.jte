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
<div>
    <div>
        <div></div>
        <h2>${errorTitle}</h2>
        <p>${errorMessage}</p>
        <p>${errorStatus}</p>
        <div>
            <a href="/static">Go to Main Page</a>
        </div>
    </div>
</div>
</body>
</html>