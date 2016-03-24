<!DOCTYPE html>
<html>
<head>
    <title>Whydah Login</title>
    <link rel="stylesheet" href="css/whydah.css" TYPE="text/css"/>
    <meta charset="utf-8"/>
</head>
<body>

<div id="page-content">
    <div id="login-page">
        <div id="logo">
            <img src="${logoURL}" alt="Whydah Log In"/>

        </div>
        <div style="margin:15px;padding:0;display:inline"></div>
        <div>
            <h4>Username:  ${username!} </h4>
            Your password has been reset. You will soon receive an email containing instructions on how to acquire a new password.</br></br>
            <a href="/sso/welcome">Log in.</a>
        </div>
    </div>
</div>
</body>
</html>