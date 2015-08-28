<!DOCTYPE html>
<html>
<head>
    <title>Whydah Redirection</title>
    <meta http-equiv="refresh" content="0;url=${redirect!"/sso/welcome"}">
    <meta charset="utf-8"/>
    <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1"/>
    <link rel="stylesheet" href="css/whydah.css" type="text/css"/>
    <link rel="shortcut icon" href="images/favicon.ico" type="image/x-icon"/>
    <link rel="icon" href="images/favicon.ico" type="image/x-icon"/>
</head>
<body>
    <div id="page-content">
        <div id="logo">
            <img src="${logoURL}" alt="Site logo"/>
            <h2>Redirecting to ${redirect!"/sso/welcome"}</h2>
        </div>
    </div>
</body>
</html>